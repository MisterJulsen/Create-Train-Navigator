package de.mrjulsen.crn.data.storage;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.ArrayList;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.data.train.ScheduleSection;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

public class GlobalSettings implements INBTSerializable {

    /** @deprecated For data migration only. Use {@code FILENAME} instead. */
    @Deprecated
    public static final String LEGACY_FILENAME = "createrailwaysnavigator_global_settings.dat";
    @Deprecated
    private static final String LEGACY_NBT_TRAIN_GROUPS = "TrainGroups";

    public static final String FILENAME = CreateRailwaysNavigator.MOD_ID + "_global_settings.nbt";

    public static final int DATA_VERSION = 2;

    private static final String NBT_VERSION = "Version";
    private static final String NBT_STATION_TAGS = "StationTags";
    private static final String NBT_TRAIN_CATEGORIES = "TrainCategories";
    private static final String NBT_STATION_BLACKLIST = "StationBlacklist";
    private static final String NBT_TRAIN_BLACKLIST = "TrainBlacklist";
    private static final String NBT_TRAIN_LINES = "TrainLines";

    private final MinecraftServer server;

    private final Map<UUID, StationTag> stationTags = new ConcurrentHashMap<>();
    private final Map<UUID, TrainCategory> trainCategories = new ConcurrentHashMap<>();
    private final Map<UUID, TrainLine> trainLines = new ConcurrentHashMap<>();
    private final Set<String> stationBlacklist = new ConcurrentSkipListSet<>();
    private final Set<String> trainBlacklist = new ConcurrentSkipListSet<>();

    private static GlobalSettings instance;

    
    private GlobalSettings(MinecraftServer server) {
        this.server = server;
    }

    public static boolean modificationsAllowed(Player player) {
        return player.hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_PERMISSION_LEVEL.get());
    }

    public synchronized static GlobalSettings getInstance() {
        if (instance == null) {
            try {
                instance = GlobalSettings.open(ModCommonEvents.getCurrentServer().get());
            } catch (Exception e) {
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
            if (ModCommonConfig.ADVANCED_LOGGING.get()) CreateRailwaysNavigator.LOGGER.info("Saved global settings.");
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("Unable to save global settings.", e);
        }    
    }
    
    public synchronized static GlobalSettings open(MinecraftServer server) throws Exception {   
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
    
    public synchronized CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_VERSION, DATA_VERSION);

        CompoundTag stationsComp = new CompoundTag();
        this.stationTags.entrySet().forEach(x -> stationsComp.put(x.getKey().toString(), x.getValue().toNbt()));
        nbt.put(NBT_STATION_TAGS, stationsComp);
        
        CompoundTag trainCategoriesComp = new CompoundTag();
        this.trainCategories.entrySet().forEach(x -> trainCategoriesComp.put(x.getKey().toString(), x.getValue().toNbt()));
        nbt.put(NBT_TRAIN_CATEGORIES, trainCategoriesComp);
        
        ListTag stationsBlacklist = new ListTag();
        this.stationBlacklist.forEach(x -> stationsBlacklist.add(StringTag.valueOf(x)));
        nbt.put(NBT_STATION_BLACKLIST, stationsBlacklist);
        
        ListTag trainsBlacklist = new ListTag();
        this.trainBlacklist.forEach(x -> trainsBlacklist.add(StringTag.valueOf(x)));
        nbt.put(NBT_TRAIN_BLACKLIST, trainsBlacklist);
        
        CompoundTag trainLinesComp = new CompoundTag();
        this.trainLines.entrySet().forEach(x -> trainLinesComp.put(x.getKey().toString(), x.getValue().toNbt()));
        nbt.put(NBT_TRAIN_LINES, trainLinesComp);

        return nbt;
    }
    
    public void deserializeNbt(CompoundTag nbt) {
        int version = nbt.getInt(NBT_VERSION);

        CompoundTag stationsComp = nbt.getCompound(NBT_STATION_TAGS);
        this.stationTags.putAll(stationsComp.getAllKeys().stream().map(x -> StationTag.fromNbt(stationsComp.getCompound(x), UUID.fromString(x))).collect(Collectors.toMap(x -> x.getId(), x -> x)));
        CompoundTag trainCategoiesComp = version <= 1 ? nbt.getCompound(LEGACY_NBT_TRAIN_GROUPS) :  nbt.getCompound(NBT_TRAIN_CATEGORIES);
        this.trainCategories.putAll(trainCategoiesComp.getAllKeys().stream().map(x -> TrainCategory.fromNbt(trainCategoiesComp.getCompound(x))).collect(Collectors.toMap(x -> x.getId(), x -> x)));
        this.stationBlacklist.addAll(nbt.getList(NBT_STATION_BLACKLIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList());        
        this.trainBlacklist.addAll(nbt.getList(NBT_TRAIN_BLACKLIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList());
        CompoundTag trainLinesComp = nbt.getCompound(NBT_TRAIN_LINES);
        this.trainLines.putAll(trainLinesComp.getAllKeys().stream().map(x -> TrainLine.fromNbt(trainLinesComp.getCompound(x))).collect(Collectors.toMap(x -> x.getId(), x -> x)));
        
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
        trainCategories.putAll(trainGroupData.stream().map(x -> TrainCategory.fromNbt(x)).collect(Collectors.toMap(x -> x.getId(), x -> x)));
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
        for (StationTag tag : stationTags.values()) {
            if (tag.contains(stationName)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean stationTagExists(String tagName) {
        return stationTagExists(TagName.of(tagName));
    }

    public boolean stationTagExists(TagName tagName) {
        for (StationTag tag : stationTags.values()) {
            if (tag.getTagName().equals(tagName)) {
                return true;
            }
        }
        return false;
    }

    public boolean stationTagExists(UUID id) {
        if (id == null) {
            return false;
        }
        return stationTags.containsKey(id);
    }
        
    public StationTag getOrCreateStationTagFor(GlobalStation station) {
        return getOrCreateStationTagFor(station.name);
    }
    
    public StationTag getOrCreateStationTagFor(TagName tagName) {
        return getTagByName(tagName).orElse(getOrCreateStationTagFor(tagName.get()));
    }

    
    public StationTag getOrCreateStationTagFor(String stationName) {
        return getOrCreateStationTagFor(stationName, null);
    }
    
    /**
     * @param stationName The name of the train station.
     * @return Returns the station tag for the given train station.
     */
    public StationTag getOrCreateStationTagFor(String stationName, Owner owner) {
        if (stationName.contains("*")) {
            return getOrCreateTagForWildcard(stationName, owner);
        }

        for (StationTag tag : stationTags.values()) {
            if (tag.contains(stationName)) {
                return tag;
            }
        }
        return new StationTag(null, TagName.of(stationName), owner, Map.of(stationName, StationInfo.empty()));
    }

    private StationTag getOrCreateTagForWildcard(String stationName, Owner owner) {
		String regex = stationName.isBlank() ? stationName : "\\Q" + stationName.replace("*", "\\E.*\\Q") + "\\E";
        for (StationTag tag : stationTags.values()) {
            for (String name : tag.getAllStationNames()) {
                if (name.matches(regex)) {
                    return tag;
                }
            }
        }
        
        return new StationTag(null, TagName.of(stationName), owner, Map.of(stationName, StationInfo.empty()));        
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
        return createOrGetStationTag(name, null);
    }

    /**
     * Get the station tag with the given name or create and register a new one, if no tag exists.
     * @param name The name of the station tag.
     * @param owner The owner of the station tag, who has full control over it.
     * @return The station tag for the name.
     */
    public StationTag createOrGetStationTag(TagName name, Owner owner) {
        Optional<StationTag> tag = getTagByName(name);
        if (tag.isPresent()) {
            return tag.get();
        }
        UUID newId;
        do {
            newId = UUID.randomUUID();
        } while (stationTags.containsKey(newId));
        StationTag newTag = new StationTag(newId, name, owner);
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
        for (StationTag tag : stationTags.values()) {
            if (tag.getTagName().equals(name)) {
                return Optional.ofNullable(tag);
            }
        }
        return Optional.empty();
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

    public List<StationTag> getAllStationTags() {
        return new ArrayList<>(stationTags.values());
    }

//#endregion
//#region +++ TRAIN CATEGORIES +++

    public boolean trainCategoryExists(UUID id) {
        if (id == null) {
            return false;
        }
        return trainCategories.containsKey(id);
    }

    /**
     * Get the train group with the given name or create and register a new one, if no group exists.
     * @param name The name of the train group.
     * @return The train group for the name.
     */
    public TrainCategory createOrGetTrainCategory(String name) {
        return createOrGetTrainCategory(name, null);
    }

    /**
     * Get the train group with the given name or create and register a new one, if no group exists.
     * @param name The name of the train group.
     * @return The train group for the name.
     */
    public TrainCategory createOrGetTrainCategory(String name, Owner owner) {
        Optional<TrainCategory> tag = getTrainCategoryByName(name);
        if (tag.isPresent()) {
            return tag.get();
        }

        UUID id;
        do {
            id = UUID.randomUUID();
        } while (trainCategories.containsKey(id));

        TrainCategory newCategory = new TrainCategory(id, name, owner);
        trainCategories.put(newCategory.getId(), newCategory);
        return newCategory;
    }

    public Optional<TrainCategory> getTrainCategory(UUID id) {
        return Optional.ofNullable(trainCategoryExists(id) ? trainCategories.get(id) : null);
    }
    
    public Optional<TrainCategory> getTrainCategoryByName(String name) {
        for (TrainCategory category : trainCategories.values()) {
            if (category.getCategoryName().equals(name)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    public TrainCategory removeTrainCategory(UUID id) {
        return trainCategories.remove(id);
    }

    public ImmutableList<TrainCategory> getAllTrainCategories() {
        return ImmutableList.copyOf(trainCategories.values());
    }

    public boolean isTrainExcludedByUser(Train train, UserSettings settings) {
        return TrainListener.getTrainData(train.id).map(data -> {
            if (data.getSections().isEmpty()) {
                return false;
            }
    
            for (ScheduleSection section : data.getSections()) {
                if (section.isUsable() && !(section.getTrainCategory().map(x -> settings.navigationExcludedTrainCategories.getValue().contains(x.getId())).orElse(false))) {
                    return false;
                }
            }
            return true;
        }).orElse(false);        
    }

    public boolean isTrainStationExcludedByUser(Train train, TrainPrediction at, UserSettings settings) {
        return at.getSection().getTrainCategory().map(x -> !at.getSection().isUsable() || (settings.navigationExcludedTrainCategories.getValue().contains(x.getId()))).orElse(false);
    }

    public boolean isTrainStationExcludedByUser(Train train, TrainStop at, UserSettings settings) {
        return TrainListener.getTrainData(train.id).map(data -> {
            ScheduleSection section = data.getSectionByIndex(at.getSectionIndex());
            return section.getTrainCategory().map(x -> !section.isUsable() || (settings.navigationExcludedTrainCategories.getValue().contains(x.getId()))).orElse(false);
        }).orElse(false);        
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
        if (ModUtils.hasWildcards(stationName)) {
            stationBlacklist.addAll(ModUtils.wildcardMatches(stationName, TrainUtils.getAllStationNames()));
            return;
        }
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

        Collection<String> names = tag.getAllStationNames();
        for (String name : names) {
            if (!isStationBlacklisted(name)) {
                return false;
            }
        }
        return !names.isEmpty();
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
        if (ModUtils.hasWildcards(trainName)) {
            trainBlacklist.addAll(ModUtils.wildcardMatches(trainName, TrainUtils.getTrainNames()));
            return;
        }
        trainBlacklist.add(trainName);
    }

    public boolean removeTrainFromBlacklist(Train train) {
        return removeTrainFromBlacklist(train.name.getString());
    }

    public boolean removeTrainFromBlacklist(String trainName) {
        return trainBlacklist.removeIf(x -> x.equals(trainName));
    }

    public ImmutableList<String> getAllBlacklistedTrains() {
        return ImmutableList.copyOf(trainBlacklist);
    }

//#endregion

//#region +++ TRAIN LINES +++

    public boolean trainLineExists(UUID id) {
        if (id == null) {
            return false;
        }
        return trainLines.containsKey(id);
    }

    public TrainLine createOrGetTrainLine(String name) {
        return createOrGetTrainLine(name, null);
    }

    public TrainLine createOrGetTrainLine(String name, Owner owner) {
        Optional<TrainLine> tag = getTrainLineByName(name);
        if (tag.isPresent()) {
            return tag.get();
        }

        UUID id;
        do {
            id = UUID.randomUUID();
        } while (trainCategories.containsKey(id));

        TrainLine newLine = new TrainLine(id, name, owner);
        trainLines.put(newLine.getId(), newLine);
        return newLine;
    }

    public Optional<TrainLine> getTrainLine(UUID id) {
        return Optional.ofNullable(trainLineExists(id) ? trainLines.get(id) : null);
    }

    public Optional<TrainLine> getTrainLineByName(String name) {
        for (TrainLine line : trainLines.values()) {
            if (line.getLineName().equals(name)) {
                return Optional.of(line);
            }
        }
        return Optional.empty();
    }

    public TrainLine removeTrainLine(UUID id) {
        return trainLines.remove(id);
    }

    public ImmutableList<TrainLine> getAllTrainLines() {
        return ImmutableList.copyOf(trainLines.values());
    }

//#endregion

}
