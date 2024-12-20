package de.mrjulsen.crn.data;

import java.util.Date;
import java.util.Objects;
import de.mrjulsen.mcdragonlib.DragonLib;
import net.minecraft.nbt.CompoundTag;

public class TrainGroup {

    public static int MAX_NAME_LENGTH = 32;

    private static final String NBT_NAME = "Name";
    private static final String NBT_COLOR = "Color";
    private static final String NBT_LAST_EDITOR = "LastEditor";
    private static final String NBT_LAST_EDITED_TIME = "LastEditedTimestamp";

    private final String name;
    private int color;
    
    protected String lastEditorName;
    protected long lastEditedTime;

    public TrainGroup(String name) {
        this.name = name;
        updateLastEdited("Server");
    }

    public String getGroupName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getLastEditorName() {
        return lastEditorName;
    }

    public void updateLastEdited(String name) {
        this.lastEditorName = name;
        this.lastEditedTime = new Date().getTime();
    }

    public Date getLastEditedTime() {
        return new Date(lastEditedTime);
    }

    public String getLastEditedTimeFormatted() {
        return DragonLib.DATE_FORMAT.format(getLastEditedTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrainGroup o) {
            return name.equals(o.name);
        }
        return false;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        
        if (lastEditorName != null) {
            nbt.putString(NBT_LAST_EDITOR, getLastEditorName());
        }
        if (lastEditedTime > 0) {
            nbt.putLong(NBT_LAST_EDITED_TIME, lastEditedTime);
        }
        nbt.putString(NBT_NAME, getGroupName());
        nbt.putInt(NBT_COLOR, getColor());

        return nbt;
    }

    public static TrainGroup fromNbt(CompoundTag nbt) {
        String groupName = nbt.getString(NBT_NAME);
        TrainGroup group = new TrainGroup(groupName);
        group.setColor(nbt.getInt(NBT_COLOR));
        if (nbt.contains(NBT_LAST_EDITOR)) {
            group.lastEditorName = nbt.getString(NBT_LAST_EDITOR);
        }
        if (nbt.contains(NBT_LAST_EDITED_TIME)) {
            group.lastEditedTime = nbt.getLong(NBT_LAST_EDITED_TIME);
        }
        return group;
    }
}
