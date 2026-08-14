package de.mrjulsen.crn.api.core;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.api.core.query.*;
import de.mrjulsen.crn.api.core.ref.TrainCategoryRef;
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
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.resources.ResourceLocation;

/**
 * The entry point for reading the train data backend. Everything behind it is internal and may
 * change between versions.
 *
 * <h2>Return values</h2>
 * Every query returns an immutable snapshot of the state at the moment it was called. A snapshot is
 * never updated afterwards, so query again for fresh data. A query that cannot answer returns an
 * empty {@link Optional} or an empty list, never {@code null}.
 *
 * <h2>Time</h2>
 * Every time and duration is measured in game ticks on the backend's own time base, the world time
 * adjusted by the configured time settings. Compare snapshot timestamps against {@link #getCurrentTime()}
 * rather than against the raw world time.
 *
 * <h2>Filtering</h2>
 * A query that lists things leaves out blacklisted trains and stations and trains whose data is not
 * yet reliable, so its results are safe to show publicly. A query that looks a single thing up by its
 * id does no such filtering, so a caller that already knows what it wants always gets it.
 *
 * <h2>Threading</h2>
 * Queries may be called from any thread. A single snapshot is consistent in itself, but two snapshots
 * from separate calls need not agree, since the backend can update in between. Prefer one
 * {@link JourneySnapshot} over piecing a run together from separate stop queries.
 *
 * <h2>Availability</h2>
 * The backend exists only while a server is running. Check {@link #isActive()} first; otherwise
 * queries return empty results.
 */
public final class RailwayBackendApi {

    private RailwayBackendApi() {}

    /** Whether a server is running and the backend is available to answer queries. */
    public static boolean isActive() {
        return RailwayBackend.isActive();
    }

    /**
     * The current time on the backend's own time base. Compare snapshot timestamps against this value
     * rather than against the raw world time.
     */
    public static long getCurrentTime() {
        return ModUtils.getTransformedWorldTime();
    }

    /** Every train fit to be shown publicly. */
    public static synchronized List<TrainSnapshot> getAllTrains() {
        return getTrackedTrains().map(TrainSnapshot::of).toList();
    }

    /** The trains matching the given query. */
    public static synchronized List<TrainSnapshot> getTrains(TrainQuery query) {
        return getTrains(query, x -> true);
    }

    /**
     * The trains matching the given query, narrowed further by a filter on the finished snapshot for
     * conditions the query cannot express.
     */
    public static synchronized List<TrainSnapshot> getTrains(TrainQuery query, Predicate<TrainSnapshot> filter) {
        List<TrainSnapshot> trains = new ArrayList<>(TrainManager.getInstance().getAllTrains().size());
        for (TrackedTrain train : TrainManager.getInstance().getAllTrains()) {
            if (!query.accept(train)) {
                continue;
            }
            TrainSnapshot snapshot = TrainSnapshot.of(train);
            if (filter.test(snapshot)) {
                trains.add(snapshot);
            }
        }
        return trains;
    }

    /** One train by its id, whether or not it would be shown publicly, if it is being tracked. */
    public static synchronized Optional<TrainSnapshot> getTrain(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(TrainSnapshot::of);
    }

    /** How many trains the backend is tracking, including those hidden from public display. */
    public static int getTrackedTrainCount() {
        return TrainManager.getInstance().getAllTrains().size();
    }

    /** The full run of one train, stop by stop, if it is being tracked. */
    public static Optional<JourneySnapshot> getJourney(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(JourneySnapshot::of);
    }

    /**
     * The run of one train as it will fall a number of whole schedule cycles from now. Meaningful only
     * for a repeating schedule; the run is returned unchanged where it cannot be projected.
     */
    public static Optional<JourneySnapshot> getJourneyIn(UUID trainId, int cycles) {
        return getJourney(trainId).map(x -> x.advancedBy(cycles));
    }

    /** Where one train is and how fast it is going, if it is being tracked. */
    public static Optional<TrainPositionSnapshot> getPosition(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(x -> TrainPositionSnapshot.of(x.getTrain(), x.getExitSide()));
    }

    /** The positions of every train fit to be shown publicly. */
    public static List<TrainPositionSnapshot> getAllPositions() {
        return getAllPositions(TrainPositionQuery.all());
    }

    /** The positions of the trains matching the given query. */
    public static List<TrainPositionSnapshot> getAllPositions(TrainPositionQuery query) {
        return getTrackedTrains()
                .map(x -> TrainPositionSnapshot.of(x.getTrain(), x.getExitSide()))
                .filter(query::accept)
                .toList();
    }

    /** What one train is made of, carriage by carriage, if it is being tracked. */
    public static Optional<TrainCompositionSnapshot> getComposition(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(x -> TrainCompositionSnapshot.of(x.getTrain()));
    }

    /**
     * The projected speed of one train over the stretch ahead. The horizon is the distance to look
     * ahead in blocks; pass a value of zero or less to use the train's remaining distance to its
     * destination.
     */
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

    /** Why one train is late or out of service, and by how much, if it is being tracked. */
    public static Optional<DelayReport> getDelayReport(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId).map(DelayReport::of);
    }

    /** A report for every train that is currently running late or out of service. */
    public static List<DelayReport> getDisruptions() {
        return getTrackedTrains()
            .filter(x -> x.isDelayed() || x.isCancelled())
            .map(DelayReport::of)
            .toList();
    }

    /**
     * Reports an external reason for a train's delay, which the backend takes into account until it
     * expires after the given number of ticks. Returns whether the train was found and the reason
     * recorded.
     */
    public static boolean reportDelay(UUID trainId, ResourceLocation causeId, long expiresInTicks, DelayArgument... args) {
        return ExternalDelayReports.report(trainId, causeId, expiresInTicks, -1, args);
    }

    /**
     * Withdraws a reason previously reported through {@link #reportDelay}. Returns whether such a
     * reason was in force.
     */
    public static boolean withdrawDelay(UUID trainId, ResourceLocation causeId) {
        return ExternalDelayReports.withdraw(trainId, causeId);
    }

    /** The calls at a station, ordered by arrival, as a board would show them. */
    public static List<BoardEntry> getBoard(String stationName, BoardQuery query) {
        return buildBoard(TrainManager.getInstance().getCallIndex().callsAt(stationName, query.includeDivertedAway()), query, Comparator.comparingLong(x -> x.realtime().arrival()));
    }

    /** The calls at every station covered by a tag, ordered by arrival, as one shared board. */
    public static List<BoardEntry> getBoard(StationTag stationTag, BoardQuery query) {
        return buildBoard(TrainManager.getInstance().getCallIndex().callsAt(stationTag, query.includeDivertedAway()), query, Comparator.comparingLong(x -> x.realtime().arrival()));
    }

    /** The same as {@link #getBoard(StationTag, BoardQuery)}, looking up the tag by its id. */
    public static List<BoardEntry> getBoard(UUID stationTagId, BoardQuery query) {
        return GlobalSettings.getInstance().getStationTag(stationTagId)
            .map(tag -> getBoard(tag, query))
            .orElse(List.of());
    }

    /** The next train to arrive at a station, if any is expected. */
    public static Optional<BoardEntry> getNextArrival(String stationName, BoardQuery query) {
        return buildBoard(TrainManager.getInstance().getCallIndex().callsAt(stationName, query.includeDivertedAway()), query.withLimit(1), Comparator.comparingLong(x -> x.realtime().arrival()))
                .stream()
                .findFirst();
    }

    /** The next train to depart from a station, if any is expected. */
    public static Optional<BoardEntry> getNextDeparture(String stationName, BoardQuery query) {
        return buildBoard(TrainManager.getInstance().getCallIndex().callsAt(stationName, query.includeDivertedAway()), query.withLimit(1), Comparator.comparingLong(x -> x.realtime().departure()))
                .stream()
                .findFirst();
    }

    /** The names of every station the backend knows about. */
    public static Set<String> getKnownStations() {
        return TrainUtils.getAllStationNames();
    }

    /** What is known about one station, if a station of that name exists. */
    public static Optional<StationSnapshot> getStation(String stationName) {
        if (stationName == null || !TrainUtils.getAllStationNames().contains(stationName)) {
            return Optional.empty();
        }
        return Optional.of(buildStation(stationName));
    }

    /** Every station, in name order. */
    public static List<StationSnapshot> getAllStations() {
        return getAllStations(StationQuery.all());
    }

    /** The stations matching the given query, in name order. */
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

    /** Every station tag configured on the server. */
    public static List<StationTag> getAllStationTags() {
        return GlobalSettings.getInstance().getAllStationTags();
    }

    /** One station tag by its id, if it exists. */
    public static Optional<StationTag> getStationTag(UUID stationTagId) {
        return GlobalSettings.getInstance().getStationTag(stationTagId);
    }

    /** One line together with the trains working it, if a line with that id exists. */
    public static Optional<LineSnapshot> getLine(UUID lineId) {
        return GlobalSettings.getInstance().getTrainLine(lineId).map(RailwayBackendApi::buildLine);
    }

    /** Every line together with the trains working it. */
    public static List<LineSnapshot> getAllLines() {
        return getAllLines(LineQuery.all());
    }

    /** The lines matching the given query. */
    public static List<LineSnapshot> getAllLines(LineQuery query) {
        return GlobalSettings.getInstance().getAllTrainLines()
                .stream()
                .map(RailwayBackendApi::buildLine)
                .filter(query::accept)
                .toList();
    }

    /** One category together with the trains running under it, if a category with that id exists. */
    public static Optional<CategorySnapshot> getCategory(UUID categoryId) {
        return GlobalSettings.getInstance().getTrainCategory(categoryId).map(RailwayBackendApi::buildCategory);
    }

    /** Every category together with the trains running under it. */
    public static List<CategorySnapshot> getAllCategories() {
        return getAllCategories(CategoryQuery.all());
    }

    /** The categories matching the given query. */
    public static List<CategorySnapshot> getAllCategories(CategoryQuery query) {
        return GlobalSettings.getInstance().getAllTrainCategories()
                .stream()
                .map(RailwayBackendApi::buildCategory)
                .filter(query::accept)
                .toList();
    }

    /**
     * When a train last departed from a station, chosen by how the departure should relate to a given
     * train: the same line, the same category, the same name, or any train at all. Returns
     * {@link DepartureLog#NEVER} where no such departure has been recorded.
     */
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

    /** Summary figures about the departures recorded at a station, such as how punctual they run. */
    public static DepartureStats getDepartureStats(String stationName) {
        GlobalSettings settings = GlobalSettings.getInstance();
        return TrainManager.getInstance().getDepartureLog().getStats(stationName,
            lineId -> settings.getTrainLine(lineId).map(TrainLine::getLineName).orElse(null),
            categoryId -> settings.getTrainCategory(categoryId).map(TrainCategory::getCategoryName).orElse(null));
    }

    /** Registers a listener to be notified of backend events. */
    public static void addListener(RailwayBackendListener listener) {
        RailwayBackendEvents.register(listener);
    }

    /** Removes a previously registered listener. Returns whether it was registered. */
    public static boolean removeListener(RailwayBackendListener listener) {
        return RailwayBackendEvents.unregister(listener);
    }

    /**
     * The live tracking object behind a train. This exposes backend internals that are not part of
     * the stable snapshot contract and may change between versions; prefer the snapshot queries where
     * they suffice.
     */
    public static Optional<TrackedTrain> getTrackedTrain(UUID trainId) {
        return TrainManager.getInstance().getTrain(trainId);
    }

    /**
     * The color a service should be shown in: its line's color where set, otherwise its category's,
     * otherwise a neutral default.
     */
    public static DLColor getServiceColor(LineRef line, TrainCategoryRef category) {
        if (line != null && !line.color().isTransparent()) {
            return line.color();
        }
        if (category != null && !category.color().isTransparent()) {
            return category.color();
        }
        return Constants.COLOR_TRAIN_BACKGROUND;
    }


    private static Stream<TrackedTrain> getTrackedTrains() {
        return TrainManager.getInstance().getAllTrains().stream()
            .filter(x -> x.isReportable() && !x.isBlacklisted());
    }

    private static List<BoardEntry> buildBoard(List<StationCallIndex.Call> calls, BoardQuery query, Comparator<BoardEntry> order) {
        if (query.limit() <= 0) {
            return Collections.emptyList();
        }

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
            if (!query.acceptsTime(entry.realtime().departure())) {
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
            entries = entries.subList(0, query.limit());
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

        return new CategorySnapshot(TrainCategoryRef.of(category), trainIds, resolveLines(lineIds), delayed);
    }

    private static List<LineRef> resolveLines(Set<UUID> lineIds) {
        GlobalSettings settings = GlobalSettings.getInstance();
        List<LineRef> lines = new ArrayList<>(lineIds.size());
        for (UUID id : lineIds) {
            Optional<TrainLine> line = settings.getTrainLine(id);
            line.ifPresent(x -> lines.add(LineRef.of(x)));
        }
        return lines;
    }

    private static List<TrainCategoryRef> resolveCategories(Set<UUID> categoryIds) {
        GlobalSettings settings = GlobalSettings.getInstance();
        List<TrainCategoryRef> categories = new ArrayList<>(categoryIds.size());
        for (UUID id : categoryIds) {
            Optional<TrainCategory> category = settings.getTrainCategory(id);
            category.ifPresent(x -> categories.add(TrainCategoryRef.of(x)));
        }
        return categories;
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
