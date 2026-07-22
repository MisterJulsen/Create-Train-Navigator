package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;
import de.mrjulsen.crn.backend.realtime.RealtimeTracker;
import de.mrjulsen.crn.backend.timing.StopTimings;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * An immutable report on why a train is late, and by how much.
 * <p>
 * The deviations are what the backend measured; the causes explain them. A cause may state how much
 * of the delay it accounts for, but those figures are informational - they are supplied by whoever
 * detected the cause and need not add up to {@link #maxDeviation()}.
 *
 * @param trainId          The id of the train.
 * @param trainName        The train's own name.
 * @param delayed          Whether the train is late beyond the configured threshold.
 * @param cancelled        Whether the train is out of service because of a disruption.
 * @param maxDeviation     The highest deviation from the timetable across all stops, in ticks.
 * @param delayOffset      Deviation carried over from a previous section, in ticks.
 * @param nextStopArrivalDeviation   Deviation of the arrival at the current stop, in ticks.
 * @param nextStopDepartureDeviation Deviation of the departure from the current stop, in ticks.
 * @param signalWaitTicks  How long the train has been held at signals during the current leg.
 * @param stalledTicks     How long a carriage has been stalled during the current leg.
 * @param dwellTicks       How long the train has been standing at the current station.
 * @param blockingTrains   The names of trains that held this one up during the current leg.
 * @param causes           Every status reason currently applying, most important first.
 */
public record DelayReport(
    UUID trainId,
    String trainName,
    boolean delayed,
    boolean cancelled,
    long maxDeviation,
    long delayOffset,
    long nextStopArrivalDeviation,
    long nextStopDepartureDeviation,
    int signalWaitTicks,
    int stalledTicks,
    int dwellTicks,
    List<String> blockingTrains,
    List<DelayInstance> causes
) {

    public DelayReport {
        blockingTrains = blockingTrains == null ? List.of() : List.copyOf(blockingTrains);
        causes = causes == null ? List.of() : List.copyOf(causes);
    }

    /** Captures the current delay situation of the given train. */
    public static DelayReport of(TrackedTrain train) {
        StopTimings current = train.getCurrentStop().map(train::getTimings).orElse(null);
        RealtimeTracker realtime = train.getRealtime();

        return new DelayReport(
            train.getTrainId(),
            train.getTrainName(),
            train.isDelayed(),
            train.isCancelled(),
            train.getMaxDeviation(),
            train.getDelayOffset(),
            current == null ? 0 : current.getArrivalDeviation(),
            current == null ? 0 : current.getDepartureDeviation(),
            realtime.getTotalSignalWaitTicks(),
            realtime.getStalledTicks(),
            realtime.getDwellTicks(),
            List.copyOf(realtime.getBlockingTrainNames()),
            train.getActiveDelays()
        );
    }

    /** The delay in ticks, i.e. the deviation not carried over from a previous section. */
    public long currentDelay() {
        return Math.max(0, maxDeviation - delayOffset);
    }

    /** Whether any cause is currently known. */
    public boolean hasCauses() {
        return !causes.isEmpty();
    }

    /** The most important reason currently applying, if there is any. */
    public Optional<DelayInstance> primaryCause() {
        return causes.isEmpty() ? Optional.empty() : Optional.of(causes.get(0));
    }

    /** The reasons that take the train out of service, as opposed to merely delaying it. */
    public List<DelayInstance> operationalCauses() {
        return causes.stream().filter(x -> x.severity() == DelaySeverity.IMPORTANT).toList();
    }

    /** The reasons that explain a delay without taking the train out of service. */
    public List<DelayInstance> delayCauses() {
        return causes.stream().filter(x -> x.severity() == DelaySeverity.DELAY).toList();
    }

    /**
     * The sum of the delay contributions the causes were able to quantify, in ticks. Causes that
     * could not quantify their share are left out, so this is a lower bound rather than a total.
     */
    public long attributedDelay() {
        return causes.stream().filter(DelayInstance::hasEstimatedDelay).mapToLong(DelayInstance::estimatedDelayTicks).sum();
    }

    /** Serializes this report. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        NbtHelper.putNullableUUID(nbt, NBT_TRAIN_ID, trainId);
        nbt.putString(NBT_TRAIN_NAME, trainName == null ? "" : trainName);
        nbt.putBoolean(NBT_DELAYED, delayed);
        nbt.putBoolean(NBT_CANCELLED, cancelled);
        nbt.putLong(NBT_MAX_DEVIATION, maxDeviation);
        nbt.putLong(NBT_DELAY_OFFSET, delayOffset);
        nbt.putLong(NBT_NEXT_ARRIVAL_DEVIATION, nextStopArrivalDeviation);
        nbt.putLong(NBT_NEXT_DEPARTURE_DEVIATION, nextStopDepartureDeviation);
        nbt.putInt(NBT_SIGNAL_WAIT, signalWaitTicks);
        nbt.putInt(NBT_STALLED, stalledTicks);
        nbt.putInt(NBT_DWELL, dwellTicks);
        nbt.put(NBT_BLOCKING_TRAINS, NbtHelper.writeStrings(blockingTrains));
        nbt.put(NBT_CAUSES, NbtHelper.writeList(causes, DelayInstance::toNbt));
        return nbt;
    }

    /** Deserializes a report written by {@link #toNbt()}. */
    public static DelayReport fromNbt(CompoundTag nbt) {
        return new DelayReport(
            NbtHelper.readNullableUUID(nbt, NBT_TRAIN_ID),
            nbt.getString(NBT_TRAIN_NAME),
            nbt.getBoolean(NBT_DELAYED),
            nbt.getBoolean(NBT_CANCELLED),
            nbt.getLong(NBT_MAX_DEVIATION),
            nbt.getLong(NBT_DELAY_OFFSET),
            nbt.getLong(NBT_NEXT_ARRIVAL_DEVIATION),
            nbt.getLong(NBT_NEXT_DEPARTURE_DEVIATION),
            nbt.getInt(NBT_SIGNAL_WAIT),
            nbt.getInt(NBT_STALLED),
            nbt.getInt(NBT_DWELL),
            NbtHelper.readStrings(nbt, NBT_BLOCKING_TRAINS),
            NbtHelper.readList(nbt, NBT_CAUSES, DelayInstance::fromNbt)
        );
    }

    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_TRAIN_NAME = "TrainName";
    private static final String NBT_DELAYED = "Delayed";
    private static final String NBT_CANCELLED = "Cancelled";
    private static final String NBT_MAX_DEVIATION = "MaxDeviation";
    private static final String NBT_DELAY_OFFSET = "DelayOffset";
    private static final String NBT_NEXT_ARRIVAL_DEVIATION = "NextStopArrivalDeviation";
    private static final String NBT_NEXT_DEPARTURE_DEVIATION = "NextStopDepartureDeviation";
    private static final String NBT_SIGNAL_WAIT = "SignalWaitTicks";
    private static final String NBT_STALLED = "StalledTicks";
    private static final String NBT_DWELL = "DwellTicks";
    private static final String NBT_BLOCKING_TRAINS = "BlockingTrains";
    private static final String NBT_CAUSES = "Causes";
}
