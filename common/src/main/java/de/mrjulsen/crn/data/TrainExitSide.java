package de.mrjulsen.crn.data;

import java.util.Arrays;

public enum TrainExitSide {
    UNKNOWN((byte)0),
    RIGHT((byte)1),
    LEFT((byte)-1);

    private byte side;

    TrainExitSide(byte side) {
        this.side = side;
    }

    public byte getAsByte() {
        return side;
    }

    public static TrainExitSide getFromByte(byte side) {
        return Arrays.stream(values()).filter(x -> x.getAsByte() == side).findFirst().orElse(UNKNOWN);
    }

    public TrainExitSide getOpposite() {
        return getFromByte((byte)-getAsByte());
    }
}
