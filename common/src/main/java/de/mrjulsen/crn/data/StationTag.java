package de.mrjulsen.crn.data;

import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class StationTag {

    public static record ClientStationTag(String tagName, String stationName, StationInfo info, UUID tagId) {
        public static final String NBT_TAG_NAME = "TagName";
        public static final String NBT_STATION_NAME = "StationName";
        public static final String NBT_STATION_INFO = "StationInfo";
        public static final String NBT_TAG_ID = "Id";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_TAG_NAME, tagName());
            nbt.putString(NBT_STATION_NAME, stationName());
            nbt.put(NBT_STATION_INFO, info().toNbt());
            nbt.putUUID(NBT_TAG_ID, tagId() == null ? new UUID(0, 0) : tagId());
            return nbt;
        }

        public static ClientStationTag fromNbt(CompoundTag nbt) {
            return new ClientStationTag(
                nbt.getString(NBT_TAG_NAME),
                nbt.getString(NBT_STATION_NAME),
                StationInfo.fromNbt(nbt.getCompound(NBT_STATION_INFO)),
                nbt.getUUID(NBT_TAG_ID)
            );
        }
    }
    
    public static final int MAX_NAME_LENGTH = 32;

    private static final String LEGACY_NBT_TAG_NAME = "AliasName";

    private static final String NBT_ID = "Id";
    private static final String NBT_TAG_NAME = "TagName";
    private static final String NBT_STATION_LIST = "Stations";
    private static final String NBT_STATION_MAP = "StationData";
    private static final String NBT_LAST_EDITOR = "LastEditor";
    private static final String NBT_LAST_EDITED_TIME = "LastEditedTimestamp";
   
    private static final String NBT_STATION_ENTRY_NAME = "Name";

    protected UUID id;
    protected TagName tagName;
    protected Map<String /*Station Name*/, StationInfo> stations = new HashMap<>();

    // History
    protected String lastEditorName = null;
    protected long lastEditedTime = 0;

    protected StationTag(UUID id, TagName tagName, Map<String, StationInfo> initialValues, String lastEditorName, long lastEditedTime) {
        this(id, tagName, initialValues);
        this.lastEditorName = lastEditorName;
        this.lastEditedTime = lastEditedTime;
    }

    public StationTag(UUID id, TagName tagName, Map<String, StationInfo> initialValues) {
        this(id, tagName);
        stations.putAll(initialValues);
    }

    public StationTag(UUID id, TagName tagName) {
        this.id = id;
        this.tagName = tagName;
        updateLastEdited("Server");
    }

    /**
     * Returns the Id of the tag. May be {@code null}, which means that this tag is temporary or not registered properly.
     * @return The id of the tag or null.
     */
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StationTag copy() {
        return new StationTag(null, new TagName(getTagName().get()), new HashMap<>(getAllStations()));
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (tagName == null) {
            return nbt;
        }

        DLUtils.doIfNotNull(id, (i) -> nbt.putUUID(NBT_ID, i));
        nbt.put(NBT_TAG_NAME, getTagName().toNbt());
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

    public static StationTag fromNbt(CompoundTag nbt, UUID overwriteId) {
        UUID id = overwriteId == null ? (nbt.contains(NBT_ID) ? nbt.getUUID(NBT_ID) : null) : overwriteId;
        TagName name = TagName.fromNbt(nbt.getCompound(!nbt.contains(NBT_TAG_NAME) ? LEGACY_NBT_TAG_NAME : NBT_TAG_NAME));
        String lastEditorName = nbt.contains(NBT_LAST_EDITOR) ? nbt.getString(NBT_LAST_EDITOR) : null;
        long lastEditedTime = nbt.getLong(NBT_LAST_EDITED_TIME);
        Map<String, StationInfo> stations;
        if (nbt.contains(NBT_STATION_LIST)) {
            stations = new HashMap<>(nbt.getList(NBT_STATION_LIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).collect(Collectors.toMap(x -> x, x -> StationInfo.empty())));
        } else if (nbt.contains(NBT_STATION_MAP)) {
            stations = new HashMap<>(nbt.getList(NBT_STATION_MAP, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).collect(Collectors.toMap(x -> {
                return x.getString(NBT_STATION_ENTRY_NAME);
            }, x -> {
                return StationInfo.fromNbt(x);
            })));
        } else {
            stations = new IdentityHashMap<>();
        }

        return new StationTag(id, name, stations, lastEditorName, lastEditedTime);
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


    public TagName getTagName() {
        return this.tagName;
    }

    public void updateInfoForStation(String station, StationInfo info) {
        if (stations.containsKey(station)) {
            stations.replace(station, info);
        }
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

    /**
     * @param stationName The name of the train station.
     * @return {@code true} if the station is part of this tag.
     */
    public boolean contains(String stationName) {
        String regex = stationName.isBlank() ? stationName : "\\Q" + stationName.replace("*", "\\E.*\\Q");
        return stations.keySet().stream().anyMatch(x -> x.matches(regex));
    }

    public Set<String> getAllStationNames() {
        return Set.copyOf(stations.keySet());
    }

    public Map<String, StationInfo> getAllStations() {
        return Map.copyOf(stations);
    }

    public StationInfo getInfoForStation(String stationName) {
        return stations.containsKey(stationName) ? stations.get(stationName) : StationInfo.empty();
    }

    public void setName(TagName name) {
        this.tagName = name;
    }

    public void remove(String station) {
        stations.remove(station);
    }

    public ClientStationTag getClientTag(String station) {
        return new ClientStationTag(getTagName().get(), station, getInfoForStation(station), getId());
    }

    @Override
    public String toString() {
        return getTagName().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StationTag alias) {
            return getTagName().equals(alias.getTagName()) && getAllStationNames().size() == alias.getAllStationNames().size() && getAllStationNames().stream().allMatch(x -> alias.contains(x));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 7 * Objects.hash(tagName);
    }

    public void applyFrom(StationTag newData) {
        this.tagName = newData.tagName;
        this.stations.clear();
        this.stations.putAll(newData.stations);
        this.lastEditedTime = newData.lastEditedTime;
        this.lastEditorName = newData.lastEditorName;
    }

    /**
     * Information about one specific train station (not station tag!)
     */
    public static record StationInfo(String platform) {

        public static final int MAX_PLATFORM_NAME_LENGTH = 8;

        private static final String NBT_PLATFORM = "Platform";

        public static StationInfo empty() {
            return new StationInfo("");
        }

        public boolean isPlatformKnown() {
            return platform() != null && !platform().isBlank();
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_PLATFORM, platform());
            return nbt;
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
                return
                    platform().equals(other.platform())
                ;
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(platform());
        }
    }
}
