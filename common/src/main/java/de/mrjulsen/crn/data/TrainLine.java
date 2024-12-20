package de.mrjulsen.crn.data;

import net.minecraft.nbt.CompoundTag;

public class TrainLine {

    public static int MAX_NAME_LENGTH = 32;

    private static final String NBT_NAME = "Name";
    private static final String NBT_COLOR = "Color";

    private final String name;
    private int lineColor = 0;
    protected String lastEditorName;
    protected long lastEditedTime;

    public TrainLine(String name) {
        this.name = name;
    }

    public String getLineName() {
        return name;
    }

    public int getColor() {
        return lineColor;
    }

    public void setColor(int color) {
        this.lineColor = color;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_NAME, name);
        nbt.putInt(NBT_COLOR, lineColor);
        return nbt;
    }

    public static TrainLine fromNbt(CompoundTag nbt) {
        TrainLine line = new TrainLine(
            nbt.getString(NBT_NAME)
        );
        line.setColor(nbt.getInt(NBT_COLOR));
        return line;
    }
}
