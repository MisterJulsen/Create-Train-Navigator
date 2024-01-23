package de.mrjulsen.crn.data;

import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.data.TrainStationAlias.StationInfo;
import de.mrjulsen.crn.network.packets.cts.GlobalSettingsUpdatePacket;
import de.mrjulsen.crn.network.packets.cts.GlobalSettingsUpdatePacket.EGlobalSettingsAction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class GlobalSettings {

    private static final String NBT_ALIAS_REGISTRY = "RegisteredAliasData";
    private static final String NBT_BLACKLIST = "StationBlacklist";
    private static final String NBT_TRAIN_BLACKLIST = "TrainBlacklist";
    private static final String NBT_TRAIN_GROUP_REGISTRY = "RegisteredTrainGroups";

    private final Collection<TrainStationAlias> registeredAlias = new ArrayList<>();
    private final Collection<TrainGroup> registeredTrainGroups = new ArrayList<>();
    private final Collection<String> blacklist = new ArrayList<>();
    private final Collection<String> trainsBlacklist = new ArrayList<>();


    protected GlobalSettings() {      
    }
    
    public CompoundTag toNbt(CompoundTag pCompoundTag) {
        if (registeredAlias != null && !registeredAlias.isEmpty()) {
            ListTag aliasTag = new ListTag();
            aliasTag.addAll(registeredAlias.stream().map(x -> x.toNbt()).toList());        
            pCompoundTag.put(NBT_ALIAS_REGISTRY, aliasTag);
        }

        if (registeredTrainGroups != null && !registeredTrainGroups.isEmpty()) {
            ListTag aliasTag = new ListTag();
            aliasTag.addAll(registeredTrainGroups.stream().map(x -> x.toNbt()).toList());        
            pCompoundTag.put(NBT_TRAIN_GROUP_REGISTRY, aliasTag);
        }
        
        if (blacklist != null && !blacklist.isEmpty()) {
            ListTag blacklistTag = new ListTag();
            blacklistTag.addAll(blacklist.stream().map(x -> StringTag.valueOf(x)).toList());       
            pCompoundTag.put(NBT_BLACKLIST, blacklistTag);
        }

        if (trainsBlacklist != null && !trainsBlacklist.isEmpty()) {
            ListTag blacklistTag = new ListTag();
            blacklistTag.addAll(trainsBlacklist.stream().map(x -> StringTag.valueOf(x)).toList());
            pCompoundTag.put(NBT_TRAIN_BLACKLIST, blacklistTag);
        }

        return pCompoundTag;
    }

    public static GlobalSettings fromNbt(CompoundTag tag) {
        Collection<TrainStationAlias> aliasData = new ArrayList<>();
        Collection<TrainGroup> trainGroupData = new ArrayList<>();
        Collection<String> blacklistData = new ArrayList<>();
        Collection<String> trainBlacklistData = new ArrayList<>();

        if (tag.contains(NBT_ALIAS_REGISTRY)) {
            aliasData = tag.getList(NBT_ALIAS_REGISTRY, Tag.TAG_COMPOUND).stream().map(x -> TrainStationAlias.fromNbt((CompoundTag)x)).toList();
        }

        if (tag.contains(NBT_TRAIN_GROUP_REGISTRY)) {
            trainGroupData = tag.getList(NBT_TRAIN_GROUP_REGISTRY, Tag.TAG_COMPOUND).stream().map(x -> TrainGroup.fromNbt((CompoundTag)x)).toList();
        }

        if (tag.contains(NBT_BLACKLIST)) {
            blacklistData = tag.getList(NBT_BLACKLIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList();
        }

        if (tag.contains(NBT_TRAIN_BLACKLIST)) {
            trainBlacklistData = tag.getList(NBT_TRAIN_BLACKLIST, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).toList();
        }

        GlobalSettings instance = new GlobalSettings();
        instance.registeredAlias.addAll(aliasData);
        instance.blacklist.addAll(blacklistData); 
        instance.trainsBlacklist.addAll(trainBlacklistData);
        instance.registeredTrainGroups.addAll(trainGroupData);

        return instance;
    }



    // Setter methods for client
    public boolean registerAlias(TrainStationAlias alias, Runnable then) {
        GlobalSettingsUpdatePacket.send(alias, EGlobalSettingsAction.REGISTER_ALIAS, then);
        return true;
    }

    public boolean updateAlias(AliasName name, TrainStationAlias newData, Runnable then) {
        GlobalSettingsUpdatePacket.send(new Object[] { name.get(), newData }, EGlobalSettingsAction.UPDATE_ALIAS, then);
        return true;
    }

    public boolean unregisterAlias(String name, Runnable then) {
        GlobalSettingsUpdatePacket.send(name, EGlobalSettingsAction.UNREGISTER_ALIAS_STRING, then);
        return true;
    }

    public boolean unregisterAlias(TrainStationAlias alias, Runnable then) {
        GlobalSettingsUpdatePacket.send(alias, EGlobalSettingsAction.UNREGISTER_ALIAS, then);
        return true;
    }



    public boolean registerTrainGroup(TrainGroup group, Runnable then) {
        GlobalSettingsUpdatePacket.send(group, EGlobalSettingsAction.REGISTER_TRAIN_GROUP, then);
        return true;
    }

    public boolean updateTrainGroup(String name, TrainGroup newData, Runnable then) {
        GlobalSettingsUpdatePacket.send(new Object[] { name, newData }, EGlobalSettingsAction.UPDATE_TRAIN_GROUP, then);
        return true;
    }

    public boolean unregisterTrainGroupTrain(String trainName, Runnable then) {
        GlobalSettingsUpdatePacket.send(trainName, EGlobalSettingsAction.UNREGISTER_TRAIN_GROUP_TRAIN, then);
        return true;
    }

    public boolean unregisterTrainGroup(TrainGroup group, Runnable then) {
        GlobalSettingsUpdatePacket.send(group, EGlobalSettingsAction.UNREGISTER_TRAIN_GROUP, then);
        return true;
    }



    public boolean addToBlacklist(String station, Runnable then) {        
        GlobalSettingsUpdatePacket.send(station, EGlobalSettingsAction.ADD_TO_BLACKLIST, then);
        return true;    
    }

    public boolean removeFromBlacklist(String name, Runnable then) {
        GlobalSettingsUpdatePacket.send(name, EGlobalSettingsAction.REMOVE_FROM_BLACKLIST, then);
        return true;
    }

    public boolean addTrainToBlacklist(String trainName, Runnable then) {        
        GlobalSettingsUpdatePacket.send(trainName, EGlobalSettingsAction.ADD_TRAIN_TO_BLACKLIST, then);
        return true;    
    }

    public boolean removeTrainFromBlacklist(String name, Runnable then) {
        GlobalSettingsUpdatePacket.send(name, EGlobalSettingsAction.REMOVE_TRAIN_FROM_BLACKLIST, then);
        return true;
    }


    public boolean registerAliasForStationNames(String name, Collection<String> stations, Runnable then) {
        //TODO
        return registerAlias(new TrainStationAlias(AliasName.of(name), stations.stream().collect(Collectors.toMap(x -> x, x -> StationInfo.empty()))), then);
    }

    public boolean registerAlias(String name, Collection<GlobalStation> stations, Runnable then) {
        //TODO
        return registerAlias(new TrainStationAlias(AliasName.of(name), stations.stream().collect(Collectors.toMap(x -> x.name, x -> StationInfo.empty()))), then);
    }


    // Setter methods for server
    public boolean registerAliasServer(TrainStationAlias alias) {
        if (!registeredAlias.contains(alias)) {
            registeredAlias.add(alias);
            return true;
        }
        return false;
    }

    public boolean updateAliasServer(AliasName name, TrainStationAlias newData) {        
        if (!registeredAlias.stream().anyMatch(x -> x.getAliasName().equals(name))) {
            return false;
        }
        registeredAlias.stream().filter(x -> x.getAliasName().equals(name)).forEach(x -> x.update(newData));
        return true;
    }

    public boolean unregisterAliasServer(String name) {        
        boolean b = registeredAlias.removeIf(x -> compareAliasAndString(x, name));
        return b;
    }

    public boolean unregisterAliasServer(TrainStationAlias alias) {
        boolean b = registeredAlias.removeIf(x -> x.equals(alias));
        return b;
    }

    public boolean registerAliasForStationNamesServer(String name, Collection<String> stations) {
        //TODO
        return registerAliasServer(new TrainStationAlias(AliasName.of(name), stations.stream().collect(Collectors.toMap(x -> x, x -> StationInfo.empty()))));
    }

    public boolean registerAliasServer(String name, Collection<GlobalStation> stations) {
        //TODO
        return registerAliasServer(new TrainStationAlias(AliasName.of(name), stations.stream().collect(Collectors.toMap(x -> x.name, x -> StationInfo.empty()))));
    }



    public boolean registerTrainGroupServer(TrainGroup group) {
        if (!registeredTrainGroups.contains(group)) {
            registeredTrainGroups.add(group);
            return true;
        }
        return false;
    }

    public boolean updateTrainGroupServer(String name, TrainGroup newData) {        
        if (!registeredTrainGroups.stream().anyMatch(x -> x.getGroupName().equals(name))) {
            return false;
        }
        registeredTrainGroups.stream().filter(x -> x.getGroupName().equals(name)).forEach(x -> x.update(newData));
        return true;
    }

    public boolean unregisterTrainGroupServer(String name) {        
        boolean b = registeredTrainGroups.removeIf(x -> x.getGroupName().equals(name));
        return b;
    }

    public boolean unregisterAliasServer(TrainGroup group) {
        boolean b = registeredTrainGroups.removeIf(x -> x.equals(group));
        return b;
    }


    public boolean addToBlacklistServer(String station) {        
        if (!blacklist.contains(station)) {
            blacklist.add(station);
            return true;
        }
        return false;
    }

    public boolean removeFromBlacklistServer(String name) {        
        boolean b = blacklist.removeIf(x -> x.equals(name));
        return b;
    }

    public boolean addTrainToBlacklistServer(String trainName) {        
        if (!trainsBlacklist.contains(trainName)) {
            trainsBlacklist.add(trainName);
            return true;
        }
        return false;
    }

    public boolean removeTrainFromBlacklistServer(String trainName) {        
        boolean b = trainsBlacklist.removeIf(x -> x.equals(trainName));
        return b;
    }


    //### Getters and testers
    // tags
    public boolean isAliasRegistered(String stationName) {
        return registeredAlias.stream().anyMatch(x -> x.contains(stationName));
    }

    public boolean isAliasRegistered(GlobalStation station) {
        return isAliasRegistered(station.name);
    }

    private TrainStationAlias getOrCreateAliasFor(String stationName) {
        if (stationName.contains("*")) {
            return getOrCreateAliasForWildcard(stationName);
        }

        Optional<TrainStationAlias> a = registeredAlias.stream().filter(x -> x.contains(stationName)).findFirst();
        if (a.isPresent()) {            
            return a.get();
        }

        return new TrainStationAlias(AliasName.of(stationName), Map.of(stationName, StationInfo.empty())); 
    }

    private TrainStationAlias getOrCreateAliasForWildcard(String stationName) {
		String regex = stationName.isBlank() ? stationName : "\\Q" + stationName.replace("*", "\\E.*\\Q") + "\\E";
        Optional<TrainStationAlias> a = registeredAlias.stream().filter(x -> x.getAllStationNames().stream().anyMatch(y -> y.matches(regex))).findFirst();
        if (a.isPresent()) {          
            return a.get();
        }
        
        return new TrainStationAlias(AliasName.of(stationName), Map.of(stationName, StationInfo.empty()));
        
    }

    private Optional<TrainStationAlias> getAlias(String stationName) {
        Optional<TrainStationAlias> a = registeredAlias.stream().filter(x -> x.getAliasName().equals(AliasName.of(stationName))).findFirst();
        return a;
    }

    public Collection<TrainStationAlias> getAliasList() {
        return registeredAlias;
    }

    public Collection<TrainGroup> getTrainGroupsList() {
        return registeredTrainGroups;
    }

    public TrainStationAlias getAliasFor(String stationName) {
        Optional<TrainStationAlias> a = getAlias(stationName);

        if (!a.isPresent()) {
            return getOrCreateAliasFor(stationName);
        }

        return a.get();
    }

    private boolean compareAliasAndString(TrainStationAlias alias, String name) {
        return alias.getAliasName().get().equals(name);
    }

    

    // station blacklist
    public boolean isBlacklisted(String stationName) {
        return blacklist.stream().anyMatch(x -> x.contains(stationName));
    }

    public boolean isBlacklisted(TrainStationAlias station) {
        return station.getAllStationNames().stream().anyMatch(x -> isBlacklisted(x));
    }

    public Collection<String> getBlacklist() {
        return blacklist;
    }


    // train blacklist
    public boolean isTrainBlacklisted(Train train) {
        return trainsBlacklist.stream().anyMatch(x -> x.equals(train.name.getString()));
    }

    public boolean isTrainBlacklisted(String trainName) {
        return trainsBlacklist.stream().anyMatch(x -> x.equals(trainName));
    }

    public Collection<String> getTrainBlacklist() {
        return trainsBlacklist;
    }

}
