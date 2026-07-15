package de.mrjulsen.crn.backend.timing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import net.minecraft.nbt.CompoundTag;

/**
 * Learns a duration (e.g. the transit time between two stations) from repeated measurements.
 * <p>
 * The tracker exposes a stable <i>reference value</i> and only changes it when a deviation proves
 * to be permanent rather than a one-off:
 * <ul>
 *   <li>A measurement within {@code threshold} of the reference confirms the reference is still
 *       attainable and <b>discards any pending change</b> - a single delayed (longer) or lucky
 *       (shorter) run can therefore never redefine the leg on its own.</li>
 *   <li>A deviating measurement is only collected if it is consistent with the ones already
 *       collected; a value that disagrees with both the reference and the pending candidate
 *       restarts the streak, so scattered outliers do not accumulate.</li>
 *   <li>Only once {@code capacity} consecutive, mutually consistent deviating measurements have
 *       been seen (i.e. the new duration held for several rounds) is their median adopted as the
 *       new reference - the assumption then being that track conditions really changed.</li>
 * </ul>
 * This makes the learned value robust against single outliers (a train held at a signal once)
 * while still adapting to permanent changes (a track being rebuilt) after a few rounds.
 */
public final class MedianDurationTracker {

    private static final String NBT_REFERENCE = "Reference";
    private static final String NBT_HISTORY = "History";

    private final int capacity;
    private final int threshold;

    /** The pending streak of consistent deviating measurements that may become the new reference. */
    private final ConcurrentLinkedDeque<Integer> history = new ConcurrentLinkedDeque<>();
    private volatile int reference = -1;
    private volatile int lastMeasurement = -1;

    /** Called whenever the reference value changes (after initialization). */
    private Runnable onReferenceChanged;

    public MedianDurationTracker(int capacity, int threshold) {
        this.capacity = Math.max(1, capacity);
        this.threshold = Math.max(0, threshold);
    }

    public void setOnReferenceChanged(Runnable listener) {
        this.onReferenceChanged = listener;
    }

    /** Whether at least one value has been learned. */
    public boolean isInitialized() {
        return reference >= 0;
    }

    /** The current stable reference duration in ticks, or {@code -1} if nothing has been learned yet. */
    public int get() {
        return reference;
    }

    /** The most recent raw measurement in ticks, or {@code -1}. */
    public int lastMeasurement() {
        return lastMeasurement;
    }

    public List<Integer> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Seeds the tracker with an estimated value without treating it as a real measurement.
     * Only has an effect while the tracker is uninitialized.
     */
    public void seed(int estimatedDuration) {
        if (!isInitialized() && estimatedDuration >= 0) {
            this.reference = estimatedDuration;
        }
    }

    /** Records an actually measured duration. */
    public synchronized void record(int measuredDuration) {
        if (measuredDuration < 0) {
            return;
        }
        this.lastMeasurement = measuredDuration;

        if (reference < 0) {
            this.reference = measuredDuration;
            return;
        }

        // The reference time was (nearly) achieved again -> it is still valid, forget any pending
        // change. This is what prevents a one-off delayed/faster run from ever changing the rule.
        if (Math.abs(measuredDuration - reference) <= threshold) {
            history.clear();
            return;
        }

        // A deviating measurement only reinforces a new rule if it agrees with the streak so far;
        // otherwise the streak was not a consistent trend and is restarted from this measurement.
        if (!history.isEmpty() && Math.abs(measuredDuration - median()) > threshold) {
            history.clear();
        }
        history.addLast(measuredDuration);

        // The deviation held for enough consecutive rounds: treat it as a permanent change.
        if (history.size() >= capacity) {
            int newReference = median();
            if (newReference != reference) {
                this.reference = newReference;
                if (onReferenceChanged != null) {
                    // Fire before clearing so listeners (diagnostics) still see the streak evidence.
                    onReferenceChanged.run();
                }
            }
            history.clear();
        }
    }

    /** Overwrites the learned value, e.g. when loading persisted data. */
    public synchronized void force(int duration) {
        this.reference = duration;
        this.lastMeasurement = duration;
        history.clear();
        if (duration >= 0) {
            history.add(duration);
        }
    }

    public synchronized void reset() {
        this.reference = -1;
        this.lastMeasurement = -1;
        history.clear();
    }

    private int median() {
        Integer[] values = history.toArray(new Integer[0]);
        if (values.length == 0) {
            return reference;
        }
        Arrays.sort(values);
        int mid = values.length / 2;
        return values.length % 2 == 0 ? (values[mid - 1] + values[mid]) / 2 : values[mid];
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_REFERENCE, reference);
        nbt.putIntArray(NBT_HISTORY, history.stream().mapToInt(Integer::intValue).toArray());
        return nbt;
    }

    public void loadNbt(CompoundTag nbt) {
        synchronized (this) {
            this.reference = nbt.getInt(NBT_REFERENCE);
            history.clear();
            for (int value : nbt.getIntArray(NBT_HISTORY)) {
                if (history.size() >= capacity) break;
                history.addLast(value);
            }
        }
    }
}
