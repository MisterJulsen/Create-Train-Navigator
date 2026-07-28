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
 * world time adjusted by the configured time settings. Use {@link #currentTime()} to obtain a value
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

    /** Whether the backend is running, which is the case exactly while a server is active. */
    public static boolean isActive() {
        return RailwayBackend.isActive();
    }

    /** The current backend time in ticks, on the same time base as every timestamp in this API. */
    public static long currentTime() {
        return ModUtils.getTransformedWorldTime();
    }

    /**
     * Every train currently worth reporting: those in service, plus those out of service whose
     * reason is still being shown. Trains parked long ago are omitted but remain reachable by id.
     */
    public static List<TrainSnapshot> getAllTrains() {
        return reportableTrains().map(TrainSnapshot::of).toList();
    }

    /** The train with the given id, whether or not it is currently worth reporting. */
    public static Optional<TrainSnapshot> getTrain(UUID trainId) {
        return manager().getTrain(trainId).map(TrainSnapshot::of);
    }

    /**
     * The first tracked train with exactly this name. Train names are not unique, so this is only
     * dependable when the caller knows the name identifies one train.
     */
    public static Optional<TrainSnapshot> getTrainByName(String trainName) {
        return manager().getAllTrains().stream()
            .filter(x -> x.getTrainName().equals(trainName))
            .findFirst()
            .map(TrainSnapshot::of);
    }

    /** Every reportable train whose snapshot matches the given test. */
    public static List<TrainSnapshot> getTrains(Predicate<TrainSnapshot> filter) {
        return reportableTrains().map(TrainSnapshot::of).filter(filter).toList();
    }

    /** Every reportable train that is currently running late. */
    public static List<TrainSnapshot> getDelayedTrains() {
        return reportableTrains().filter(TrackedTrain::isDelayed).map(TrainSnapshot::of).toList();
    }

    /** Every reportable train that is currently out of service. */
    public static List<TrainSnapshot> getCancelledTrains() {
        return reportableTrains().filter(TrackedTrain::isCancelled).map(TrainSnapshot::of).toList();
    }

    /**
     * Every reportable train whose <em>current</em> section belongs to the given line. A train that
     * serves the line later in its run is not included until it reaches that section.
     */
    public static List<TrainSnapshot> getTrainsOfLine(UUID lineId) {
        return filterBySection(section -> lineId != null && lineId.equals(section.getTrainLineId()));
    }

    /**
     * Every reportable train whose <em>current</em> section belongs to the given category. A train
     * that runs under the category later in its run is not included until it reaches that section.
     */
    public static List<TrainSnapshot> getTrainsOfCategory(UUID categoryId) {
        return filterBySection(section -> categoryId != null && categoryId.equals(section.getTrainCategoryId()));
    }

    /** How many trains the backend tracks in total, including those not worth reporting. */
    public static int getTrackedTrainCount() {
        return manager().getAllTrains().size();
    }

    /** The complete run of a train: every stop with its times, grouped into sections. */
    public static Optional<JourneySnapshot> getJourney(UUID trainId) {
        return manager().getTrain(trainId).map(JourneySnapshot::of);
    }

    /** The section the train is currently operating in, with its own stops and times. */
    public static Optional<SectionSnapshot> getCurrentSection(UUID trainId) {
        return getJourney(trainId).flatMap(JourneySnapshot::currentSection);
    }

    /** The stop the train is standing at, or empty while it is running between stops. */
    public static Optional<StopSnapshot> getCurrentStop(UUID trainId) {
        return getJourney(trainId).flatMap(JourneySnapshot::currentStop);
    }

    /** The stop the train is heading for next. */
    public static Optional<StopSnapshot> getNextStop(UUID trainId) {
        return getJourney(trainId).flatMap(JourneySnapshot::nextStop);
    }

    /**
     * The stops the train has still to depart from, in the order it will reach them, beginning with
     * the one it currently stands at. On a cyclic schedule this wraps around and covers one whole
     * cycle.
     */
    public static List<StopSnapshot> getUpcomingStops(UUID trainId) {
        return getJourney(trainId).map(JourneySnapshot::upcomingStops).orElse(List.of());
    }

    /** The train's next call at the given station among its upcoming stops. */
    public static Optional<StopSnapshot> getNextCallAt(UUID trainId, String stationName) {
        return getUpcomingStops(trainId).stream()
            .filter(x -> TrainUtils.stationMatches(x.stationName(), stationName))
            .findFirst();
    }

    /**
     * The train's next call at the given station that is not earlier than {@code notBefore}. On a
     * cyclic schedule this may fall in a later cycle, so a call further in the future can be found
     * than {@link #getNextCallAt(UUID, String)} would return.
     */
    public static Optional<StopSnapshot> getNextCallAt(UUID trainId, String stationName, long notBefore) {
        return getJourney(trainId).flatMap(x -> x.nextCallAt(stationName, notBefore));
    }

    /**
     * The train's run projected forward by the given number of whole schedule cycles. Only
     * meaningful for a cyclic schedule; {@code cycles} of zero yields the current run.
     */
    public static Optional<JourneySnapshot> getJourneyIn(UUID trainId, int cycles) {
        return getJourney(trainId).map(x -> x.advancedBy(cycles));
    }

    /** Where the train is and how it is oriented. <b>Server thread only.</b> */
    public static Optional<TrainPositionSnapshot> getPosition(UUID trainId) {
        return manager().getTrain(trainId).map(x -> TrainPositionSnapshot.of(x.getTrain(), x.getExitSide()));
    }

    /** The positions of all reportable trains. <b>Server thread only.</b> */
    public static List<TrainPositionSnapshot> getAllPositions() {
        return reportableTrains().map(x -> TrainPositionSnapshot.of(x.getTrain(), x.getExitSide())).toList();
    }

    /** What the train is made of, carriage by carriage. <b>Server thread only.</b> */
    public static Optional<TrainCompositionSnapshot> getComposition(UUID trainId) {
        return manager().getTrain(trainId).map(x -> TrainCompositionSnapshot.of(x.getTrain()));
    }

    /**
     * The speed the train is expected to hold over the distance ahead of it, including the speed
     * limits that apply. Returns {@link SpeedProfileSnapshot#NONE} if the train is unknown or is
     * not currently navigating.
     * <p>
     * <b>Server thread only.</b>
     *
     * @param horizon How far ahead to look, in blocks. Values of zero or less use the remaining
     *                distance to the train's current destination.
     */
    public static SpeedProfileSnapshot getSpeedProfile(UUID trainId, double horizon) {
        return manager().getTrain(trainId).map(tracked -> {
            var train = tracked.getTrain();
            if (train.navigation == null) {
                return SpeedProfileSnapshot.NONE;
            }
            double distance = horizon > 0 ? horizon : train.navigation.distanceToDestination;
            double cruiseSpeed = (train.maxSpeed() + train.maxTurnSpeed()) / 2;
            return SpeedProfileEstimator.profile(train, distance, cruiseSpeed, SpeedLimitQuery.SpeedLimitPurpose.DISPLAY);
        }).orElse(SpeedProfileSnapshot.NONE);
    }

    /** Why the train is late or out of service, and by how much. */
    public static Optional<DelayReport> getDelayReport(UUID trainId) {
        return manager().getTrain(trainId).map(DelayReport::of);
    }

    /** A report for every reportable train that is currently late or out of service. */
    public static List<DelayReport> getDisruptions() {
        return reportableTrains()
            .filter(x -> x.isDelayed() || x.isCancelled())
            .map(DelayReport::of)
            .toList();
    }

    /**
     * Attaches a delay reason to a train from outside the backend. Repeating a report for the same
     * train and cause refreshes it and keeps the time it was first raised, so a caller may report
     * the same cause every tick without the reason appearing to restart.
     * <p>
     * The reported reason carries no delay magnitude of its own; the backend continues to measure
     * that itself.
     *
     * @param causeId       The delay cause to attach. Must already be registered, otherwise nothing
     *                      is reported.
     * @param expiresInTicks How long the reason stays attached. Values below zero never expire, in
     *                      which case the reason remains until it is withdrawn.
     * @param args          Values to fill the cause's message placeholders.
     * @return Whether the reason was attached. False if the train id or the cause is unknown.
     */
    public static boolean reportDelay(UUID trainId, ResourceLocation causeId, long expiresInTicks, DelayArgument... args) {
        return ExternalDelayReports.report(trainId, causeId, expiresInTicks, -1, args);
    }

    /**
     * Removes a previously reported reason again.
     *
     * @return Whether a reason for this train and cause was attached and has now been removed.
     */
    public static boolean withdrawDelay(UUID trainId, ResourceLocation causeId) {
        return ExternalDelayReports.withdraw(trainId, causeId);
    }

    /** Departures from the station with this exact name, earliest first. */
    public static List<BoardEntry> getDepartures(String stationName, BoardQuery query) {
        return board(manager().getCallIndex().callsAt(stationName, query.includeDivertedAway()), query,
            Comparator.comparingLong(BoardEntry::realtimeDeparture));
    }

    /** Departures from every station covered by the given tag, earliest first. */
    public static List<BoardEntry> getDepartures(StationTag stationTag, BoardQuery query) {
        return board(manager().getCallIndex().callsAt(stationTag, query.includeDivertedAway()), query,
            Comparator.comparingLong(BoardEntry::realtimeDeparture));
    }

    /** Arrivals at the station with this exact name, earliest first. */
    public static List<BoardEntry> getArrivals(String stationName, BoardQuery query) {
        return board(manager().getCallIndex().callsAt(stationName, query.includeDivertedAway()), query,
            Comparator.comparingLong(BoardEntry::realtimeArrival));
    }

    /** Arrivals at every station covered by the given tag, earliest first. */
    public static List<BoardEntry> getArrivals(StationTag stationTag, BoardQuery query) {
        return board(manager().getCallIndex().callsAt(stationTag, query.includeDivertedAway()), query,
            Comparator.comparingLong(BoardEntry::realtimeArrival));
    }

    /** Departures for a station tag by id. Empty if no such tag exists. */
    public static List<BoardEntry> getDepartures(UUID stationTagId, BoardQuery query) {
        return GlobalSettings.getInstance().getStationTag(stationTagId)
            .map(tag -> getDepartures(tag, query))
            .orElse(List.of());
    }

    /** Arrivals for a station tag by id. Empty if no such tag exists. */
    public static List<BoardEntry> getArrivals(UUID stationTagId, BoardQuery query) {
        return GlobalSettings.getInstance().getStationTag(stationTagId)
            .map(tag -> getArrivals(tag, query))
            .orElse(List.of());
    }

    /** The single next departure matching the query. The query's own limit is ignored. */
    public static Optional<BoardEntry> getNextDeparture(String stationName, BoardQuery query) {
        return getDepartures(stationName, query.withLimit(1)).stream().findFirst();
    }

    /** The single next arrival matching the query. The query's own limit is ignored. */
    public static Optional<BoardEntry> getNextArrival(String stationName, BoardQuery query) {
        return getArrivals(stationName, query.withLimit(1)).stream().findFirst();
    }

    /** The names of all stations that exist on the server, including blacklisted ones. */
    public static Set<String> getKnownStations() {
        return TrainUtils.getAllStationNames();
    }

    /** What is known about one station: its tags, and the lines, categories and trains calling. */
    public static Optional<StationSnapshot> getStation(String stationName) {
        if (stationName == null || !TrainUtils.getAllStationNames().contains(stationName)) {
            return Optional.empty();
        }
        return Optional.of(buildStation(stationName));
    }

    /** The same for every station, sorted by name. */
    public static List<StationSnapshot> getAllStations() {
        return TrainUtils.getAllStationNames().stream().sorted().map(RailwayBackendApi::buildStation).toList();
    }

    /** A lightweight reference to a station by name, for use where a full snapshot is not needed. */
    public static StationRef getStationRef(String stationName) {
        return StationRef.of(stationName);
    }

    /** Every station tag configured on the server. */
    public static List<StationTag> getAllStationTags() {
        return GlobalSettings.getInstance().getAllStationTags();
    }

    /** The station tag with the given id. */
    public static Optional<StationTag> getStationTag(UUID stationTagId) {
        return GlobalSettings.getInstance().getStationTag(stationTagId);
    }

    /** A line with the trains currently operating on it and the stations they serve. */
    public static Optional<LineSnapshot> getLine(UUID lineId) {
        return GlobalSettings.getInstance().getTrainLine(lineId).map(RailwayBackendApi::buildLine);
    }

    /** The same for every configured line. */
    public static List<LineSnapshot> getAllLines() {
        return GlobalSettings.getInstance().getAllTrainLines().stream().map(RailwayBackendApi::buildLine).toList();
    }

    /** A category with the trains currently running under it and the lines it covers. */
    public static Optional<CategorySnapshot> getCategory(UUID categoryId) {
        return GlobalSettings.getInstance().getTrainCategory(categoryId).map(RailwayBackendApi::buildCategory);
    }

    /** The same for every configured category. */
    public static List<CategorySnapshot> getAllCategories() {
        return GlobalSettings.getInstance().getAllTrainCategories().stream().map(RailwayBackendApi::buildCategory).toList();
    }

    /**
     * When a train last departed from the given station, restricted to trains related to the one
     * named here in the way the filter demands.
     *
     * @param stationFilter The station name to look at, which may be a filter matching several.
     * @param filter        Which departures count: any, or only those of the same line, category or
     *                      train name as the given train.
     * @param trainId       The train the line and category filters relate to.
     * @param trainName     The train name the name filter compares against.
     * @return The departure time, or {@link DepartureLog#NEVER} if nothing matching has departed.
     */
    public static long getLastDepartureTime(String stationFilter, ETrainFilter filter, UUID trainId, String trainName) {
        DepartureLog log = manager().getDepartureLog();
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
        return manager().getTrain(trainId).flatMap(TrackedTrain::getCurrentSection);
    }

    /** The recorded departure history of a station, with lines and categories resolved to names. */
    public static DepartureStats getDepartureStats(String stationName) {
        GlobalSettings settings = GlobalSettings.getInstance();
        return manager().getDepartureLog().getStats(stationName,
            lineId -> settings.getTrainLine(lineId).map(TrainLine::getLineName).orElse(null),
            categoryId -> settings.getTrainCategory(categoryId).map(TrainCategory::getCategoryName).orElse(null));
    }

    /**
     * Subscribes to backend events. Events are queued where they occur and delivered on the server
     * thread during the backend's tick, so a listener may touch game state but should return
     * quickly. Registering the same listener twice has no additional effect.
     * <p>
     * See {@link RailwayBackendListener} for what is reported and what the delayed delivery means
     * for the snapshots passed in.
     */
    public static void addListener(RailwayBackendListener listener) {
        RailwayBackendEvents.register(listener);
    }

    /** Removes a listener again, reporting whether it was registered. */
    public static boolean removeListener(RailwayBackendListener listener) {
        return RailwayBackendEvents.unregister(listener);
    }

    /**
     * The backend's own mutable record for a train.
     * <p>
     * This exposes internal state that changes as the backend runs and is not part of the stable
     * API. Prefer the snapshot queries; use this only for data no snapshot offers.
     */
    public static Optional<TrackedTrain> getTrackedTrain(UUID trainId) {
        return manager().getTrain(trainId);
    }

    private static TrainManager manager() {
        return TrainManager.getInstance();
    }

    private static java.util.stream.Stream<TrackedTrain> reportableTrains() {
        return manager().getAllTrains().stream()
            .filter(TrackedTrain::isReportable)
            .filter(x -> !x.isBlacklisted());
    }

    private static List<TrainSnapshot> filterBySection(Predicate<JourneySection> filter) {
        return reportableTrains()
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

        for (StationCallIndex.Call call : manager().getCallIndex().callsAt(stationName, false)) {
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
            tags.stream().map(TagRef::of).toList(),
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

        for (TrackedTrain train : reportableTrains().toList()) {
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

        for (TrackedTrain train : reportableTrains().toList()) {
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
