package de.mrjulsen.crn.api.core.snapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.delay.DelaySeverity;
import de.mrjulsen.crn.core.realtime.RealtimeTracker;
import de.mrjulsen.crn.core.timing.StopTimings;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;
import net.minecraft.nbt.CompoundTag;

/**
 * Why a train is late or out of service, and by how much. Combines the measured deviation with the
 * observations behind it and the reasons the backend has attributed.
 * <p>
 * All times are in ticks. The measured counters describe the current stop or leg only, not the run
 * as a whole.
 *
 * @param trainId                    The train this describes.
 * @param trainName                  The train's own name.
 * @param delayed                    Whether the train counts as late by the configured threshold.
 * @param cancelled                  Whether the train is out of service.
 * @param maxDeviation               How far behind its timetable the train is, before the
 *                                   allowance is applied. Negative when it is early.
 * @param delayOffset                The part of the deviation deliberately not counted as delay.
 * @param nextStopArrivalDeviation   How much later than scheduled the train is expected at its
 *                                   current stop.
 * @param nextStopDepartureDeviation The same for its departure.
 * @param signalWaitTicks            How long the train has been held at signals.
 * @param stalledTicks               How long the train has been unable to move.
 * @param dwellTicks                 How long the train has been standing at its current stop.
 * @param separationHoldTicks        How much longer the train is being held at its current stop to
 *                                   keep the configured distance to the train before it, or zero
 *                                   when nothing is holding it. This is a hard constraint: the train
 *                                   will not leave earlier, however late it is running.
 * @param blockingTrains             The names of the trains observed to be in the way.
 * @param causes                     The attributed reasons, most important first.
 */
public record DelayReport(
    @ResponseAlwaysInclude UUID trainId,
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
    long separationHoldTicks,
    List<String> blockingTrains,
    List<DelayInstance> causes
) {

    public DelayReport {
        blockingTrains = blockingTrains == null ? List.of() : List.copyOf(blockingTrains);
        causes = causes == null ? List.of() : List.copyOf(causes);
    }

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
            train.getSeparationHoldTicksRemaining(),
            List.copyOf(realtime.getBlockingTrainNames()),
            train.getActiveDelays()
        );
    }

    /**
     * How late the train is, in ticks, after the allowance is applied. Never negative: a train
     * running early reports no delay.
     */
    public long currentDelay() {
        return Math.max(0, maxDeviation - delayOffset);
    }

    public boolean hasCauses() {
        return !causes.isEmpty();
    }

    /** The most important reason, where one is known. */
    public Optional<DelayInstance> primaryCause() {
        return causes.isEmpty() ? Optional.empty() : Optional.of(causes.get(0));
    }

    /**
     * Reasons worth reporting whether or not they cost time, such as a train being out of service.
     */
    public List<DelayInstance> operationalCauses() {
        return causes.stream().filter(x -> x.severity() == DelaySeverity.IMPORTANT).toList();
    }

    /** Reasons that account for lost time. */
    public List<DelayInstance> delayCauses() {
        return causes.stream().filter(x -> x.severity() == DelaySeverity.DELAY).toList();
    }

    /**
     * The delay the reasons together account for, in ticks. Reasons without an estimate of their
     * own contribute nothing, so this can fall short of {@link #currentDelay()} and is not a
     * substitute for it.
     */
    public long attributedDelay() {
        return causes.stream().filter(DelayInstance::hasEstimatedDelay).mapToLong(DelayInstance::estimatedDelayTicks).sum();
    }

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
        nbt.putLong(NBT_SEPARATION_HOLD, separationHoldTicks);
        nbt.put(NBT_BLOCKING_TRAINS, NbtHelper.writeStrings(blockingTrains));
        nbt.put(NBT_CAUSES, NbtHelper.writeList(causes, DelayInstance::toNbt));
        return nbt;
    }

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
            nbt.getLong(NBT_SEPARATION_HOLD),
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
    private static final String NBT_SEPARATION_HOLD = "SeparationHoldTicks";
    private static final String NBT_BLOCKING_TRAINS = "BlockingTrains";
    private static final String NBT_CAUSES = "Causes";
}
