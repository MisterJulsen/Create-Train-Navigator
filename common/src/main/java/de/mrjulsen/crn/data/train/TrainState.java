package de.mrjulsen.crn.data.train;

import java.util.Arrays;

/** The status of the train measured at the current station. */
public enum TrainState {
    /** The train will arrive at this station in the future. */
    BEFORE((byte)-2, '-'),
    /** The next stop was announced. */
    ANNOUNCED((byte)-1, '.'),
    /** The train is waiting at this station. */
    STAYING((byte)0, '~'),
    /** The train has already departed from this station. */
    AFTER((byte)1, '+');

    private byte position;
    private char indicator;

    private TrainState(byte position, char indicator) {
        this.position = position;
        this.indicator = indicator;
    }

    public byte getPositionMultiplier() {
        return position;
    }

    public char getIndicator() {
        return indicator;
    }

    public static TrainState getByPositionInt(int position) {
        return Arrays.stream(values()).filter(x -> x.getPositionMultiplier() == position).findFirst().orElse(STAYING);
    }
}
