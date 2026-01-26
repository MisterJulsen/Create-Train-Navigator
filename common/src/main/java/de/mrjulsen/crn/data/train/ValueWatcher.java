package de.mrjulsen.crn.data.train;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;

public class ValueWatcher {

    private final int threshold;
    private final int bufferSize;
    private final Runnable updateValue;
    
    private int value;
    private int measuredValue = -1;
    private final Queue<Integer> valueHistory = new ConcurrentLinkedQueue<>();


    public ValueWatcher(int threshold, int bufferSize, Runnable updateValue) {
        this.threshold = threshold;
        this.bufferSize = bufferSize;
        this.updateValue = updateValue;
    }

    public void fillHistory(int value) {
        valueHistory.clear();
        for (int i = 0; i < bufferSize; i++) {
            valueHistory.add(value);
        }
    }

    public void forceValue(int value) {
        fillHistory(value);
        this.measuredValue = value;
        this.value = value;
    }

    /**
     * Adds a new transit time to the history and re-calculates the total duration, if necessary.
     * @param value The transit time in ticks
     * @param initializeOnly Whether the transit time should only be processed when the transit times have not been initialized yet.
     */
    public void add(int value, boolean initializeOnly) {
        // If no transit time was known, initialize first
        if (this.value < 0) {
            forceValue(value); // Set initial reference transit time
            DLUtils.doIfNotNull(updateValue, Runnable::run);
        }

        if (initializeOnly)
            return;

        this.measuredValue = value;

        // remove elements 
        while (valueHistory.size() >= bufferSize) {
            valueHistory.poll();
        }
        valueHistory.add(value); // add current transit time to the history

        final int refCurrentTransitTime = this.value;
        double median = ModUtils.calculateMedian(valueHistory, threshold, x -> true);

        if (Math.abs(refCurrentTransitTime - median) > threshold) { // Deviation is too large -> change transit time for this section
            int newValue = ModUtils.calculateMedian(valueHistory, threshold, x -> Math.abs(refCurrentTransitTime - x) > threshold);
            forceValue(newValue); // apply new transit time
            DLUtils.doIfNotNull(updateValue, Runnable::run); // re-calculate the total duration
        } else if (Math.abs(refCurrentTransitTime - value) < threshold) { // new value is smaller than current -> reset history (no changes needed)
            fillHistory(refCurrentTransitTime);
        }
    }

    public void tick() {
        this.measuredValue++;
    }




    public int treshold() {
        return threshold;
    }

    public int bufferSize() {
        return bufferSize;
    }

    public int value() {
        return value;
    }

    public int measuredValue() {
        return measuredValue;
    }

    public Integer[] history() {
        return valueHistory.toArray(new Integer[0]);
    }

    public boolean isInitialized() {
        return history().length > 0 && value() >= 0 && measuredValue() >= 0;
    }
}
