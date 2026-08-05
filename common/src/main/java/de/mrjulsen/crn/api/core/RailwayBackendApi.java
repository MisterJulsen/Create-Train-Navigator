package de.mrjulsen.crn.api.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import de.mrjulsen.crn.api.core.query.*;
import de.mrjulsen.crn.api.core.ref.CategoryRef;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.api.core.ref.StationTagRef;
import de.mrjulsen.crn.api.core.snapshot.*;
import de.mrjulsen.crn.api.core.speed.SpeedLimitQuery;
import de.mrjulsen.crn.api.core.speed.SpeedProfileSnapshot;
import de.mrjulsen.crn.core.RailwayBackend;
import de.mrjulsen.crn.core.TrainManager;
import de.mrjulsen.crn.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.api.event.RailwayBackendListener;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.delay.DelayArgument;
import de.mrjulsen.crn.core.delay.ExternalDelayReports;
import de.mrjulsen.crn.core.history.DepartureLog;
import de.mrjulsen.crn.core.history.DepartureStats;
import de.mrjulsen.crn.core.index.StationCallIndex;
import de.mrjulsen.crn.core.schedule.JourneySection;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.core.schedule.TrainJourney;
import de.mrjulsen.crn.core.timing.SpeedProfileEstimator;
import de.mrjulsen.crn.core.timing.StopTimings;
import de.mrjulsen.crn.data.settings.StationTag;
import de.mrjulsen.crn.data.settings.TrainCategory;
import de.mrjulsen.crn.data.settings.TrainLine;
import de.mrjulsen.crn.data.schedule.condition.ETrainFilter;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.util.ModUtils;
import net.minecraft.resources.ResourceLocation;

/**
 * Read access to the train data backend. This is the entry point for addons; the classes behind it
 * are internal and may change between versions.
 *
 * <h2>Return values</h2>
 * Every query returns an immutable snapshot of the state at the moment of the call. Snapshots are
 * never updated afterwards, so a caller that needs current data must query again. Queries that
 * cannot answer return an empty {@link Optional} or an empty list rather than {@code null}.
 *
 * <h2>Time</h2>
 * All times and durations in this API are game ticks on the backend's own time base, which is the
 * world time adjusted by the configured time settings. Use {@link #getCurrentTime()} to obtain a value
 * comparable to the timestamps in the returned snapshots; do not mix them with raw level times.
 *
 * <h2>Filtering</h2>
 * Queries that <em>list</em> trains, calls or stops omit blacklisted trains, blacklisted stations
 * and trains whose data is not yet reliable, so their results are safe to display publicly. Queries
 * that look up a single object <em>by id</em> do not filter, so a caller that already knows which
 * train it wants always gets it.
 *
 * <h2>Threading</h2>
 * Queries may be called from any thread. A single returned snapshot is internally consistent, but
 * two snapshots obtained from separate calls may not be, because the backend can update in between.
 * Prefer one {@link JourneySnapshot} over assembling a run from individual stop queries.
 *
 * <h2>Availability</h2>
 * The backend exists only while a server is running. Check {@link #isActive()} before querying;
 * otherwise queries return empty results.
 */
public final class RailwayBackendApi {

    private RailwayBackendApi() {}

    public static boolean isActive() {
        return RailwayBackend.isActive();
    }

    public static long getCurrentTime() {
        return ModUtils.getTransformedWorldTime();
    }

    public synchronized static List<TrainSnapshot> getAllTrains() {
        return getTrackedTrains().map(TrainSnapshot::of).toList();
    }

    public synchronized static List<TrainSnapshot> getTrains(TrainQuery query) {
        return getTrains(query, x -> true);
    }

    public synchronized static List<TrainSnapshot> getTrains(TrainQuery query, Predicate<TrainSnapshot> filter) {
        List<TrainSnapshot> trains = new ArrayList<>(TrainManager.getInstance().getAllTrains().size());
        for (TrackedTrain train : TrainManager.getInstance().getAllTrains()) {
            if (!query.accept(train))
                continue;

            TrainSnapshot snapshot = TrainSnapshot.of(train);
            if (!filter.test(snapshot))
                continue;

            trains.add(snapshot);
        }
        return trains;
    }

    public synchronized static Optional<TrainSnapshot> getTrain(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(TrainSnapshot::of);
    }

    public static int getTrackedTrainCount() {
        return TrainManager.getInstance().getAllTrains().size();
    }

    public static Optional<JourneySnapshot> getJourney(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(JourneySnapshot::of);
    }

    public static Optional<JourneySnapshot> getJourneyIn(UUID trainId, int cycles) {
        return getJourney(trainId).map(x -> x.advancedBy(cycles));
    }

    public static Optional<SectionSnapshot> getCurrentSection(UUID trainId) {
        return getJourney(trainId).flatMap(JourneySnapshot::currentSection);
    }

    public static Optional<StopSnapshot> getCurrentStop(UUID trainId) {
        return getJourney(trainId).flatMap(JourneySnapshot::currentStop);
    }

    public static Optional<StopSnapshot> getNextStop(UUID trainId) {
        return getJourney(trainId).flatMap(JourneySnapshot::nextStop);
    }

    public static List<StopSnapshot> getPassedStops(UUID trainId) {
        return getJourney(trainId).map(JourneySnapshot::passedStops).orElse(List.of());
    }

    public static List<StopSnapshot> getUpcomingStops(UUID trainId) {
        return getJourney(trainId).map(JourneySnapshot::upcomingStops).orElse(List.of());
    }

    public static List<StopSnapshot> getRecentStops(UUID trainId) {
        return getJourney(trainId).map(JourneySnapshot::recentStops).orElse(List.of());
    }

    public static Optional<StopSnapshot> getPreviousCallAt(UUID trainId, String stationName) {
        List<StopSnapshot> stops = getPassedStops(trainId);
        for (int i = stops.size() - 1; i >= 0; i--) {
            StopSnapshot stop = stops.get(i);
            if (TrainUtils.stationMatches(stop.stationName(), stationName)) {
                return Optional.of(stop);
            }
        }
        return Optional.empty();
    }

    public static Optional<StopSnapshot> getNextCallAt(UUID trainId, String stationName) {
        List<StopSnapshot> stops = getPassedStops(trainId);
        for (StopSnapshot stop : stops) {
            if (TrainUtils.stationMatches(stop.stationName(), stationName)) {
                return Optional.of(stop);
            }
        }
        return Optional.empty();
    }

    public static Optional<StopSnapshot> getNextCallAt(UUID trainId, String stationName, long notBefore) {
        return getJourney(trainId).flatMap(x -> x.nextCallAt(stationName, notBefore));
    }


    public static Optional<TrainPositionSnapshot> getPosition(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(x -> TrainPositionSnapshot.of(x.getTrain(), x.getExitSide()));
    }

    public static List<TrainPositionSnapshot> getAllPositions() {
        return getAllPositions(TrainPositionQuery.all());
    }

    public static List<TrainPositionSnapshot> getAllPositions(TrainPositionQuery query) {
        return getTrackedTrains()
                .map(x -> TrainPositionSnapshot.of(x.getTrain(), x.getExitSide()))
                .filter(query::accept)
                .toList();
    }

    public static Optional<TrainCompositionSnapshot> getComposition(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(x -> TrainCompositionSnapshot.of(x.getTrain()));
    }

    public static SpeedProfileSnapshot getSpeedProfile(UUID trainId, double horizon) {
        return TrainManager.getInstance().getTrain(trainId).map(tracked -> {
            var train = tracked.getTrain();
            if (train.navigation == null) {
                return SpeedProfileSnapshot.NONE;
            }
            double distance = horizon > 0 ? horizon : train.navigation.distanceToDestination;
            double cruiseSpeed = (train.maxSpeed() + train.maxTurnSpeed()) / 2;
            return SpeedProfileEstimator.profile(train, distance, cruiseSpeed, SpeedLimitQuery.SpeedLimitPurpose.DISPLAY);
        }).orElse(SpeedProfileSnapshot.NONE);
    }

    public static Optional<DelayReport> getDelayReport(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(DelayReport::of);
    }

    public static List<DelayReport> getDisruptions() {
        return getTrackedTrains()
            .filter(x -> x.isDelayed() || x.isCancelled())
            .map(DelayReport::of)
            .toList();
    }

    public static boolean reportDelay(UUID trainId, ResourceLocation causeId, long expiresInTicks, DelayArgument... args) {
        return ExternalDelayReports.report(trainId, causeId, expiresInTicks, -1, args);
    }

    public static boolean withdrawDelay(UUID trainId, ResourceLocation causeId) {
        return ExternalDelayReports.withdraw(trainId, causeId);
    }

    public static List<BoardEntry> getBoard(String stationName, BoardQuery query) {
        return board(TrainManager.getInstance().getCallIndex().callsAt(stationName, query.includeDivertedAway()), query, Comparator.comparingLong(BoardEntry::realtimeArrival));
    }

    public static List<BoardEntry> getBoard(StationTag stationTag, BoardQuery query) {
        return board(TrainManager.getInstance().getCallIndex().callsAt(stationTag, query.includeDivertedAway()), query, Comparator.comparingLong(BoardEntry::realtimeArrival));
    }

    public static List<BoardEntry> getBoard(UUID stationTagId, BoardQuery query) {
        return GlobalSettings.getInstance().getStationTag(stationTagId)
            .map(tag -> getBoard(tag, query))
            .orElse(List.of());
    }

    public static Optional<BoardEntry> getNextArrival(String stationName, BoardQuery query) {
        return board(TrainManager.getInstance().getCallIndex().callsAt(stationName, query.includeDivertedAway()), query.withLimit(1), Comparator.comparingLong(BoardEntry::realtimeArrival))
                .stream()
                .findFirst();
    }

    public static Optional<BoardEntry> getNextDeparture(String stationName, BoardQuery query) {
        return board(TrainManager.getInstance().getCallIndex().callsAt(stationName, query.includeDivertedAway()), query.withLimit(1), Comparator.comparingLong(BoardEntry::realtimeDeparture))
                .stream()
                .findFirst();
    }

    public static Set<String> getKnownStations() {
        return TrainUtils.getAllStationNames();
    }

    public static Optional<StationSnapshot> getStation(String stationName) {
        if (stationName == null || !TrainUtils.getAllStationNames().contains(stationName)) {
            return Optional.empty();
        }
        return Optional.of(buildStation(stationName));
    }

    public static List<StationSnapshot> getAllStations() {
        return getAllStations(StationQuery.all());
    }

    public static List<StationSnapshot> getAllStations(StationQuery query) {
        List<StationSnapshot> stations = new ArrayList<>(TrainUtils.getAllStationNames().size());
        for (String station : TrainUtils.getAllStationNames()) {
            StationSnapshot snapshot = buildStation(station);
            if (query.accept(snapshot)) {
                stations.add(snapshot);
            }
        }
        stations.sort(Comparator.comparing(StationSnapshot::name));
        return stations;
    }

    public static StationRef getStationRef(String stationName) {
        return StationRef.of(stationName);
    }

    public static List<StationTag> getAllStationTags() {
        return GlobalSettings.getInstance().getAllStationTags();
    }

    public static Optional<StationTag> getStationTag(UUID stationTagId) {
        return GlobalSettings.getInstance().getStationTag(stationTagId);
    }

    public static Optional<LineSnapshot> getLine(UUID lineId) {
        return GlobalSettings.getInstance().getTrainLine(lineId).map(RailwayBackendApi::buildLine);
    }

    public static List<LineSnapshot> getAllLines() {
        return getAllLines(LineQuery.all());
    }

    public static List<LineSnapshot> getAllLines(LineQuery query) {
        return GlobalSettings.getInstance().getAllTrainLines()
                .stream()
                .map(RailwayBackendApi::buildLine)
                .filter(query::accept)
                .toList();
    }

    public static Optional<CategorySnapshot> getCategory(UUID categoryId) {
        return GlobalSettings.getInstance().getTrainCategory(categoryId).map(RailwayBackendApi::buildCategory);
    }

    public static List<CategorySnapshot> getAllCategories() {
        return getAllCategories(CategoryQuery.all());
    }

    public static List<CategorySnapshot> getAllCategories(CategoryQuery query) {
        return GlobalSettings.getInstance().getAllTrainCategories()
                .stream()
                .map(RailwayBackendApi::buildCategory)
                .filter(query::accept)
                .toList();
    }

    public static long getLastDepartureTime(String stationFilter, ETrainFilter filter, UUID trainId, String trainName) {
        DepartureLog log = TrainManager.getInstance().getDepartureLog();
        return switch (filter) {
            case SAME_LINE -> currentSectionOf(trainId)
                .map(section -> log.getLastDepartureOfLine(stationFilter, section.getTrainLineId()))
                .orElse(DepartureLog.NEVER);
            case SAME_CATEGORY -> currentSectionOf(trainId)
                .map(section -> log.getLastDepartureOfCategory(stationFilter, section.getTrainCategoryId()))
                .orElse(DepartureLog.NEVER);
            case SAME_NAME -> log.getLastDepartureOfName(stationFilter, trainName);
            default -> log.getLastDeparture(stationFilter);
        };
    }

    private static Optional<JourneySection> currentSectionOf(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).flatMap(TrackedTrain::getCurrentSection);
    }

    public static DepartureStats getDepartureStats(String stationName) {
        GlobalSettings settings = GlobalSettings.getInstance();
        return TrainManager.getInstance().getDepartureLog().getStats(stationName,
            lineId -> settings.getTrainLine(lineId).map(TrainLine::getLineName).orElse(null),
            categoryId -> settings.getTrainCategory(categoryId).map(TrainCategory::getCategoryName).orElse(null));
    }

    public static void addListener(RailwayBackendListener listener) {
        RailwayBackendEvents.register(listener);
    }

    public static boolean removeListener(RailwayBackendListener listener) {
        return RailwayBackendEvents.unregister(listener);
    }

    public static Optional<TrackedTrain> getTrackedTrain(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId);
    }








    private static java.util.stream.Stream<TrackedTrain> getTrackedTrains() {
        return TrainManager.getInstance().getAllTrains().stream()
            .filter(TrackedTrain::isReportable)
            .filter(x -> !x.isBlacklisted());
    }

    private static List<TrainSnapshot> filterBySection(Predicate<JourneySection> filter) {
        return getTrackedTrains()
            .filter(x -> x.getCurrentSection().map(filter::test).orElse(false))
            .map(TrainSnapshot::of)
            .toList();
    }

    private static List<BoardEntry> board(List<StationCallIndex.Call> calls, BoardQuery query, Comparator<BoardEntry> order) {
        List<BoardEntry> entries = new ArrayList<>();
        GlobalSettings settings = GlobalSettings.getInstance();

        for (StationCallIndex.Call call : calls) {
            TrackedTrain train = call.train();
            JourneyStop stop = call.stop();

            if (!train.isReportable() || train.isBlacklisted()) {
                continue;
            }
            if (!query.includeCancelled() && train.isCancelled()) {
                continue;
            }
            if (!query.includeUnreliable() && !train.getLifecycleState().isUsable()) {
                continue;
            }
            if (settings.isStationBlacklisted(train.getDisplayStationName(stop))) {
                continue;
            }
            if (!isServiceable(train.getJourney(), stop)) {
                continue;
            }
            StopTimings timing = train.getTimings(stop);
            if (timing == null) {
                continue;
            }
            BoardEntry entry = BoardEntry.of(train, stop).atOrAfter(query.fromTime(), train.getTotalDuration());
            if (!query.acceptsTime(entry.realtimeDeparture())) {
                continue;
            }
            if (query.accepts(entry)) {
                entries.add(entry);
            }
        }

        entries.sort(order);

        if (query.deduplicateTrains()) {
            Set<UUID> seen = new HashSet<>();
            entries.removeIf(x -> !seen.add(x.trainId()));
        }
        if (entries.size() > query.limit()) {
            entries = entries.subList(0, Math.max(0, query.limit()));
        }
        return List.copyOf(entries);
    }

    private static boolean isServiceable(TrainJourney journey, JourneyStop stop) {
        JourneySection section = stop.getSection();
        if (section == null || section.isUsable()) {
            return true;
        }
        return section.isFirstStop(stop) && journey.previousSectionOf(section)
            .map(previous -> previous.isUsable() && journey.carriesPassengersOnward(previous))
            .orElse(false);
    }

    private static StationSnapshot buildStation(String stationName) {
        GlobalSettings settings = GlobalSettings.getInstance();
        List<StationTag> tags = new ArrayList<>();
        for (StationTag tag : settings.getAllStationTags()) {
            if (tag.contains(stationName)) {
                tags.add(tag);
            }
        }

        Set<UUID> lineIds = new LinkedHashSet<>();
        Set<UUID> categoryIds = new LinkedHashSet<>();
        Set<UUID> trainIds = new LinkedHashSet<>();

        for (StationCallIndex.Call call : TrainManager.getInstance().getCallIndex().callsAt(stationName, false)) {
            if (!call.train().isReportable() || call.train().isBlacklisted()) {
                continue;
            }
            trainIds.add(call.train().getTrainId());
            JourneySection section = call.stop().getSection();
            if (section != null) {
                if (section.getTrainLineId() != null) lineIds.add(section.getTrainLineId());
                if (section.getTrainCategoryId() != null) categoryIds.add(section.getTrainCategoryId());
            }
        }

        return new StationSnapshot(
            StationRef.of(stationName, tags.isEmpty() ? null : tags.get(0)),
            tags.stream().map(StationTagRef::of).toList(),
            resolveLines(lineIds),
            resolveCategories(categoryIds),
            trainIds,
            settings.isStationBlacklisted(stationName)
        );
    }

    private static LineSnapshot buildLine(TrainLine line) {
        Set<UUID> trainIds = new LinkedHashSet<>();
        Set<String> stations = new LinkedHashSet<>();
        int delayed = 0;

        for (TrackedTrain train : getTrackedTrains().toList()) {
            if (!operatesOn(train, line.getId())) {
                continue;
            }
            trainIds.add(train.getTrainId());
            if (train.isDelayed()) {
                delayed++;
            }
            for (JourneySection section : train.getJourney().getSections()) {
                if (!line.getId().equals(section.getTrainLineId())) {
                    continue;
                }
                for (JourneyStop stop : section.getStops()) {
                    stations.add(train.getDisplayStationName(stop));
                }
            }
        }

        return new LineSnapshot(LineRef.of(line), trainIds, stations.stream().map(StationRef::of).toList(), delayed);
    }

    private static CategorySnapshot buildCategory(TrainCategory category) {
        Set<UUID> trainIds = new LinkedHashSet<>();
        Set<UUID> lineIds = new LinkedHashSet<>();
        int delayed = 0;

        for (TrackedTrain train : getTrackedTrains().toList()) {
            boolean carries = false;
            for (JourneySection section : train.getJourney().getSections()) {
                if (category.getId().equals(section.getTrainCategoryId())) {
                    carries = true;
                    if (section.getTrainLineId() != null) {
                        lineIds.add(section.getTrainLineId());
                    }
                }
            }
            if (carries) {
                trainIds.add(train.getTrainId());
                if (train.isDelayed()) {
                    delayed++;
                }
            }
        }

        return new CategorySnapshot(CategoryRef.of(category), trainIds, resolveLines(lineIds), delayed);
    }

    private static List<LineRef> resolveLines(Set<UUID> lineIds) {
        GlobalSettings settings = GlobalSettings.getInstance();
        return lineIds.stream().map(settings::getTrainLine).filter(Optional::isPresent).map(Optional::get).map(LineRef::of).toList();
    }

    private static List<CategoryRef> resolveCategories(Set<UUID> categoryIds) {
        GlobalSettings settings = GlobalSettings.getInstance();
        return categoryIds.stream().map(settings::getTrainCategory).filter(Optional::isPresent).map(Optional::get).map(CategoryRef::of).toList();
    }

    private static boolean operatesOn(TrackedTrain train, UUID lineId) {
        for (JourneySection section : train.getJourney().getSections()) {
            if (lineId.equals(section.getTrainLineId())) {
                return true;
            }
        }
        return false;
    }
}
