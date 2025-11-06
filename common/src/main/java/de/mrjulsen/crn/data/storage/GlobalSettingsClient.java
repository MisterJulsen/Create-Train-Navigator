package de.mrjulsen.crn.data.storage;

import java.util.function.Consumer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.network.packets.pain.AddStationTagEntryPacketData;
import de.mrjulsen.crn.network.packets.pain.AddStationToBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.AddTrainToBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.CreateStationTagPacketData;
import de.mrjulsen.crn.network.packets.pain.CreateTrainCategoryPacketData;
import de.mrjulsen.crn.network.packets.pain.CreateTrainLinePacketData;
import de.mrjulsen.crn.network.packets.pain.DeleteStationTagPacketData;
import de.mrjulsen.crn.network.packets.pain.DeleteTrainCategoryPacketData;
import de.mrjulsen.crn.network.packets.pain.DeleteTrainLinePacketData;
import de.mrjulsen.crn.network.packets.pain.GetTrainCategoryPacketData;
import de.mrjulsen.crn.network.packets.pain.RegisterStationTagPacketData;
import de.mrjulsen.crn.network.packets.pain.RemoveStationFromBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.RemoveStationTagEntryPacketData;
import de.mrjulsen.crn.network.packets.pain.RemoveTrainFromBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.StationTagRequestPacketData;
import de.mrjulsen.crn.network.packets.pain.StationTagUpdatePermissionsPacketData;
import de.mrjulsen.crn.network.packets.pain.TrainCategoryUpdatePermissionsPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateStationTagEntryPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateStationTagNamePacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainCategoryColorPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainCategoryNamePacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainLineColorPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainLineNamePacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainLinePermissionsPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.client.Minecraft;

/**
 * Client access for global settings, which are only present on the server side.
 */
public class GlobalSettingsClient {

    public static boolean modificationsAllowed() {
        return Minecraft.getInstance().player.hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_PERMISSION_LEVEL.get());
    }

    public static void getStationTags(Consumer<Collection<StationTag>> result) {
        ModNetworkManager.GET_ALL_STATION_TAGS.send(NetworkDirection.toServer(), (response) -> result.accept(response.getTags()), () -> {});
    }

    public static void getStationTag(String name, Consumer<StationTag> result) {
        ModNetworkManager.GET_STATION_TAG.send(NetworkDirection.toServer(), new StationTagRequestPacketData.Request(name), (response) -> result.accept(response.getTag()), () -> {});
    }

    public static void createStationTag(String name, Owner owner, Consumer<Optional<StationTag>> result) {
        ModNetworkManager.CREATE_STATION_TAG.send(NetworkDirection.toServer(), new CreateStationTagPacketData.Request(name, Optional.ofNullable(owner)), (response) -> result.accept(response.getTag()), () -> {});
    }

    public static void registerNewStationTag(StationTag tag, Runnable callback) {
        ModNetworkManager.REGISTER_STATION_TAG.send(NetworkDirection.toServer(), new RegisterStationTagPacketData(tag), (response) -> callback.run(), () -> {});
    }

    public static void deleteStationTag(UUID tagId, Runnable callback) {
        ModNetworkManager.DELETE_STATION_TAG.send(NetworkDirection.toServer(), new DeleteStationTagPacketData(tagId), (response) -> callback.run(), () -> {});
    }

    public static void updateStationTagPermissions(StationTagUpdatePermissionsPacketData.Request data, Consumer<Optional<StationTag>> callback) {
        ModNetworkManager.UPDATE_STATION_TAG_PERMISSIONS.send(NetworkDirection.toServer(), data, (response) -> callback.accept(response.getTag()), () -> {});
    }

    public static void updateStationTagNameData(UUID tagId, String name, Runnable callback) {
        ModNetworkManager.UPDATE_STATION_TAG_NAME.send(NetworkDirection.toServer(), new UpdateStationTagNamePacketData(tagId, name), (response) -> callback.run(), () -> {});
    }

    public static void addStationTagEntry(UUID tagId, String station, StationInfo info, Consumer<Optional<StationTag>> callback) {
        ModNetworkManager.ADD_STATION_TAG_ENTRY.send(NetworkDirection.toServer(), new AddStationTagEntryPacketData.Request(tagId, station, info), (response) -> callback.accept(response.getTag()), () -> {});
    }

    public static void updateStationTagEntry(UUID tagId, String station, StationInfo info, Consumer<Optional<StationTag>> callback) {
        ModNetworkManager.UPDATE_STATION_TAG_ENTRY.send(NetworkDirection.toServer(), new UpdateStationTagEntryPacketData.Request(tagId, station, info), (response) -> callback.accept(response.getTag()), () -> {});
    }

    public static void removeStationTagEntry(UUID tagId, String station, Consumer<Optional<StationTag>> callback) {
        ModNetworkManager.REMOVE_STATION_TAG_ENTRY.send(NetworkDirection.toServer(), new RemoveStationTagEntryPacketData.Request(tagId, station), (response) -> callback.accept(response.getTag()), () -> {});
    }


    
    public static void getTrainCategories(Consumer<List<TrainCategory>> result) {
        ModNetworkManager.GET_ALL_TRAIN_CATEGORIES.send(NetworkDirection.toServer(), (response) -> result.accept(response.getCategories()), () -> {});
    }

    public static void deleteTrainCategory(UUID id, Runnable callback) {
        ModNetworkManager.DELETE_TRAIN_CATEGORY.send(NetworkDirection.toServer(), new DeleteTrainCategoryPacketData(id), (response) -> callback.run(), () -> {});
    }

    public static void getTrainCategory(UUID id, Runnable callback) {
        ModNetworkManager.GET_TRAIN_CATEGORY.send(NetworkDirection.toServer(), new GetTrainCategoryPacketData.Request(id), (response) -> callback.run(), () -> {});
    }
    
    public static void updateTrainCategoryColor(UUID id, DLColor color, Runnable callback) {
        ModNetworkManager.UPDATE_TRAIN_CATEGORY_COLOR.send(NetworkDirection.toServer(), new UpdateTrainCategoryColorPacketData(id, color), (response) -> callback.run(), () -> {});
    }

    public static void createTrainCategory(String name, Consumer<Optional<TrainCategory>> result) {
        ModNetworkManager.CREATE_TRAIN_CATEGORY.send(NetworkDirection.toServer(), new CreateTrainCategoryPacketData.Request(name), (response) -> result.accept(response.getCategory()), () -> {});
    }

    public static void updateTrainCategoryName(UUID id, String name, Consumer<Optional<TrainCategory>> callback) {
        ModNetworkManager.UPDATE_TRAIN_CATEGORY_NAME.send(NetworkDirection.toServer(), new UpdateTrainCategoryNamePacketData.Request(id, name), (response) -> callback.accept(response.getCategory()), () -> {});
    }

    public static void updateTrainCategoryPermissions(TrainCategoryUpdatePermissionsPacketData.Request data, Consumer<Optional<TrainCategory>> callback) {
        ModNetworkManager.UPDATE_TRAIN_CATEGORY_PERMISSIONS.send(NetworkDirection.toServer(), data, (response) -> callback.accept(response.getCategory()), () -> {});
    }

    
    
    public static void getBlacklistedStations(Consumer<List<String>> result) {
        ModNetworkManager.GET_ALL_BLACKLISTED_STATIONS.send(NetworkDirection.toServer(), (response) -> result.accept(response.getNames()), () -> {});
    }

    public static void addStationToBlacklist(String name, Consumer<Collection<String>> result) {
        ModNetworkManager.ADD_STATION_TO_BLACKLIST.send(NetworkDirection.toServer(), new AddStationToBlacklistPacketData.Request(name), (response) -> result.accept(response.getNames()), () -> {});
    }

    public static void removeStationFromBlacklist(String name, Consumer<Collection<String>> result) {
        ModNetworkManager.REMOVE_STATION_FROM_BLACKLIST.send(NetworkDirection.toServer(), new RemoveStationFromBlacklistPacketData.Request(name), (response) -> result.accept(response.getNames()), () -> {});
    }
    


    public static void getBlacklistedTrains(Consumer<List<String>> result) {
        ModNetworkManager.GET_ALL_BLACKLISTED_TRAINS.send(NetworkDirection.toServer(), (response) -> result.accept(response.getNames()), () -> {});
    }

    public static void addTrainToBlacklist(String name, Consumer<Collection<String>> result) {
        ModNetworkManager.ADD_TRAIN_TO_BLACKLIST.send(NetworkDirection.toServer(), new AddTrainToBlacklistPacketData.Request(name), (response) -> result.accept(response.getNames()), () -> {});
    }

    public static void removeTrainFromBlacklist(String name, Consumer<Collection<String>> result) {
        ModNetworkManager.REMOVE_TRAIN_FROM_BLACKLIST.send(NetworkDirection.toServer(), new RemoveTrainFromBlacklistPacketData.Request(name), (response) -> result.accept(response.getNames()), () -> {});
    }


    
    public static void getTrainLines(Consumer<List<TrainLine>> result) {
        ModNetworkManager.GET_ALL_TRAIN_LINES.send(NetworkDirection.toServer(), (response) -> result.accept(response.getLines()), () -> {});
    }

    public static void deleteTrainLine(UUID id, Runnable callback) {
        ModNetworkManager.DELETE_TRAIN_LINE.send(NetworkDirection.toServer(), new DeleteTrainLinePacketData(id), (response) -> callback.run(), () -> {});
    }
    
    public static void updateTrainLineColor(UUID id, DLColor color, Runnable callback) {
        ModNetworkManager.UPDATE_TRAIN_LINE_COLOR.send(NetworkDirection.toServer(), new UpdateTrainLineColorPacketData(id, color), (response) -> callback.run(), () -> {});
    }

    public static void createTrainLine(String name, Consumer<Optional<TrainLine>> result) {
        ModNetworkManager.CREATE_TRAIN_LINE.send(NetworkDirection.toServer(), new CreateTrainLinePacketData.Request(name), (response) -> result.accept(response.getLine()), () -> {});
    }

    public static void updateTrainLinePermissions(UpdateTrainLinePermissionsPacketData.Request data, Consumer<Optional<TrainLine>> callback) {
        ModNetworkManager.UPDATE_TRAIN_LINE_PERMISSIONS.send(NetworkDirection.toServer(), data, (response) -> callback.accept(response.getLine()), () -> {});
    }

    public static void updateTrainLineName(UUID id, String name, Consumer<Optional<TrainLine>> callback) {
        ModNetworkManager.UPDATE_TRAIN_LINE_NAME.send(NetworkDirection.toServer(), new UpdateTrainLineNamePacketData.Request(id, name), (response) -> callback.accept(response.getLine()), () -> {});
    }
}
