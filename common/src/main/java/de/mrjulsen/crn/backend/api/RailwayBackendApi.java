package de.mrjulsen.crn.backend.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import de.mrjulsen.crn.backend.RailwayBackend;
import de.mrjulsen.crn.backend.TrainManager;
import de.mrjulsen.crn.backend.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.backend.api.event.RailwayBackendListener;
import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.delay.DelayArgument;
import de.mrjulsen.crn.backend.delay.ExternalDelayReports;
import de.mrjulsen.crn.backend.history.DepartureLogEntry;
import de.mrjulsen.crn.backend.index.StationCallIndex;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.backend.schedule.TrainJourney;
import de.mrjulsen.crn.backend.timing.SpeedProfileEstimator;
import de.mrjulsen.crn.backend.timing.StopTimings;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.util.ModUtils;
import net.minecraft.resources.ResourceLocation;

/**
 * The query API of the train data backend, and the single entry point for everything that wants to
 * know what the trains are doing: in-game screens, displays, the navigator and any external
 * interface built on top of them.
 *
 * <h2>What to ask for</h2>
 * Every query returns an immutable snapshot, layered so a caller pays only for what it needs:
 * <ul>
 *   <li>{@link TrainSnapshot} - a train's overall state. Enough for a list, a status panel or a
 *       map marker.</li>
 *   <li>{@link JourneySnapshot} - the whole run stop by stop, grouped into
 *       {@linkplain SectionSnapshot sections}, marked with what is done and what is to come.</li>
 *   <li>{@link BoardEntry} - one call at one station, for departure and arrival boards.</li>
 *   <li>{@link DelayReport} - why a train is late, and by how much.</li>
 *   <li>{@link TrainPositionSnapshot} and {@link TrainCompositionSnapshot} - where a train is and
 *       what it is made of, for maps and detail views.</li>
 *   <li>{@link StationSnapshot}, {@link LineSnapshot}, {@link CategorySnapshot} - the network as
 *       seen from a station, a line or a category.</li>
 * </ul>
 * <h2>What is withheld</h2>
 * Every query that <em>lists</em> trains, calls or stops respects the train and station blacklists
 * as well as section usability, so anything it returns is fit to show publicly. A blacklisted train
 * keeps being tracked and keeps learning in the background - it is withheld here, not forgotten - so
 * taking one off the boards and putting it back costs nothing. A blacklisted station is left out of
 * journeys and boards alike, except where it is the stop a train is currently at, which stays so the
 * train's own position remains describable.
 * <p>
 * Lookups <em>by identity</em> - {@link #getTrain(UUID)}, {@link #getTrainByName(String)},
 * {@link #getJourney(UUID)} and the other id-based queries - deliberately do not filter, so a caller
 * that already knows which train it wants always gets it. Anything building a public display should
 * use the listing queries.
 *
 * <h2>Threading</h2>
 * Queries may be called from any thread. Everything reachable from here is either immutable or held
 * in concurrent state, so a query never corrupts data and never throws because of a concurrent
 * update. A few queries read the live train objects and are marked <b>server thread only</b>.
 * <p>
 * A snapshot assembled from several queries is not guaranteed to be internally consistent, since an
 * update may land in between. A {@link JourneySnapshot} is, so prefer one of those over stitching a
 * run together from individual stop queries.
 */
public final class RailwayBackendApi {

    private RailwayBackendApi() {}

    /** Whether the backend is currently running, i.e. a server is active. */
    public static boolean isActive() {
        return RailwayBackend.isActive();
    }

    /** The backend's current time in transformed game ticks, the unit of every time in this API. */
    public static long currentTime() {
        return ModUtils.getTransformedWorldTime();
    }

    /**
     * Every train worth showing: those in service, plus those out of service whose reason is still
     * being reported. A train parked long ago is left out, but its data survives and can still be
     * reached by id.
     */
    public static List<TrainSnapshot> getAllTrains() {
        return reportableTrains().map(TrainSnapshot::of).toList();
    }

    /** The train with the given id, whether or not it is currently worth showing. */
    public static Optional<TrainSnapshot> getTrain(UUID trainId) {
        return manager().getTrain(trainId).map(TrainSnapshot::of);
    }

    /** The first tracked train with the given name. */
    public static Optional<TrainSnapshot> getTrainByName(String trainName) {
        return manager().getAllTrains().stream()
            .filter(x -> x.getTrainName().equals(trainName))
            .findFirst()
            .map(TrainSnapshot::of);
    }

    /** Every train matching the given test, evaluated against its snapshot. */
    public static List<TrainSnapshot> getTrains(Predicate<TrainSnapshot> filter) {
        return reportableTrains().map(TrainSnapshot::of).filter(filter).toList();
    }

    /** Every train that is currently late. */
    public static List<TrainSnapshot> getDelayedTrains() {
        return reportableTrains().filter(TrackedTrain::isDelayed).map(TrainSnapshot::of).toList();
    }

    /** Every train that is currently out of service. */
    public static List<TrainSnapshot> getCancelledTrains() {
        return reportableTrains().filter(TrackedTrain::isCancelled).map(TrainSnapshot::of).toList();
    }

    /** Every train currently operating on the given line. */
    public static List<TrainSnapshot> getTrainsOfLine(UUID lineId) {
        return filterBySection(section -> lineId != null && lineId.equals(section.getTrainLineId()));
    }

    /** Every train currently operating under the given category. */
    public static List<TrainSnapshot> getTrainsOfCategory(UUID categoryId) {
        return filterBySection(section -> categoryId != null && categoryId.equals(section.getTrainCategoryId()));
    }

    /** How many trains the backend is currently tracking, including those not worth showing. */
    public static int getTrackedTrainCount() {
        return manager().getAllTrains().size();
    }

    /** The full run of a train: every stop with its times, grouped into sections. */
    public static Optional<JourneySnapshot> getJourney(UUID trainId) {
        return manager().getTrain(trainId).map(JourneySnapshot::of);
    }

    /** The section a train is currently operating in, with its own stops and timetable. */
    public static Optional<SectionSnapshot> getCurrentSection(UUID trainId) {
        return getJourney(trainId).flatMap(JourneySnapshot::currentSection);
    }

    /** The stop a train is at or traveling towards. */
    public static Optional<StopSnapshot> getCurrentStop(UUID trainId) {
        return getJourney(trainId).flatMap(JourneySnapshot::currentStop);
    }

    /** The stop a train will call at after its current one. */
    public static Optional<StopSnapshot> getNextStop(UUID trainId) {
        return getJourney(trainId).flatMap(JourneySnapshot::nextStop);
    }

    /**
     * Every stop a train still has to serve, in the order it will reach them. On a cyclic journey
     * the list wraps around and covers a full cycle.
     */
    public static List<StopSnapshot> getUpcomingStops(UUID trainId) {
        return getJourney(trainId).map(JourneySnapshot::upcomingStops).orElse(List.of());
    }

    /**
     * When a train is next expected to call at a station, if it is scheduled to at all.
     *
     * @param stationName The exact station name, or a filter containing {@code *} wildcards.
     */
    public static Optional<StopSnapshot> getNextCallAt(UUID trainId, String stationName) {
        return getUpcomingStops(trainId).stream()
            .filter(x -> TrainUtils.stationMatches(x.realtimeStationName(), stationName))
            .findFirst();
    }

    /**
     * When a train next calls at a station at or after a given time, projected into later journey
     * cycles if its current run has already passed it.
     * <p>
     * Unlike {@link #getNextCallAt(UUID, String)}, which is bound to the run the train is on, this
     * answers the question a route search asks: could someone standing at this station at this time
     * still board this train, and when. On a non-cyclic journey there is only the current run.
     *
     * @param stationName The exact station name, or a filter containing {@code *} wildcards.
     * @param notBefore   The earliest acceptable arrival, in transformed game ticks.
     */
    public static Optional<StopSnapshot> getNextCallAt(UUID trainId, String stationName, long notBefore) {
        return getJourney(trainId).flatMap(x -> x.nextCallAt(stationName, notBefore));
    }

    /**
     * A train's journey as it will run {@code cycles} cycles from now. Returns the current run for a
     * journey that does not repeat.
     *
     * @see de.mrjulsen.crn.backend.timing.CycleProjector
     */
    public static Optional<JourneySnapshot> getJourneyIn(UUID trainId, int cycles) {
        return getJourney(trainId).map(x -> x.advancedBy(cycles));
    }

    /**
     * Where a train is and how fast it is moving. Small enough to poll for many trains at once.
     * <p>
     * <b>Server thread only.</b>
     */
    public static Optional<TrainPositionSnapshot> getPosition(UUID trainId) {
        return manager().getTrain(trainId).map(x -> TrainPositionSnapshot.of(x.getTrain()));
    }

    /**
     * Where every reportable train is, for a live map.
     * <p>
     * <b>Server thread only.</b>
     */
    public static List<TrainPositionSnapshot> getAllPositions() {
        return reportableTrains().map(x -> TrainPositionSnapshot.of(x.getTrain())).toList();
    }

    /**
     * What a train is made of: its carriages, their sizes and where they are.
     * <p>
     * <b>Server thread only.</b>
     */
    public static Optional<TrainCompositionSnapshot> getComposition(UUID trainId) {
        return manager().getTrain(trainId).map(x -> TrainCompositionSnapshot.of(x.getTrain()));
    }

    /**
     * The speed limits in force on the stretch ahead of a train, as reported by the registered
     * {@link ISpeedLimitProvider}s.
     * <p>
     * <b>Server thread only</b>, since the providers are handed the live train object.
     *
     * @param horizon How far ahead to look, in blocks. {@code 0} or less uses the remaining
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

    /** Why a train is late, and by how much. */
    public static Optional<DelayReport> getDelayReport(UUID trainId) {
        return manager().getTrain(trainId).map(DelayReport::of);
    }

    /** A delay report for every train that is currently late or out of service. */
    public static List<DelayReport> getDisruptions() {
        return reportableTrains()
            .filter(x -> x.isDelayed() || x.isCancelled())
            .map(DelayReport::of)
            .toList();
    }

    /**
     * Attaches a status reason to a train from outside the backend, for a situation the backend
     * cannot observe by itself.
     *
     * @return Whether the report was accepted. Rejected if the cause is not registered.
     * @see ExternalDelayReports
     */
    public static boolean reportDelay(UUID trainId, ResourceLocation causeId, long expiresInTicks, DelayArgument... args) {
        return ExternalDelayReports.report(trainId, causeId, expiresInTicks, -1, args);
    }

    /** Removes a status reason previously attached via {@link #reportDelay}. */
    public static boolean withdrawDelay(UUID trainId, ResourceLocation causeId) {
        return ExternalDelayReports.withdraw(trainId, causeId);
    }

    /**
     * The departure board of a station: every upcoming call by every train serving it, sorted by
     * projected departure.
     *
     * @param stationName The exact station name, or a filter containing {@code *} wildcards.
     */
    public static List<BoardEntry> getDepartures(String stationName, BoardQuery query) {
        return board(manager().getCallIndex().callsAt(stationName), query,
            Comparator.comparingLong(BoardEntry::realtimeDeparture));
    }

    /** The combined departure board of every station carrying the given tag. */
    public static List<BoardEntry> getDepartures(StationTag stationTag, BoardQuery query) {
        return board(manager().getCallIndex().callsAt(stationTag), query,
            Comparator.comparingLong(BoardEntry::realtimeDeparture));
    }

    /** The arrival board of a station, sorted by projected arrival. */
    public static List<BoardEntry> getArrivals(String stationName, BoardQuery query) {
        return board(manager().getCallIndex().callsAt(stationName), query,
            Comparator.comparingLong(BoardEntry::realtimeArrival));
    }

    /** The combined arrival board of every station carrying the given tag. */
    public static List<BoardEntry> getArrivals(StationTag stationTag, BoardQuery query) {
        return board(manager().getCallIndex().callsAt(stationTag), query,
            Comparator.comparingLong(BoardEntry::realtimeArrival));
    }

    /** The combined departure board of every station carrying the tag with the given id. */
    public static List<BoardEntry> getDepartures(UUID stationTagId, BoardQuery query) {
        return GlobalSettings.getInstance().getStationTag(stationTagId)
            .map(tag -> getDepartures(tag, query))
            .orElse(List.of());
    }

    /** The combined arrival board of every station carrying the tag with the given id. */
    public static List<BoardEntry> getArrivals(UUID stationTagId, BoardQuery query) {
        return GlobalSettings.getInstance().getStationTag(stationTagId)
            .map(tag -> getArrivals(tag, query))
            .orElse(List.of());
    }

    /** The next departure from a station, if any train is due. */
    public static Optional<BoardEntry> getNextDeparture(String stationName, BoardQuery query) {
        return getDepartures(stationName, query.withLimit(1)).stream().findFirst();
    }

    /** The next arrival at a station, if any train is due. */
    public static Optional<BoardEntry> getNextArrival(String stationName, BoardQuery query) {
        return getArrivals(stationName, query.withLimit(1)).stream().findFirst();
    }

    /** The names of every station in the track network. */
    public static Set<String> getKnownStations() {
        return TrainUtils.getAllStationNames();
    }

    /** A station with its tags and the services currently calling there. */
    public static Optional<StationSnapshot> getStation(String stationName) {
        if (stationName == null || !TrainUtils.getAllStationNames().contains(stationName)) {
            return Optional.empty();
        }
        return Optional.of(buildStation(stationName));
    }

    /** Every station in the track network, with its tags and services. */
    public static List<StationSnapshot> getAllStations() {
        return TrainUtils.getAllStationNames().stream().sorted().map(RailwayBackendApi::buildStation).toList();
    }

    /** A station by name, with its tag attached but without looking up the services calling there. */
    public static StationRef getStationRef(String stationName) {
        return StationRef.of(stationName);
    }

    /** Every configured station tag. */
    public static List<StationTag> getAllStationTags() {
        return GlobalSettings.getInstance().getAllStationTags();
    }

    /** The station tag with the given id. */
    public static Optional<StationTag> getStationTag(UUID stationTagId) {
        return GlobalSettings.getInstance().getStationTag(stationTagId);
    }

    /** A train line with the trains and stations it currently covers. */
    public static Optional<LineSnapshot> getLine(UUID lineId) {
        return GlobalSettings.getInstance().getTrainLine(lineId).map(RailwayBackendApi::buildLine);
    }

    /** Every configured train line, whether or not it is currently being operated. */
    public static List<LineSnapshot> getAllLines() {
        return GlobalSettings.getInstance().getAllTrainLines().stream().map(RailwayBackendApi::buildLine).toList();
    }

    /** A train category with the trains and lines currently carrying it. */
    public static Optional<CategorySnapshot> getCategory(UUID categoryId) {
        return GlobalSettings.getInstance().getTrainCategory(categoryId).map(RailwayBackendApi::buildCategory);
    }

    /** Every configured train category. */
    public static List<CategorySnapshot> getAllCategories() {
        return GlobalSettings.getInstance().getAllTrainCategories().stream().map(RailwayBackendApi::buildCategory).toList();
    }

    /** The departures already recorded at a station, oldest first. */
    public static List<DepartureLogEntry> getDepartureHistory(String stationName) {
        return manager().getDepartureLog().getDepartures(stationName);
    }

    /** The most recent departure from a station matching the given test. */
    public static Optional<DepartureLogEntry> getLastDeparture(String stationName, Predicate<DepartureLogEntry> filter) {
        return manager().getDepartureLog().getLastDeparture(stationName, filter);
    }

    /**
     * Subscribes to what happens to the trains, instead of polling for changes. Events are delivered
     * on the server thread, one at a time.
     *
     * @see RailwayBackendListener
     */
    public static void addListener(RailwayBackendListener listener) {
        RailwayBackendEvents.register(listener);
    }

    /** Unsubscribes a listener registered with {@link #addListener(RailwayBackendListener)}. */
    public static boolean removeListener(RailwayBackendListener listener) {
        return RailwayBackendEvents.unregister(listener);
    }

    /**
     * Direct access to a tracked train, for consumers needing more than the snapshots provide.
     * The returned object is updated continuously by the backend; treat it as read-only.
     */
    public static Optional<TrackedTrain> getTrackedTrain(UUID trainId) {
        return manager().getTrain(trainId);
    }

    private static TrainManager manager() {
        return TrainManager.getInstance();
    }

    /**
     * The trains any public query may return: those worth showing and not withheld by the
     * blacklist. Every listing query goes through this, so a blacklisted train can never leak onto
     * a board, into a route search or into a station's service list - while the backend keeps
     * tracking it in the background.
     */
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

    /**
     * Collects, filters, sorts and trims the rows of one board.
     * <p>
     * The calls are taken from the {@linkplain StationCallIndex station index} rather than by
     * walking every train, so the cost scales with how many trains actually serve the station
     * instead of with the size of the network.
     */
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
            if (settings.isStationBlacklisted(stop.getStationName())) {
                continue;
            }
            if (!isServiceable(train.getJourney(), stop)) {
                continue;
            }
            StopTimings timing = train.getTimings(stop);
            if (timing == null || !query.acceptsTime(timing.getRealtime().departure())) {
                continue;
            }
            BoardEntry entry = BoardEntry.of(train, stop);
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

    /**
     * Whether a stop may appear on public boards: its section must be usable, or it must be the
     * first stop of an unusable section still covered by the previous, usable one.
     */
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

        for (StationCallIndex.Call call : manager().getCallIndex().callsAt(stationName)) {
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
