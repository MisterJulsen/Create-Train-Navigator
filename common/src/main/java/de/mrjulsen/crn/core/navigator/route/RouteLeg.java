package de.mrjulsen.crn.core.navigator.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.ref.TrainCategoryRef;
import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.snapshot.SectionSnapshot;
import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.core.timing.StopTimes;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * One leg of a journey: a stretch travelled aboard a single train, from where travellers board to
 * where they alight, with every call in between.
 * <p>
 * Times are in game ticks on the backend's time base.
 *
 * @param trainId         The id of the train worked on this leg.
 * @param sessionId       The train's tracking session at the time the leg was planned.
 * @param trainName       The train's own name.
 * @param iconId          The train's icon, or {@code null}.
 * @param line            The line the leg runs under, or {@link LineRef#NONE}.
 * @param category        The category the leg runs under, or {@link TrainCategoryRef#NONE}.
 * @param destinationText The destination to show for the leg.
 * @param sectionIndex    The section of the train's run this leg covers.
 * @param cancelled       Whether the train is out of service.
 * @param calls           The calls the leg makes, from boarding to alighting.
 * @param delays          The delays recorded against this leg.
 */
public record RouteLeg(
    UUID trainId,
    UUID sessionId,
    String trainName,
    ResourceLocation iconId,
    LineRef line,
    TrainCategoryRef category,
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
        category = category == null ? TrainCategoryRef.NONE : category;
        destinationText = destinationText == null ? "" : destinationText;
        delays = delays == null ? new DelayLog() : delays;
    }

    public RouteLeg(UUID trainId, UUID sessionId, String trainName, ResourceLocation iconId, LineRef line,
                    TrainCategoryRef category, String destinationText, int sectionIndex, boolean cancelled,
                    List<RouteCall> calls) {
        this(trainId, sessionId, trainName, iconId, line, category, destinationText, sectionIndex, cancelled,
            calls, new DelayLog());
    }

    /** The name to show for this leg's train: its line name, or its own name where it has none. */
    public String displayName() {
        return line.nameOr(trainName);
    }

    /** The colour to show this leg in, taken from its line or category. */
    public DLColor displayColor() {
        return RailwayBackendApi.getServiceColor(line, category);
    }

    /** The call where travellers board this leg. */
    public RouteCall boarding() {
        return calls.get(0);
    }

    /** The call where travellers alight from this leg. */
    public RouteCall alighting() {
        return calls.get(calls.size() - 1);
    }

    /** The station travellers board at. */
    public StationRef from() {
        return boarding().station();
    }

    /** The station travellers alight at. */
    public StationRef to() {
        return alighting().station();
    }

    /** When the leg departs its boarding stop. */
    public long departure() {
        return boarding().realtime().departure();
    }

    /** When the leg reaches its alighting stop. */
    public long arrival() {
        return alighting().realtime().arrival();
    }

    /** How long the leg takes, in ticks. */
    public long duration() {
        return Math.max(0, arrival() - departure());
    }

    /** The calls between boarding and alighting, both ends excluded. */
    public List<RouteCall> intermediateCalls() {
        return calls.size() < 3 ? List.of() : calls.subList(1, calls.size() - 1);
    }

    /** How many stops lie between boarding and alighting. */
    public int intermediateStopCount() {
        return Math.max(0, calls.size() - 2);
    }

    /** Whether the leg runs under a named line. */
    public boolean hasLine() {
        return line.isKnown();
    }

    /** Whether the leg runs under a named category. */
    public boolean hasCategory() {
        return category.isKnown();
    }

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

    public static RouteLeg ofCurrentSection(TrainSnapshot train, JourneySnapshot journey) {
        return ofSection(train, journey, journey.currentSectionIndex(), null);
    }

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

    private static int runVisit(JourneySnapshot journey, int position, StopSnapshot stop) {
        return journey.currentStopIndex() >= 0 && position < journey.currentStopIndex()
            ? stop.completedVisits() - 1
            : stop.completedVisits();
    }

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

    private static RouteCall callOf(StopSnapshot stop, int cycle, long cycleDuration) {
        int cyclesAhead = cycle - stop.completedVisits();
        if (cyclesAhead >= 0) {
            StopSnapshot projected = stop.advancedBy(cyclesAhead, cycleDuration);
            return new RouteCall(projected.scheduledStation(), projected.station(),
                projected.entryIndex(), cycle, projected.scheduled(), projected.realtime());
        }

        long shift = cyclesAhead * Math.max(0, cycleDuration);
        StopTimes actual = cyclesAhead == -1 && stop.previousActual().isKnown()
            ? stop.previousActual()
            : stop.realtime().shifted(shift);

        RouteCall call = new RouteCall(stop.scheduledStation(), stop.station(), stop.entryIndex(),
            cycle, stop.scheduled().shifted(shift), actual);
        call.markPassed();
        return call;
    }

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

    public static RouteLeg fromNbt(CompoundTag nbt) {
        return new RouteLeg(
            NbtHelper.readNullableUUID(nbt, NBT_TRAIN_ID),
            NbtHelper.readNullableUUID(nbt, NBT_SESSION_ID),
            nbt.getString(NBT_TRAIN_NAME),
            nbt.contains(NBT_ICON_ID) ? new ResourceLocation(nbt.getString(NBT_ICON_ID)) : null,
            LineRef.fromNbt(nbt.getCompound(NBT_LINE)),
            TrainCategoryRef.fromNbt(nbt.getCompound(NBT_CATEGORY)),
            nbt.getString(NBT_DESTINATION_TEXT),
            nbt.getInt(NBT_SECTION_INDEX),
            nbt.getBoolean(NBT_CANCELLED),
            NbtHelper.readList(nbt, NBT_CALLS, RouteCall::fromNbt),
            DelayLog.fromNbt(nbt.getCompound(NBT_DELAYS))
        );
    }

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
