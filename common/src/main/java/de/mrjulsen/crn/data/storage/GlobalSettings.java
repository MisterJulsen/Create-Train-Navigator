package de.mrjulsen.crn.data.storage;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Map;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainTravelSection;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class GlobalSettings implements INBTSerializable {

    /** @deprecated For data migration only. Use {@code FILENAME} instead. */
    @Deprecated
    public static final String LEGACY_FILENAME = "createrailwaysnavigator_global_settings.dat";

    public static final String FILENAME = CreateRailwaysNavigator.MOD_ID + "_global_settings.nbt";

    public static final int DATA_VERSION = 1;

    private static final String NBT_VERSION = "Version";
    private static final String NBT_STATION_TAGS = "StationTags";
    private static final String NBT_TRAIN_GROUPS = "TrainGroups";
    private static final String NBT_STATION_BLACKLIST = "StationBlacklist";
    private static final String NBT_TRAIN_BLACKLIST = "TrainBlacklist";
    private static final String NBT_TRAIN_LINES = "TrainLines";

    private final MinecraftServer server;

    private final Map<UUID, StationTag> stationTags = new HashMap<>();
    private final Map<String, TrainGroup> trainGroups = new HashMap<>();
    private final Set<String> stationBlacklist = new HashSet<>();
    private final Set<String> trainBlacklist = new HashSet<>();
    private final Map<String, TrainLine> trainLines = new HashMap<>();

    private static GlobalSettings instance;

    
    private GlobalSettings(MinecraftServer server) {
        this.server = server;
    }

    public synchronized static GlobalSettings getInstance() {
        if (instance == null) {
            try {
                instance = GlobalSettings.open(ModCommonEvents.getCurrentServer().get());
            } catch (IOException e) {
                CreateRailwaysNavigator.LOGGER.error("Unable to open settings file.", e);
                instance = new GlobalSettings(ModCommonEvents.getCurrentServer().get());
            }
        }
        return instance;
    }

    public static boolean hasInstance() {
        return instance != null;
    }

    public static void clearInstance() {
        if (instance != null) {
            instance.close();
        }
        instance = null;
    }

    public synchronized void save() {
        CompoundTag nbt = this.serializeNbt();
    
        try {
            NbtIo.writeCompressed(nbt, new File(server.getWorldPath(new LevelResource("data/" + FILENAME)).toString()));
            CreateRailwaysNavigator.LOGGER.info("Saved global settings.");
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("Unable to save global settings.", e);
        }    
    }
    
    public synchronized static GlobalSettings open(MinecraftServer server) throws IOException {   
        File legacyFile = new File(server.getWorldPath(new LevelResource("data/" + LEGACY_FILENAME)).toString()); 
        File settingsFile = new File(server.getWorldPath(new LevelResource("data/" + FILENAME)).toString());    

        GlobalSettings file = new GlobalSettings(server);  

        if (legacyFile.exists()) {
            CreateRailwaysNavigator.LOGGER.warn("A legacy global settings file was found. Try to load it.");
            file.deserializeNbtLegacy(NbtIo.readCompressed(legacyFile).getCompound("data"));
            legacyFile.delete();
        } else if (settingsFile.exists()) {
            file.deserializeNbt(NbtIo.readCompressed(settingsFile));
        }
        return file;
    }
    
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_VERSION, DATA_VERSION);

        CompoundTag stationsComp = new CompoundTag();
        this.stationTags.entrySet().forEach(x -> stationsComp.put(x.getKey().toString(), x.getValue().toNbt()));
        nbt.put(NBT_STATION_TAGS, stationsComp);
        
        CompoundTag trainGroupComp = new CompoundTag();
        this.trainGroups.entrySet().forEach(x -> trainGroupComp.put(x.getKey().toString(), x.getValue().toNbt()));
        nbt.put(NBT_TRAIN_GROUPS, trainGroupComp);
        
        ListTag stationsBlacklist = new ListTag();
        this.stationBlacklist.stream().forEach(x -> stationsBlacklist.add(StringTag.valueOf(x)));
        nbt.put(NBT_STATION_BLACKLIST, stationsBlacklist);
        
        ListTag trainsBlacklist = new ListTag();
        this.trainBlacklist.stream().forEach(x -> trainsBlacklist.add(StringTag.valueOf(x)));
        nbt.put(NBT_TRAIN_BLACKLIST, trainsBlacklist);
        
        CompoundTag trainLinesComp = new CompoundTag();
        this.trainLines.entrySet().forEach(x -> trainLinesComp.put(x.getKey().toString(), x.getValue().toNbt()));
        nbt.put(NBT_TRAIN_LINES, trainLinesComp);

        return nbt;
    }
    
    public void deserializeNbt(CompoundTag nbt) {
        @SuppressWarnings("unused") int version = nbt.getInt(NBT_VERSION);

        CompoundTag stationsComp = nbt.getCompound(NBT_STATION_TAGS);
        this.stationTags.putAll(stationsComp.getAllKeys().stream().map(x -> StationTag.fromNbt(stationsComp.getCompound(x), UUID.fromString(x))).collect(Collectors.toMap(x -> x.getId(), x -> x)));
        CompoundTag trainGroupsComp = nbt.getCompound(NBT_TRAIN_GROUPS);
        this.trainGroups.putAll(trainGroupsComp.getAllKeys().stream().map(x -> TrainGroup.fromNbt(trainGroupsComp.getCompound(x))).collect(Collectors.toMap(x -> x.getGroupName(), x -> x)));
        this.stationBlacklist.addAll(nbt.getList(NBT_STATION_BLACKLIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList());        
        this.trainBlacklist.addAll(nbt.getList(NBT_TRAIN_BLACKLIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList());
        CompoundTag trainLinesComp = nbt.getCompound(NBT_TRAIN_LINES);
        this.trainLines.putAll(trainLinesComp.getAllKeys().stream().map(x -> TrainLine.fromNbt(trainLinesComp.getCompound(x))).collect(Collectors.toMap(x -> x.getLineName(), x -> x)));
        
    }

    /**
     * @deprecated For data migration only. Use {@code deserializeNbt} instead.
     */
    @Deprecated
    private void deserializeNbtLegacy(CompoundTag nbt) {        
        final String NBT_ALIAS_REGISTRY = "RegisteredAliasData";
        final String NBT_BLACKLIST = "StationBlacklist";
        final String NBT_TRAIN_BLACKLIST = "TrainBlacklist";
        final String NBT_TRAIN_GROUP_REGISTRY = "RegisteredTrainGroups";

        Collection<CompoundTag> aliasData = new ArrayList<>();
        Collection<CompoundTag> trainGroupData = new ArrayList<>();
        Collection<String> blacklistData = new ArrayList<>();
        Collection<String> trainBlacklistData = new ArrayList<>();

        if (nbt.contains(NBT_ALIAS_REGISTRY)) {
            aliasData = nbt.getList(NBT_ALIAS_REGISTRY, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList();
        }

        if (nbt.contains(NBT_TRAIN_GROUP_REGISTRY)) {
            trainGroupData = nbt.getList(NBT_TRAIN_GROUP_REGISTRY, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList();
        }

        if (nbt.contains(NBT_BLACKLIST)) {
            blacklistData = nbt.getList(NBT_BLACKLIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList();
        }

        if (nbt.contains(NBT_TRAIN_BLACKLIST)) {
            trainBlacklistData = nbt.getList(NBT_TRAIN_BLACKLIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList();
        }

        Set<UUID> usedIds = new LinkedHashSet<>();
        stationTags.putAll(aliasData.stream().map(x -> {
            UUID id;
            do {
                id = UUID.randomUUID();
            } while (usedIds.contains(id));
            usedIds.add(id);
            return StationTag.fromNbt(x, id);
        }).collect(Collectors.toMap(x -> x.getId(), x -> x)));
        usedIds.clear();
        trainGroups.putAll(trainGroupData.stream().map(x -> TrainGroup.fromNbt(x)).collect(Collectors.toMap(x -> x.getGroupName(), x -> x)));
        stationBlacklist.addAll(blacklistData); 
        trainBlacklist.addAll(trainBlacklistData);

        save();
    }
    
    public void close() {
        this.save();
    }

//#region +++ STATION TAGS +++

    public boolean hasStationTag(GlobalStation station) {
        return hasStationTag(station.name);
    }

    public boolean hasStationTag(String stationName) {
        return stationTags.values().stream().anyMatch(x -> x.contains(stationName));
    }
    
    public boolean stationTagExists(String tagName) {
        return stationTagExists(TagName.of(tagName));
    }

    public boolean stationTagExists(TagName tagName) {
        return stationTags.values().stream().anyMatch(x -> x.getTagName().equals(tagName));
    }

    public boolean stationTagExists(UUID id) {
        return stationTags.containsKey(id);
    }
        
    public StationTag getOrCreateStationTagFor(GlobalStation station) {
        return getOrCreateStationTagFor(station.name);
    }
    
    public StationTag getOrCreateStationTagFor(TagName tagName) {
        return getTagByName(tagName).orElse(getOrCreateStationTagFor(tagName.get()));
    }
    
    /**
     * @param stationName The name of the train station.
     * @return Returns the station tag for the given train station.
     */
    public StationTag getOrCreateStationTagFor(String stationName) {
        if (stationName.contains("*")) {
            return getOrCreateTagForWildcard(stationName);
        }

        Optional<StationTag> a = stationTags.values().stream().filter(x -> x.contains(stationName)).findFirst();
        if (a.isPresent()) {            
            return a.get();
        }

        return new StationTag(null, TagName.of(stationName), Map.of(stationName, StationInfo.empty()));
    }

    private StationTag getOrCreateTagForWildcard(String stationName) {
		String regex = stationName.isBlank() ? stationName : "\\Q" + stationName.replace("*", "\\E.*\\Q") + "\\E";
        Optional<StationTag> a = stationTags.values().stream().filter(x -> x.getAllStationNames().stream().anyMatch(y -> y.matches(regex))).findFirst();
        if (a.isPresent()) {          
            return a.get();
        }
        
        return new StationTag(null, TagName.of(stationName), Map.of(stationName, StationInfo.empty()));        
    }
    
    /**
     * Get the station tag with the given name or create and register a new one, if no tag exists.
     * @param name The name of the station tag.
     * @return The station tag for the name.
     */
    public StationTag createOrGetStationTag(String name) {
        return createOrGetStationTag(TagName.of(name));
    }
    
    /**
     * Get the station tag with the given name or create and register a new one, if no tag exists.
     * @param name The name of the station tag.
     * @return The station tag for the name.
     */
    public StationTag createOrGetStationTag(TagName name) {
        Optional<StationTag> tag = getTagByName(name);
        if (tag.isPresent()) {
            return tag.get();
        }
        UUID newId;
        do {
            newId = UUID.randomUUID();
        } while (stationTags.containsKey(newId));
        StationTag newTag = new StationTag(newId, name);
        stationTags.put(newId, newTag);
        return newTag;
    }

    public StationTag registerStationTag(StationTag tag) {
        UUID newId;
        do {
            newId = UUID.randomUUID();
        } while (stationTags.containsKey(newId));
        tag.setId(newId);
        stationTags.put(newId, tag);
        return tag;
    }
    
    public Optional<StationTag> getTagByName(TagName name) {
        return stationTags.values().stream().filter(x -> x.getTagName().equals(name)).findFirst();
    }
    
    public Optional<StationTag> getStationTag(UUID id) {
        return Optional.ofNullable(stationTagExists(id) ? stationTags.get(id) : null);
    }

    public boolean removeStationTag(String name) {
        return removeStationTag(TagName.of(name));
    }    

    public boolean removeStationTag(TagName name) {
        return stationTags.values().removeIf(x -> x.getTagName().equals(name));
    }

    public StationTag removeStationTag(UUID id) {
        return stationTags.remove(id);
    }

    public ImmutableList<StationTag> getAllStationTags() {
        return ImmutableList.copyOf(stationTags.values());
    }

//#endregion
//#region +++ TRAIN GROUPS +++

    public boolean trainGroupExists(String name) {
        return trainGroups.containsKey(name);
    }

    /**
     * Get the train group with the given name or create and register a new one, if no group exists.
     * @param name The name of the train group.
     * @return The train group for the name.
     */
    public TrainGroup createOrGetTrainGroup(String name) {
        Optional<TrainGroup> tag = getTrainGroup(name);
        if (tag.isPresent()) {
            return tag.get();
        }
        TrainGroup newGroup = new TrainGroup(name);
        trainGroups.put(name, newGroup);
        return newGroup;
    }

    public Optional<TrainGroup> getTrainGroup(String name) {
        return Optional.ofNullable(trainGroupExists(name) ? trainGroups.get(name) : null);
    }

    public TrainGroup removeTrainGroup(String name) {
        return trainGroups.remove(name);
    }

    public ImmutableList<TrainGroup> getAllTrainGroups() {
        return ImmutableList.copyOf(trainGroups.values());
    }

    public boolean isTrainExcludedByUser(Train train, UserSettings settings) {  
        return !TrainListener.data.get(train.id).getSections().isEmpty() && TrainListener.data.get(train.id).getSections().stream().allMatch(x -> !x.isUsable() || (x.getTrainGroup() != null && settings.navigationExcludedTrainGroups.getValue().contains(x.getTrainGroup().getGroupName())));
        //List<TrainGroup> groupsOfTrain = getTrainGroupsOfTrain(train);
        //Set<TrainGroup> excludedGroups = settings.navigationExcludedTrainGroups.getValue();
        //return !groupsOfTrain.isEmpty() && !excludedGroups.isEmpty() && groupsOfTrain.stream().allMatch(a -> excludedGroups.stream().anyMatch(b -> a.getId().equals(b.getId())));
    }

    public boolean isTrainStationExcludedByUser(Train train, TrainPrediction at, UserSettings settings) {
        return at.getSection().getTrainGroup() != null && (!at.getSection().isUsable() || (at.getSection().getTrainGroup() != null && settings.navigationExcludedTrainGroups.getValue().contains(at.getSection().getTrainGroup().getGroupName())));
    }

    public boolean isTrainStationExcludedByUser(Train train, TrainStop at, UserSettings settings) {
        TrainTravelSection section = TrainListener.data.get(train.id).getSectionByIndex(at.getSectionIndex());
        return section.getTrainGroup() != null && (!section.isUsable() || (section.getTrainGroup() != null && settings.navigationExcludedTrainGroups.getValue().contains(section.getTrainGroup().getGroupName())));
    }

//#endregion
//#region +++ STATION BLACKLIST +++

    public boolean isStationBlacklisted(GlobalStation station) {
        return isStationBlacklisted(station.name);
    }

    public boolean isStationBlacklisted(String name) {
        return stationBlacklist.contains(name);
    }
    
    public void blacklistStation(GlobalStation station) {
        blacklistStation(station.name);
    }

    public void blacklistStation(String stationName) {
        stationBlacklist.add(stationName);
    }
    
    public boolean removeStationFromBlacklist(GlobalStation station) {
        return removeStationFromBlacklist(station.name);
    }

    public boolean removeStationFromBlacklist(String stationName) {
        return stationBlacklist.removeIf(x -> x.equals(stationName));
    }

    public boolean isEntireStationTagBlacklisted(StationTag tag) {
        if (tag == null) {
            return true;
        }
        return tag.getAllStationNames().stream().allMatch(x -> isStationBlacklisted(x));
    }

    public ImmutableList<String> getAllBlacklistedStations() {
        return ImmutableList.copyOf(stationBlacklist);
    }

//#endregion
//#region +++ TRAIN BLACKLIST +++

    public boolean isTrainBlacklisted(Train train) {
        return isTrainBlacklisted(train.name.getString());
    }

    public boolean isTrainBlacklisted(String trainName) {
        return trainBlacklist.contains(trainName);
    }

    public void blacklistTrain(Train train) {
        blacklistTrain(train.name.getString());
    }
    
    public void blacklistTrain(String trainName) {
        trainBlacklist.add(trainName);
    }

    public boolean removeTrainFromBlacklist(Train train) {
        return removeTrainFromBlacklist(train.name.getString());
    }

    public boolean removeTrainFromBlacklist(String trainName) {
        return trainBlacklist.removeIf(x -> x.equals(trainName));
    }

    /* TODO
    public boolean isEntireTrainGroupBlacklisted(TrainGroup tag) {
        if (tag == null) {
            return true;
        }
        return tag.getTrainNames().stream().allMatch(x -> isTrainBlacklisted(x));
    }
        */

    public ImmutableList<String> getAllBlacklistedTrains() {
        return ImmutableList.copyOf(trainBlacklist);
    }

//#endregion

//#region +++ TRAIN LINES +++

    public boolean trainLineExists(String name) {
        return trainLines.containsKey(name);
    }

    public TrainLine createOrGetTrainLine(String name) {
        Optional<TrainLine> tag = getTrainLine(name);
        if (tag.isPresent()) {
            return tag.get();
        }
        TrainLine newGroup = new TrainLine(name);
        trainLines.put(newGroup.getLineName(), newGroup);
        return newGroup;
    } 

    public Optional<TrainLine> getTrainLine(String name) {
        return Optional.ofNullable(trainLineExists(name) ? trainLines.get(name) : null);
    }

    public TrainLine removeTrainLine(String name) {
        return trainLines.remove(name);
    }

    public ImmutableList<TrainLine> getAllTrainLines() {
        return ImmutableList.copyOf(trainLines.values());
    }

//#endregion

}
