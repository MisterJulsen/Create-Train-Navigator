package de.mrjulsen.crn.data;

import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.mrjulsen.crn.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class TrainStationAlias {
    
    private static final String NBT_ALIAS_NAME = "AliasName";
    private static final String NBT_STATION_LIST = "Stations";
    private static final String NBT_STATION_MAP = "StationData";
    private static final String NBT_LAST_EDITOR = "LastEditor";
    private static final String NBT_LAST_EDITED_TIME = "LastEditedTimestamp";
   
    private static final String NBT_STATION_ENTRY_NAME = "Name";

    protected AliasName aliasName;
    protected Map<String, StationInfo> stations = new HashMap<>();
    // log
    protected String lastEditorName = null;
    protected long lastEditedTime = 0;

    protected TrainStationAlias(AliasName aliasName, Map<String, StationInfo> initialValues, String lastEditorName, long lastEditedTime) {
        this(aliasName, initialValues);
        this.lastEditorName = lastEditorName;
        this.lastEditedTime = lastEditedTime;
    }

    public TrainStationAlias(AliasName aliasName, Map<String, StationInfo> initialValues) {
        this(aliasName);
        stations.putAll(initialValues);
    }

    public TrainStationAlias(AliasName aliasName) {
        this.aliasName = aliasName;
    }

    public TrainStationAlias copy() {
        return new TrainStationAlias(new AliasName(getAliasName().get()), new HashMap<>(getAllStations()));
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
        stations.forEach((key, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString(NBT_STATION_ENTRY_NAME, key);
            value.writeNbt(entry);
            stationsList.add(entry);
        });
        nbt.put(NBT_STATION_MAP, stationsList);

        return nbt;
    }

    public static TrainStationAlias fromNbt(CompoundTag nbt) {
        if (!nbt.contains(NBT_ALIAS_NAME)) {
            return new TrainStationAlias(AliasName.of("null"));
        }
        AliasName name = AliasName.fromNbt(nbt.getCompound(NBT_ALIAS_NAME));
        String lastEditorName = nbt.contains(NBT_LAST_EDITOR) ? nbt.getString(NBT_LAST_EDITOR) : null;
        long lastEditedTime = nbt.getLong(NBT_LAST_EDITED_TIME);
        Map<String, StationInfo> stations;
        if (nbt.contains(NBT_STATION_LIST)) {
            stations = nbt.getList(NBT_STATION_LIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).collect(Collectors.toMap(x -> x, x -> StationInfo.empty()));        
        } else if (nbt.contains(NBT_STATION_MAP)) {
            stations = nbt.getList(NBT_STATION_MAP, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).collect(Collectors.toMap(x -> {
                return x.getString(NBT_STATION_ENTRY_NAME);
            }, x -> {
                return StationInfo.fromNbt(x);
            }));
        } else {
            stations = new IdentityHashMap<>();
        }

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

    public void add(String station, StationInfo info) {
        if (!stations.containsKey(station)) {
            stations.put(station, info);
        }
    }

    public void addAll(Map<String, StationInfo> stations) {
        stations.forEach((key, value) -> {
            if (!this.stations.containsKey(key)) {
                this.stations.put(key, value);
            }
        });
    }

    public boolean contains(String station) {
        String regex = station.isBlank() ? station : "\\Q" + station.replace("*", "\\E.*\\Q");
        return stations.keySet().stream().anyMatch(x -> x.matches(regex));
    }

    public Set<String> getAllStationNames() {
        return stations.keySet();
    }

    public Map<String, StationInfo> getAllStations() {
        return stations;
    }

    public StationInfo getInfoForStation(String stationName) {
        return stations.containsKey(stationName) ? stations.get(stationName) : StationInfo.empty();
    }

    public void setName(AliasName name) {
        this.aliasName = name;
    }

    public void remove(String station) {
        stations.remove(station);
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

    @Override
    public int hashCode() {
        return 7 * Objects.hash(aliasName);
    }

    public void update(TrainStationAlias newData) {
        this.aliasName = newData.aliasName;
        this.stations = newData.stations;
        this.lastEditedTime = newData.lastEditedTime;
        this.lastEditorName = newData.lastEditorName;
    }

    public static record StationInfo(String platform) {
        private static final String NBT_PLATFORM = "Platform";

        public static StationInfo empty() {
            return new StationInfo("");
        }

        public void writeNbt(CompoundTag nbt) {
            nbt.putString(NBT_PLATFORM, platform());
        }

        public static StationInfo fromNbt(CompoundTag nbt) {
            return new StationInfo(
                nbt.getString(NBT_PLATFORM)
            );
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj instanceof StationInfo other) {
                return platform().equals(other.platform());
            }
            return false;
        }
    }

}
