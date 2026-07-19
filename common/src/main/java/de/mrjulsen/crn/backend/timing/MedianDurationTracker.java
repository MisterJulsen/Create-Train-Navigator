package de.mrjulsen.crn.backend.timing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import net.minecraft.nbt.CompoundTag;

/**
 * Learns a duration from repeated measurements, exposing a stable reference value that only changes
 * once a deviation proves permanent rather than a one-off:
 * <ul>
 *   <li>A measurement within the threshold of the reference confirms it is still attainable and
 *       discards any pending change, so a single slow or fast run cannot redefine the value.</li>
 *   <li>A deviating measurement is only collected while it agrees with the ones already collected;
 *       one that disagrees restarts the streak, so scattered outliers do not accumulate.</li>
 *   <li>Only after a full streak of consistent deviating measurements is their median adopted.</li>
 * </ul>
 */
public final class MedianDurationTracker {

    private static final String NBT_REFERENCE = "Reference";
    private static final String NBT_HISTORY = "History";

    private final int capacity;
    private final int threshold;

    /** The pending streak of consistent deviating measurements. */
    private final ConcurrentLinkedDeque<Integer> history = new ConcurrentLinkedDeque<>();
    private volatile int reference = -1;
    private volatile int lastMeasurement = -1;

    /** Notified whenever the reference value changes after initialization. */
    private Runnable onReferenceChanged;

    public MedianDurationTracker(int capacity, int threshold) {
        this.capacity = Math.max(1, capacity);
        this.threshold = Math.max(0, threshold);
    }

    /** Sets the listener notified when the reference value changes. */
    public void setOnReferenceChanged(Runnable listener) {
        this.onReferenceChanged = listener;
    }

    /** Whether a value has been learned or seeded. */
    public boolean isInitialized() {
        return reference >= 0;
    }

    /** The stable reference duration in ticks, or {@code -1} if nothing is known yet. */
    public int get() {
        return reference;
    }

    /** The most recent raw measurement in ticks, or {@code -1}. */
    public int lastMeasurement() {
        return lastMeasurement;
    }

    /** The measurements of the pending streak, oldest first. */
    public List<Integer> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Seeds an estimated value without treating it as a measurement. Only has an effect while the
     * tracker is uninitialized.
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

        if (Math.abs(measuredDuration - reference) <= threshold) {
            history.clear();
            return;
        }

        if (!history.isEmpty() && Math.abs(measuredDuration - median()) > threshold) {
            history.clear();
        }
        history.addLast(measuredDuration);

        if (history.size() >= capacity) {
            int newReference = median();
            if (newReference != reference) {
                this.reference = newReference;
                if (onReferenceChanged != null) {
                    onReferenceChanged.run();
                }
            }
            history.clear();
        }
    }

    /** Overwrites the learned value and drops the pending streak. */
    public synchronized void force(int duration) {
        this.reference = duration;
        this.lastMeasurement = duration;
        history.clear();
        if (duration >= 0) {
            history.add(duration);
        }
    }

    /** Discards the learned value and the pending streak. */
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

    /** Serializes the reference value and the pending streak. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_REFERENCE, reference);
        nbt.putIntArray(NBT_HISTORY, history.stream().mapToInt(Integer::intValue).toArray());
        return nbt;
    }

    /** Restores persisted data. */
    public synchronized void loadNbt(CompoundTag nbt) {
        this.reference = nbt.contains(NBT_REFERENCE) ? nbt.getInt(NBT_REFERENCE) : -1;
        history.clear();
        for (int value : nbt.getIntArray(NBT_HISTORY)) {
            if (history.size() >= capacity) break;
            history.addLast(value);
        }
    }
}
