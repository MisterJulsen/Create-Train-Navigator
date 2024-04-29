package de.mrjulsen.crn.util;

import java.util.Arrays;

import net.minecraft.util.StringRepresentable;

public enum ESpeedUnit implements StringRepresentable {
    MS(0, "m/s", 1.0D),
    KMH(1, "km/h", 3.6D),
    MPH(3, "mph", 2.237136D),
    FTS(4, "ft/s", 3.28084D),
    KT(5, "kt", 1.944012D),
    CMS(6, "cm/s", 100.0D);

    private int index;
    private String unit;
    private double fac;

    private ESpeedUnit(int index, String unit, double fac) {
        this.index = index;
        this.unit = unit;
        this.fac = fac;
    }

    public int getIndex() {
        return index;
    }

    public String getUnit() {
        return unit;
    }

    public double getFactor() {
        return fac;
    }

    public static ESpeedUnit getByIndex(int index) {
        return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(MS);
    }

    @Override
    public String getSerializedName() {
        return unit;
    }
    
}
