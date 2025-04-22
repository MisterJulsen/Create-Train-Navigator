package de.mrjulsen.crn.data.storage;

import java.util.function.Consumer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.crn.util.Lock.PermissionsUpdateData;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.client.Minecraft;

/**
 * Client access for global settings, which are only present on the server side.
 */
public class GlobalSettingsClient {

    public static boolean modificationsAllowed() {
        return Minecraft.getInstance().player.hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_PERMISSION_LEVEL.get());
    }

    public static void getStationTags(Consumer<Collection<StationTag>> result) {
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_STATION_TAGS, result);
    }

    public static void getStationTag(String name, Consumer<StationTag> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.GET_STATION_TAG, result);
    }

    public static record CreateStationTagData(String name, Owner owner) {}
    public static void createStationTag(String name, Owner owner, Consumer<Optional<StationTag>> result) {
        DataAccessor.getFromServer(new CreateStationTagData(name, owner), ModAccessorTypes.CREATE_STATION_TAG, result);
    }

    public static void registerNewStationTag(StationTag tag, Runnable callback) {
        DataAccessor.getFromServer(tag, ModAccessorTypes.REGISTER_STATION_TAG, x -> callback.run());
    }

    public static void deleteStationTag(UUID tagId, Runnable callback) {
        DataAccessor.getFromServer(tagId, ModAccessorTypes.DELETE_STATION_TAG, x -> callback.run());
    }

    public static void updateStationTagPermissions(PermissionsUpdateData data, Consumer<Optional<StationTag>> callback) {
        DataAccessor.getFromServer(data, ModAccessorTypes.UPDATE_STATION_TAG_PERMISSIONS, x -> callback.accept(x));
    }

    public static record UpdateStationTagNameData(UUID tagId, String name) {}
    public static void updateStationTagNameData(UUID tagId, String name, Runnable callback) {
        DataAccessor.getFromServer(new UpdateStationTagNameData(tagId, name), ModAccessorTypes.UPDATE_STATION_TAG_NAME, x -> callback.run());
    }

    public static record AddStationTagEntryData(UUID tagId, String station, StationInfo info) {}
    public static void addStationTagEntry(UUID tagId, String station, StationInfo info, Consumer<Optional<StationTag>> callback) {
        DataAccessor.getFromServer(new AddStationTagEntryData(tagId, station, info), ModAccessorTypes.ADD_STATION_TAG_ENTRY, callback);
    }

    public static void updateStationTagEntry(UUID tagId, String station, StationInfo info, Consumer<Optional<StationTag>> callback) {
        DataAccessor.getFromServer(new AddStationTagEntryData(tagId, station, info), ModAccessorTypes.UPDATE_STATION_TAG_ENTRY, callback);
    }

    public static record RemoveStationTagEntryData(UUID tagId, String station) {}
    public static void removeStationTagEntry(UUID tagId, String station, Consumer<Optional<StationTag>> callback) {
        DataAccessor.getFromServer(new RemoveStationTagEntryData(tagId, station), ModAccessorTypes.REMOVE_STATION_TAG_ENTRY, callback);
    }


    
    public static void getTrainGroups(Consumer<List<TrainGroup>> result) {
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_TRAIN_GROUPS, result);
    }

    public static void deleteTrainGroup(UUID id, Runnable callback) {
        DataAccessor.getFromServer(id, ModAccessorTypes.DELETE_TRAIN_GROUP, x -> callback.run());
    }

    public static void getTrainGroup(UUID id, Runnable callback) {
        DataAccessor.getFromServer(id, ModAccessorTypes.DELETE_TRAIN_GROUP, x -> callback.run());
    }
    
    public static record UpdateTrainGroupColorData(UUID id, int color) {}
    public static void updateTrainGroupColor(UUID id, int color, Runnable callback) {
        DataAccessor.getFromServer(new UpdateTrainGroupColorData(id, color), ModAccessorTypes.UPDATE_TRAIN_GROUP_COLOR, x -> callback.run());
    }

    public static void createTrainGroup(String name, Consumer<Optional<TrainGroup>> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.CREATE_TRAIN_GROUP, result);
    }

    public static record UpdateTrainGroupNameData(UUID id, String name) {}
    public static void updateTrainGroupName(UUID id, String name, Consumer<Optional<TrainGroup>> callback) {
        DataAccessor.getFromServer(new UpdateTrainGroupNameData(id, name), ModAccessorTypes.UPDATE_TRAIN_GROUP_NAME, x -> callback.accept(x));
    }

    public static void updateTrainGroupPermissions(PermissionsUpdateData data, Consumer<Optional<TrainGroup>> callback) {
        DataAccessor.getFromServer(data, ModAccessorTypes.UPDATE_TRAIN_GROUP_PERMISSIONS, x -> callback.accept(x));
    }

    
    
    public static void getBlacklistedStations(Consumer<List<String>> result) {
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_BLACKLISTED_STATIONS, result);
    }

    public static void addStationToBlacklist(String name, Consumer<Collection<String>> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.ADD_STATION_TO_BLACKLIST, result);
    }

    public static void removeStationFromBlacklist(String name, Consumer<Collection<String>> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.REMOVE_STATION_FROM_BLACKLIST, result);
    }
    


    public static void getBlacklistedTrains(Consumer<List<String>> result) {
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_BLACKLISTED_TRAINS, result);
    }

    public static void addTrainToBlacklist(String name, Consumer<Collection<String>> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.ADD_TRAIN_TO_BLACKLIST, result);
    }

    public static void removeTrainFromBlacklist(String name, Consumer<Collection<String>> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.REMOVE_TRAIN_FROM_BLACKLIST, result);
    }


    
    public static void getTrainLines(Consumer<List<TrainLine>> result) {
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_TRAIN_LINES, result);
    }

    public static void deleteTrainLine(UUID id, Runnable callback) {
        DataAccessor.getFromServer(id, ModAccessorTypes.DELETE_TRAIN_LINE, x -> callback.run());
    }
    
    public static record UpdateTrainLineColorData(UUID id, int color) {}
    public static void updateTrainLineColor(UUID id, int color, Runnable callback) {
        DataAccessor.getFromServer(new UpdateTrainLineColorData(id, color), ModAccessorTypes.UPDATE_TRAIN_LINE_COLOR, x -> callback.run());
    }

    public static void createTrainLine(String name, Consumer<Optional<TrainLine>> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.CREATE_TRAIN_LINE, result);
    }

    public static void updateTrainLinePermissions(PermissionsUpdateData data, Consumer<Optional<TrainLine>> callback) {
        DataAccessor.getFromServer(data, ModAccessorTypes.UPDATE_TRAIN_LINE_PERMISSIONS, x -> callback.accept(x));
    }

    public static record UpdateTrainLineNameData(UUID id, String name) {}
    public static void updateTrainLineName(UUID id, String name, Consumer<Optional<TrainLine>> callback) {
        DataAccessor.getFromServer(new UpdateTrainLineNameData(id, name), ModAccessorTypes.UPDATE_TRAIN_LINE_NAME, x -> callback.accept(x));
    }
}
