package de.mrjulsen.crn.block.properties;

import java.util.Arrays;

import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import net.minecraft.util.StringRepresentable;

public enum ETimeDisplay implements StringRepresentable, ITranslatableEnum {
    ABS((byte)0, "abs"),
    ETA((byte)1, "eta");

    private byte id;
    private String name;

    private ETimeDisplay(byte id, String name) {
        this.id = id;
        this.name = name;
    }

    public byte getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static ETimeDisplay getById(int id) {
        return Arrays.stream(values()).filter(x -> x.getId() == id).findFirst().orElse(ABS);
    }

    @Override
    public String getEnumName() {
        return "time_display";
    }

    @Override
    public String getEnumValueName() {
        return getName();
    }

    @Override
    public String getSerializedName() {
        return getName();
    }

}