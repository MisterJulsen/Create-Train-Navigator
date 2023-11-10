package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import de.mrjulsen.crn.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class TrainStationAlias {
    
    private static final String NBT_ALIAS_NAME = "AliasName";
    private static final String NBT_STATION_LIST = "Stations";
    private static final String NBT_LAST_EDITOR = "LastEditor";
    private static final String NBT_LAST_EDITED_TIME = "LastEditedTimestamp";

    private AliasName aliasName;
    private Collection<String> stations = new ArrayList<>();
    // log
    private String lastEditorName = null;
    private long lastEditedTime = 0;

    protected TrainStationAlias(AliasName aliasName, Collection<String> initialValues, String lastEditorName, long lastEditedTime) {
        this(aliasName, initialValues);
        this.lastEditorName = lastEditorName;
        this.lastEditedTime = lastEditedTime;
    }

    public TrainStationAlias(AliasName aliasName, Collection<String> initialValues) {
        this(aliasName);
        stations.addAll(initialValues);
    }

    public TrainStationAlias(AliasName aliasName) {
        this.aliasName = aliasName;
    }


    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (aliasName == null) {
            return nbt;
        }

        nbt.put(NBT_ALIAS_NAME, getAliasName().toNbt());
        if (lastEditorName != null) {
            nbt.putString(NBT_LAST_EDITOR, getLastEditorName());
        }
        nbt.putLong(NBT_LAST_EDITED_TIME, lastEditedTime);

        ListTag stationsList = new ListTag();
        stations.forEach(x -> stationsList.add(StringTag.valueOf(x)));
        nbt.put(NBT_STATION_LIST, stationsList);

        return nbt;
    }

    public static TrainStationAlias fromNbt(CompoundTag nbt) {
        if (!nbt.contains(NBT_ALIAS_NAME)) {
            return new TrainStationAlias(AliasName.of("null"));
        }
        AliasName name = AliasName.fromNbt(nbt.getCompound(NBT_ALIAS_NAME));
        String lastEditorName = nbt.contains(NBT_LAST_EDITOR) ? nbt.getString(NBT_LAST_EDITOR) : null;
        long lastEditedTime = nbt.getLong(NBT_LAST_EDITED_TIME);
        Collection<String> stations = nbt.getList(NBT_STATION_LIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList();
        return new TrainStationAlias(name, stations, lastEditorName, lastEditedTime);
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
        return Constants.DATE_FORMAT.format(getLastEditedTime());
    }


    public AliasName getAliasName() {
        return this.aliasName;
    }

    public void add(String station) {
        if (!stations.contains(station)) {
            stations.add(station);
        }
    }

    public void addAll(Collection<String> stations) {
        stations.forEach(x -> {
            if (!this.stations.contains(x)) {
                this.stations.add(x);
            }
        });
    }

    public boolean contains(String station) {
        String regex = station.isBlank() ? station : "\\Q" + station.replace("*", "\\E.*\\Q");
        return stations.stream().anyMatch(x -> x.matches(regex));
    }

    public Collection<String> getAllStationNames() {
        return stations;
    }

    public void setName(AliasName name) {
        this.aliasName = name;
    }

    public void remove(String station) {
        stations.removeIf(x -> x.equals(station));
    }

    @Override
    public String toString() {
        return String.format("%s", getAliasName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrainStationAlias alias) {
            return getAliasName().equals(alias.getAliasName()) && getAllStationNames().size() == alias.getAllStationNames().size() && getAllStationNames().stream().allMatch(x -> alias.contains(x));
        }
        return false;
    }

    public void update(TrainStationAlias newData) {
        this.aliasName = newData.aliasName;
        this.stations = newData.stations;
        this.lastEditedTime = newData.lastEditedTime;
        this.lastEditorName = newData.lastEditorName;
    }

}
