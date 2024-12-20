package de.mrjulsen.crn.debug;

import java.util.Arrays;

import de.mrjulsen.crn.Constants;
import net.minecraft.util.StringRepresentable;

public enum TrainDebugState implements StringRepresentable {
    PREPARING((byte)-1, "Preparing", Constants.COLOR_DELAYED),
    INITIALIZING((byte)1, "Initializing", Constants.COLOR_DELAYED),
    READY((byte)0, "Ready", Constants.COLOR_ON_TIME);

    byte id;
    String name;
    int color;

    TrainDebugState(byte id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public byte getId() {
        return id;
    }

    public int getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public static TrainDebugState getStateById(int id) {
        return Arrays.stream(values()).filter(x -> x.getId() == id).findFirst().orElse(INITIALIZING);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
    
}
