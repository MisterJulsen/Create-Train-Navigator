package de.mrjulsen.crn.navigator.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.backend.api.CategoryRef;
import de.mrjulsen.crn.backend.api.JourneySnapshot;
import de.mrjulsen.crn.backend.api.LineRef;
import de.mrjulsen.crn.backend.api.SectionSnapshot;
import de.mrjulsen.crn.backend.api.ServiceColor;
import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.backend.api.StopSnapshot;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.backend.timing.StopTimes;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * One ride on one service: the traveller gets on at the first call and off at the last, staying
 * seated for everything in between.
 * <p>
 * A leg never spans a change of train and never spans a change of service, so the line, the category
 * and the advertised destination hold for the whole of it.
 *
 * @param trainId         The id of the train operating this leg.
 * @param sessionId       The train's tracking session at the time of the search. A change of it means
 *                        the route was planned against data that no longer applies.
 * @param trainName       The train's own name. What to actually show is {@link #displayName()}.
 * @param iconId          The id of the train's icon, or {@code null} if it carries none. Display data
 *                        like the name and the line, carried along so a leg can be drawn without
 *                        having to ask the backend about the train again.
 * @param line            The train line this leg runs on, or {@link LineRef#NONE} if it carries none.
 * @param category        The train category this leg carries, or {@link CategoryRef#NONE}.
 * @param destinationText What the train advertises as its destination, for the departure board.
 * @param sectionIndex    The position of the section this leg is operated as in the train's journey.
 * @param cancelled       Whether the train was out of service because of a disruption when the route
 *                        was searched. Like everything else here this is a moment in time - re-run
 *                        the search to learn about a train cancelled since.
 * @param calls           Every station this leg calls at, boarding first and alighting last.
 * @param delays          Every reason this ride was ever delayed. Written while the journey is being
 *                        travelled and kept for good afterwards; see {@link DelayLog}.
 */
public record RouteLeg(
    UUID trainId,
    UUID sessionId,
    String trainName,
    ResourceLocation iconId,
    LineRef line,
    CategoryRef category,
    String destinationText,
    int sectionIndex,
    boolean cancelled,
    List<RouteCall> calls,
    DelayLog delays
) {

    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_SESSION_ID = "SessionId";
    private static final String NBT_TRAIN_NAME = "TrainName";
    private static final String NBT_ICON_ID = "IconId";
    private static final String NBT_LINE = "Line";
    private static final String NBT_CATEGORY = "Category";
    private static final String NBT_DESTINATION_TEXT = "DestinationText";
    private static final String NBT_SECTION_INDEX = "SectionIndex";
    private static final String NBT_CANCELLED = "Cancelled";
    private static final String NBT_CALLS = "Calls";
    private static final String NBT_DELAYS = "Delays";

    public RouteLeg {
        calls = calls == null ? List.of() : List.copyOf(calls);
        trainName = trainName == null ? "" : trainName;
        line = line == null ? LineRef.NONE : line;
        category = category == null ? CategoryRef.NONE : category;
        destinationText = destinationText == null ? "" : destinationText;
        delays = delays == null ? new DelayLog() : delays;
    }

    /** A freshly planned leg, which has no delay history yet. */
    public RouteLeg(UUID trainId, UUID sessionId, String trainName, ResourceLocation iconId, LineRef line,
                    CategoryRef category, String destinationText, int sectionIndex, boolean cancelled,
                    List<RouteCall> calls) {
        this(trainId, sessionId, trainName, iconId, line, category, destinationText, sectionIndex, cancelled,
            calls, new DelayLog());
    }

    /**
     * What to show as this leg's operator: the name of the line it runs on, or the train's own name
     * if it carries no line or the line is unnamed.
     */
    public String displayName() {
        return line.hasName() ? line.name() : trainName;
    }

    /**
     * What to paint this leg in: the colour of the line it runs on, the colour of its category if
     * the line carries none, and a neutral default if neither does.
     */
    public DLColor displayColor() {
        return ServiceColor.of(line, category);
    }

    /** Where the traveller gets on. */
    public RouteCall boarding() {
        return calls.get(0);
    }

    /** Where the traveller gets off. */
    public RouteCall alighting() {
        return calls.get(calls.size() - 1);
    }

    /**
     * The station the traveller gets on at, as the train really serves it. Everything on this level
     * describes the journey as it will actually happen, so it is the realtime side throughout; the
     * timetable's version of a call is reached through {@link #boarding()} / {@link #alighting()}.
     */
    public StationRef from() {
        return boarding().realtimeStation();
    }

    /** The station the traveller gets off at, as the train really serves it. */
    public StationRef to() {
        return alighting().realtimeStation();
    }

    /** When the train really leaves the boarding station, in transformed game ticks. */
    public long departure() {
        return boarding().realtime().departure();
    }

    /** When the train really reaches the alighting station, in transformed game ticks. */
    public long arrival() {
        return alighting().realtime().arrival();
    }

    /** How long the traveller spends on this train, in ticks. */
    public long duration() {
        return Math.max(0, arrival() - departure());
    }

    /** The stations passed between getting on and getting off, in travel order. */
    public List<RouteCall> intermediateCalls() {
        return calls.size() < 3 ? List.of() : calls.subList(1, calls.size() - 1);
    }

    /** How many stations lie between getting on and getting off. */
    public int intermediateStopCount() {
        return Math.max(0, calls.size() - 2);
    }

    /** Whether this leg runs on a train line at all. */
    public boolean hasLine() {
        return line.isKnown();
    }

    /** Whether this leg carries a train category at all. */
    public boolean hasCategory() {
        return category.isKnown();
    }

    /**
     * The whole of one section of a train's run, for views that show the service a traveller is on
     * rather than only the stretch they ride - the entire Munich to Hamburg run while they sit in it
     * from Augsburg to Ulm.
     * <p>
     * Every stop of the section becomes a call, all of them on the <em>same run</em>: the one the
     * anchor belongs to. That is what this exists for. The backend reports each stop's next
     * occurrence, so a stop the train has already served this run reports a time a full cycle later
     * than one it has not - put side by side without projecting, the section reads 18:00, 18:30,
     * 07:30, and the traveller is looking at two different journeys at once.
     * <p>
     * Stops the train has already served on that run are filled in with what it actually did and are
     * sealed straight away, so they never move again.
     * <p>
     * The times are taken as the backend reports them and are not collapsed at the ends the way a
     * searched leg's are: showing a service's own run, when it arrives at its first stop and leaves
     * its last are real facts about it rather than noise.
     *
     * @param sectionIndex Which section of the run to show.
     * @param anchor       A call on the run to show, which fixes which cycle every stop is taken on.
     *                     Pass {@code null} for the run the train is working through now.
     */
    public static RouteLeg ofSection(TrainSnapshot train, JourneySnapshot journey, int sectionIndex, RouteCall anchor) {
        List<Integer> positions = sectionStopPositions(journey, sectionIndex);
        int laps = lapsOf(journey, anchor);

        List<RouteCall> calls = new ArrayList<>(positions.size());
        int lap = 0;
        int previousPosition = -1;
        for (int position : positions) {
            if (position <= previousPosition) {
                lap++;
            }
            previousPosition = position;
            StopSnapshot stop = journey.stops().get(position);
            calls.add(callOf(stop, runVisit(journey, position, stop) + laps + lap, journey.totalDuration()));
        }

        SectionSnapshot section = journey.section(sectionIndex).orElse(null);
        return new RouteLeg(train.trainId(), train.sessionId(), train.trainName(), train.iconId(),
            section == null ? train.line() : section.line(),
            section == null ? train.category() : section.category(),
            section == null ? train.destinationText() : section.destination().displayName(),
            sectionIndex, train.isCancelled(), calls);
    }

    /** The whole of the section the train is working through right now. */
    public static RouteLeg ofCurrentSection(TrainSnapshot train, JourneySnapshot journey) {
        return ofSection(train, journey, journey.currentSectionIndex(), null);
    }

    /**
     * Where the section's stops sit in the journey's stop list, in travel order. A section that runs
     * into the next one carries that one's first stop as well, so the traveller is shown the station
     * the service actually hands over at.
     * <p>
     * The last section of a cyclic run hands over to the first one, which on a schedule of A, B, C, D
     * makes the handover station A again - the A of the following cycle, not the one this section
     * started from. A position that does not move forward is what says so.
     */
    private static List<Integer> sectionStopPositions(JourneySnapshot journey, int sectionIndex) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < journey.stops().size(); i++) {
            if (journey.stops().get(i).sectionIndex() == sectionIndex) {
                positions.add(i);
            }
        }
        if (journey.section(sectionIndex).map(SectionSnapshot::includesNextSectionStart).orElse(false)) {
            nextSectionStart(journey, sectionIndex).ifPresent(positions::add);
        }
        return positions;
    }

    /** Where the section after the given one starts, wrapping to the first if the run repeats. */
    private static Optional<Integer> nextSectionStart(JourneySnapshot journey, int sectionIndex) {
        int next = sectionIndex + 1;
        if (journey.section(next).isEmpty()) {
            if (!journey.cyclic()) {
                return Optional.empty();
            }
            next = 0;
        }
        for (int i = 0; i < journey.stops().size(); i++) {
            if (journey.stops().get(i).sectionIndex() == next) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    /**
     * Which visit of a stop the train's current run is, counting the way {@link RouteCall#cycle()}
     * does. A stop the train has already served this run reports its next occurrence, which is one
     * visit further on than the run being counted.
     */
    private static int runVisit(JourneySnapshot journey, int position, StopSnapshot stop) {
        return journey.currentStopIndex() >= 0 && position < journey.currentStopIndex()
            ? stop.completedVisits() - 1
            : stop.completedVisits();
    }

    /** How many whole cycles ahead of the current run the anchor's run is. */
    private static int lapsOf(JourneySnapshot journey, RouteCall anchor) {
        if (anchor == null) {
            return 0;
        }
        for (int i = 0; i < journey.stops().size(); i++) {
            StopSnapshot stop = journey.stops().get(i);
            if (stop.entryIndex() == anchor.entryIndex()) {
                return anchor.cycle() - runVisit(journey, i, stop);
            }
        }
        return 0;
    }

    /** One stop as the given cycle of the run reaches it. */
    private static RouteCall callOf(StopSnapshot stop, int cycle, long cycleDuration) {
        int cyclesAhead = cycle - stop.completedVisits();
        if (cyclesAhead >= 0) {
            StopSnapshot projected = stop.advancedBy(cyclesAhead, cycleDuration);
            return new RouteCall(projected.scheduledStation(), projected.realtimeStation(),
                projected.entryIndex(), cycle, projected.scheduled(), projected.realtime());
        }

        long shift = cyclesAhead * Math.max(0, cycleDuration);
        StopTimes actual = cyclesAhead == -1 && stop.previousActual().isKnown()
            ? stop.previousActual()
            : stop.realtime().shifted(shift);

        RouteCall call = new RouteCall(stop.scheduledStation(), stop.realtimeStation(), stop.entryIndex(),
            cycle, stop.scheduled().shifted(shift), actual);
        call.markPassed();
        return call;
    }

    /** Serializes this leg. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        NbtHelper.putNullableUUID(nbt, NBT_TRAIN_ID, trainId);
        NbtHelper.putNullableUUID(nbt, NBT_SESSION_ID, sessionId);
        nbt.putString(NBT_TRAIN_NAME, trainName);
        if (iconId != null) {
            nbt.putString(NBT_ICON_ID, iconId.toString());
        }
        nbt.put(NBT_LINE, line.toNbt());
        nbt.put(NBT_CATEGORY, category.toNbt());
        nbt.putString(NBT_DESTINATION_TEXT, destinationText);
        nbt.putInt(NBT_SECTION_INDEX, sectionIndex);
        nbt.putBoolean(NBT_CANCELLED, cancelled);
        nbt.put(NBT_CALLS, NbtHelper.writeList(calls, RouteCall::toNbt));
        nbt.put(NBT_DELAYS, delays.toNbt());
        return nbt;
    }

    /** Deserializes a leg written by {@link #toNbt()}. */
    public static RouteLeg fromNbt(CompoundTag nbt) {
        return new RouteLeg(
            NbtHelper.readNullableUUID(nbt, NBT_TRAIN_ID),
            NbtHelper.readNullableUUID(nbt, NBT_SESSION_ID),
            nbt.getString(NBT_TRAIN_NAME),
            nbt.contains(NBT_ICON_ID) ? new ResourceLocation(nbt.getString(NBT_ICON_ID)) : null,
            LineRef.fromNbt(nbt.getCompound(NBT_LINE)),
            CategoryRef.fromNbt(nbt.getCompound(NBT_CATEGORY)),
            nbt.getString(NBT_DESTINATION_TEXT),
            nbt.getInt(NBT_SECTION_INDEX),
            nbt.getBoolean(NBT_CANCELLED),
            NbtHelper.readList(nbt, NBT_CALLS, RouteCall::fromNbt),
            DelayLog.fromNbt(nbt.getCompound(NBT_DELAYS))
        );
    }

    /**
     * Two legs are the same ride when the same train serves the same calls as the same section of its
     * journey. What the ride has done since - its delay history, the live half of its calls - is left
     * out on purpose: a leg that stopped equalling itself the moment a train ran late would take a
     * saved route off the saved list.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof RouteLeg other
            && Objects.equals(trainId, other.trainId)
            && sectionIndex == other.sectionIndex
            && calls.equals(other.calls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trainId, sectionIndex, calls);
    }

    @Override
    public String toString() {
        return displayName() + " " + from().name() + " " + departure() + " -> " + to().name() + " " + arrival();
    }
}
