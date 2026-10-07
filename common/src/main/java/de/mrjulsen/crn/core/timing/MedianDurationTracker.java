package de.mrjulsen.crn.core.timing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import net.minecraft.nbt.CompoundTag;

public final class MedianDurationTracker {

    private static final String NBT_REFERENCE = "Reference";
    private static final String NBT_HISTORY = "History";
    private static final String NBT_SEEDED = "Seeded";

    private final int capacity;
    private final int threshold;

    private final ConcurrentLinkedDeque<Integer> history = new ConcurrentLinkedDeque<>();
    private volatile int reference = -1;
    private volatile int lastMeasurement = -1;
    private volatile boolean seeded = false;

    private Runnable onReferenceChanged;

    public MedianDurationTracker(int capacity, int threshold) {
        this.capacity = Math.max(1, capacity);
        this.threshold = Math.max(0, threshold);
    }

    public void setOnReferenceChanged(Runnable listener) {
        this.onReferenceChanged = listener;
    }

    public boolean isInitialized() {
        return reference >= 0;
    }

    public int get() {
        return reference;
    }

    public int lastMeasurement() {
        return lastMeasurement;
    }

    public List<Integer> getHistory() {
        return new ArrayList<>(history);
    }

    public void seed(int estimatedDuration) {
        if (!isInitialized() && estimatedDuration >= 0) {
            this.reference = estimatedDuration;
            this.seeded = true;
        }
    }

    public void reseed(int estimatedDuration) {
        if (seeded && estimatedDuration >= 0) {
            this.reference = estimatedDuration;
        }
    }

    public synchronized void record(int measuredDuration) {
        if (measuredDuration < 0) {
            return;
        }
        this.lastMeasurement = measuredDuration;

        if (reference < 0) {
            this.reference = measuredDuration;
            return;
        }

        if (seeded) {
            this.seeded = false;
            history.clear();
            if (measuredDuration != reference) {
                this.reference = measuredDuration;
                if (onReferenceChanged != null) {
                    onReferenceChanged.run();
                }
            }
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

    public synchronized void force(int duration) {
        this.reference = duration;
        this.lastMeasurement = duration;
        this.seeded = false;
        history.clear();
        if (duration >= 0) {
            history.add(duration);
        }
    }

    public synchronized void reset() {
        this.reference = -1;
        this.lastMeasurement = -1;
        this.seeded = false;
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
        nbt.putBoolean(NBT_SEEDED, seeded);
        return nbt;
    }

    public synchronized void loadNbt(CompoundTag nbt) {
        this.reference = nbt.contains(NBT_REFERENCE) ? nbt.getInt(NBT_REFERENCE) : -1;
        this.seeded = nbt.getBoolean(NBT_SEEDED);
        history.clear();
        for (int value : nbt.getIntArray(NBT_HISTORY)) {
            if (history.size() >= capacity) break;
            history.addLast(value);
        }
    }
}
