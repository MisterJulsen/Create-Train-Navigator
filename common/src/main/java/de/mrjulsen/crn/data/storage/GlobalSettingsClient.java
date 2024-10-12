package de.mrjulsen.crn.data.storage;

import java.util.function.Consumer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;

/**
 * Client access for global settings, which are only present on the server side.
 */
public class GlobalSettingsClient {

    public static void getStationTags(Consumer<Collection<StationTag>> result) {
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_STATION_TAGS, result);
    }

    public static void getStationTag(String name, Consumer<StationTag> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.GET_STATION_TAG, result);
    }

    public static void createStationTag(String name, Consumer<StationTag> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.CREATE_STATION_TAG, result);
    }

    public static void registerNewStationTag(StationTag tag, Runnable callback) {
        DataAccessor.getFromServer(tag, ModAccessorTypes.REGISTER_STATION_TAG, x -> callback.run());
    }

    public static void deleteStationTag(UUID tagId, Runnable callback) {
        DataAccessor.getFromServer(tagId, ModAccessorTypes.DELETE_STATION_TAG, x -> callback.run());
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

    public static void deleteTrainGroup(String name, Runnable callback) {
        DataAccessor.getFromServer(name, ModAccessorTypes.DELETE_TRAIN_GROUP, x -> callback.run());
    }
    
    public static record UpdateTrainGroupColorData(String name, int color) {}
    public static void updateTrainGroupColor(String name, int color, Runnable callback) {
        DataAccessor.getFromServer(new UpdateTrainGroupColorData(name, color), ModAccessorTypes.UPDATE_TRAIN_GROUP_COLOR, x -> callback.run());
    }

    public static void createTrainGroup(String name, Consumer<TrainGroup> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.CREATE_TRAIN_GROUP, result);
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

    public static void deleteTrainLine(String lineId, Runnable callback) {
        DataAccessor.getFromServer(lineId, ModAccessorTypes.DELETE_TRAIN_LINE, x -> callback.run());
    }
    public static record UpdateTrainLineColorData(String name, int color) {}
    public static void updateTrainLineColor(String name, int color, Runnable callback) {
        DataAccessor.getFromServer(new UpdateTrainLineColorData(name, color), ModAccessorTypes.UPDATE_TRAIN_LINE_COLOR, x -> callback.run());
    }

    public static void createTrainLine(String name, Consumer<TrainLine> result) {
        DataAccessor.getFromServer(name, ModAccessorTypes.CREATE_TRAIN_LINE, result);
    }
}
