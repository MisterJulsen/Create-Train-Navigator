package de.mrjulsen.crn.registry;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.simibubi.create.content.trains.entity.Train;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.ArrayList;
import java.util.Optional;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient.AddStationTagEntryData;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient.CreateStationTagData;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient.RemoveStationTagEntryData;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient.UpdateStationTagNameData;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient.UpdateTrainCategoryColorData;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient.UpdateTrainCategoryNameData;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient.UpdateTrainLineColorData;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient.UpdateTrainLineNameData;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.train.DepartureHistory;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.ScheduleSection;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.data.train.ClientTrainStop.TrainStopRealTimeData;
import de.mrjulsen.crn.data.train.portable.NextConnectionsDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainDisplayData;
import de.mrjulsen.crn.debug.TrainDebugData;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.NavigatableGraph;
import de.mrjulsen.crn.data.navigation.Route;
import de.mrjulsen.crn.data.navigation.RoutePart;
import de.mrjulsen.crn.data.navigation.ClientRoutePart.TrainRealTimeData;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Single.MutableSingle;
import de.mrjulsen.mcdragonlib.util.accessor.BasicDataAccessorPacket.IChunkReceiver;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessorType;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.crn.registry.data.NextConnectionsRequestData;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.crn.util.Lock.PermissionsUpdateData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ModAccessorTypes {

//#region STATION TAGS

    public static final DataAccessorType<String, StationTag, StationTag> GET_STATION_TAG = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_station_tag"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putString(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getString(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, GlobalSettings.getInstance().getOrCreateStationTagFor(in).toNbt());
            return false;
        }, (hasMore, previousData, iteration, nbt) -> {
            return StationTag.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), null);
        }
    ));

    public static final DataAccessorType<TagName, StationTag, StationTag> GET_STATION_TAG_BY_TAG_NAME = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_station_tag_by_tag_name"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putString(DataAccessorType.DEFAULT_NBT_DATA, in.get());
        }, (nbt) -> {
            return TagName.of(nbt.getString(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, GlobalSettings.getInstance().getOrCreateStationTagFor(in).toNbt());
            return false;
        }, (hasMore, previousData, iteration, nbt) -> {
            return StationTag.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), null);
        }
    ));

    public static final DataAccessorType<CreateStationTagData, Optional<StationTag>, Optional<StationTag>> CREATE_STATION_TAG = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "create_station_tag"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putString("Name", in.name());
            if (in.owner() != null) nbt.put("Owner", in.owner().toNbt());
        }, (nbt) -> {
            return new CreateStationTagData(nbt.getString("Name"), nbt.contains("Owner") ? Owner.fromNbt(nbt.getCompound("Owner")) : null);
        }, (player, in, temp, nbt, iteration) -> {
            if (!GlobalSettings.modificationsAllowed(player)) {
                return false;
            }
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, GlobalSettings.getInstance().createOrGetStationTag(TagName.of(in.name()), in.owner()).toNbt());
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? StationTag.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), null) : null);
        }
    ));

    public static final DataAccessorType<StationTag, Void, Void> REGISTER_STATION_TAG = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "register_station_tag"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return StationTag.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), null);
        }, (player, in, temp, nbt, iteration) -> {
            if (!GlobalSettings.modificationsAllowed(player)) {
                return false;
            }
            in.updateLastEdited(player);
            GlobalSettings.getInstance().registerStationTag(in);
            return false;
        }
    ));

    public static final DataAccessorType<UUID, Void, Void> DELETE_STATION_TAG = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "delete_station_tag"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getStationTag(in).ifPresent(x -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                GlobalSettings.getInstance().removeStationTag(in);
            });
            return false;
        }
    ));

    public static final DataAccessorType<UpdateStationTagNameData, Void, Void> UPDATE_STATION_TAG_NAME = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_station_tag_name"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.putUUID("Id", in.tagId());
            nbt.putString("Name", in.name());
        }, (nbt) -> {
            return new UpdateStationTagNameData(nbt.getUUID("Id"), nbt.getString("Name"));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getStationTag(in.tagId()).ifPresent(x -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                x.updateLastEdited(player);
                x.setName(TagName.of(in.name()));
            });
            return false;
        }
    ));

    public static final DataAccessorType<PermissionsUpdateData, Optional<StationTag>, Optional<StationTag>> UPDATE_STATION_TAG_PERMISSIONS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_station_tag_permissions"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return PermissionsUpdateData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getStationTag(in.id()).ifPresent((tag) -> {
                if (!tag.getOwner().isAdmin(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                if (in.state() != null) tag.getOwner().set(in.state());
                if (in.trusted() != null) tag.getOwner().updateTrusted(in.trusted());
                if (in.newOwner() != null) {
                    tag.getOwner().setOwner(in.newOwner());
                    tag.getOwner().addTrusted(new Owner(player));
                }
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, tag.toNbt());
            });
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? StationTag.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), null) : null);
        }
    ));

    public static final DataAccessorType<AddStationTagEntryData, Optional<StationTag>, Optional<StationTag>> ADD_STATION_TAG_ENTRY = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "add_station_tag_entry"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID("Id", in.tagId());
            nbt.putString("Name", in.station());
            nbt.put("Info", in.info().toNbt());
        }, (nbt) -> {
            return new AddStationTagEntryData(nbt.getUUID("Id"), nbt.getString("Name"), StationInfo.fromNbt(nbt.getCompound("Info")));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getStationTag(in.tagId()).ifPresent(x -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                x.add(in.station(), in.info());
                x.updateLastEdited(player);
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, x.toNbt());
            });
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? StationTag.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), null) : null);
        }
    )); 

    public static final DataAccessorType<AddStationTagEntryData, Optional<StationTag>, Optional<StationTag>> UPDATE_STATION_TAG_ENTRY = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_station_tag_entry"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID("Id", in.tagId());
            nbt.putString("Name", in.station());
            nbt.put("Info", in.info().toNbt());
        }, (nbt) -> {
            return new AddStationTagEntryData(nbt.getUUID("Id"), nbt.getString("Name"), StationInfo.fromNbt(nbt.getCompound("Info")));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getStationTag(in.tagId()).ifPresent(x -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                x.updateInfoForStation(in.station(), in.info());
                x.updateLastEdited(player);
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, x.toNbt());
            });
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? StationTag.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), null) : null);
        }
    ));    

    public static final DataAccessorType<RemoveStationTagEntryData, Optional<StationTag>, Optional<StationTag>> REMOVE_STATION_TAG_ENTRY = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "remove_station_tag_entry"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID("Id", in.tagId());
            nbt.putString("Name", in.station());
        }, (nbt) -> {
            return new RemoveStationTagEntryData(nbt.getUUID("Id"), nbt.getString("Name"));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getStationTag(in.tagId()).ifPresent(x -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                x.remove(in.station());
                x.updateLastEdited(player);
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, x.toNbt());
            });
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? StationTag.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), null) : null);
        }
    ));
    
    public static final DataAccessorType<Void, Collection<StationTag>, Collection<StationTag>> GET_ALL_STATION_TAGS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_all_station_tags"), DataAccessorType.Builder.createNoInputChunked(
        (player, in, temp, nbt, iteration) -> {
            if (temp.getFirst() == null) {
                temp.setFirst(new ConcurrentLinkedQueue<>(GlobalSettings.getInstance().getAllStationTags().stream().sorted((a, b) -> a.getTagName().get().compareToIgnoreCase(b.getTagName().get())).toList()));
            }
            @SuppressWarnings("unchecked")
            Queue<StationTag> tags = (Queue<StationTag>)((MutableSingle<Object>)temp).getFirst();
            if (tags.isEmpty()) {
                return false;
            }
            for (int i = 0; i < 64 && !tags.isEmpty(); i++) {
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll().toNbt());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<Collection<StationTag>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<StationTag>();
            }
            final Collection<StationTag> l = list;
            nbt.getAllKeys().forEach(x -> l.add(StationTag.fromNbt(nbt.getCompound(x), null)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));

    /** Input: true = exclude blacklisted */
    public static final DataAccessorType<Boolean, Collection<StationTag>, Collection<StationTag>> GET_ALL_STATIONS_AS_TAGS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_all_stations_as_tags"), DataAccessorType.Builder.createChunked(
        (in, nbt) -> {
            nbt.putBoolean(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getBoolean(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            if (temp.getFirst() == null) {
                temp.setFirst(new ConcurrentLinkedQueue<>(TrainUtils.getAllStations().stream().filter(x -> !in || !GlobalSettings.getInstance().isStationBlacklisted(x)).map(x -> GlobalSettings.getInstance().getOrCreateStationTagFor(x)).distinct().sorted((a, b) -> a.getTagName().get().compareToIgnoreCase(b.getTagName().get())).toList()));
            }
            @SuppressWarnings("unchecked")
            Queue<StationTag> tags = (Queue<StationTag>)((MutableSingle<Object>)temp).getFirst();
            if (tags.isEmpty()) {
                return false;
            }
            for (int i = 0; i < 64 && !tags.isEmpty(); i++) {
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll().toNbt());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<Collection<StationTag>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<StationTag>();
            }
            final Collection<StationTag> l = list;
            nbt.getAllKeys().forEach(x -> l.add(StationTag.fromNbt(nbt.getCompound(x), null)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));

//#endregion
//#region TRAIN CATEGORIES

    public static final DataAccessorType<Void, List<TrainCategory>, List<TrainCategory>> GET_ALL_TRAIN_CATEGORIES = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_all_train_categories"), DataAccessorType.Builder.createNoInputChunked(
        (player, in, temp, nbt, iteration) -> {
            if (temp.getFirst() == null) {
                temp.setFirst(new ConcurrentLinkedQueue<>(GlobalSettings.getInstance().getAllTrainCategories()));
            }
            @SuppressWarnings("unchecked")
            Queue<TrainCategory> tags = (Queue<TrainCategory>)((MutableSingle<Object>)temp).getFirst();
            if (tags.isEmpty()) {
                return false;
            }
            for (int i = 0; i < 64 && !tags.isEmpty(); i++) {
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll().toNbt());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<List<TrainCategory>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<TrainCategory>();
            }
            final List<TrainCategory> l = list;
            nbt.getAllKeys().forEach(x -> l.add(TrainCategory.fromNbt(nbt.getCompound(x))));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));

    public static final DataAccessorType<UUID, Void, Void> DELETE_TRAIN_CATEGORY = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "delete_train_category"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getTrainCategory(in).ifPresent(x -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                GlobalSettings.getInstance().removeTrainCategory(in);
            });
            return false;
        }
    ));

    public static final DataAccessorType<UUID, Optional<TrainCategory>, Optional<TrainCategory>> GET_TRAIN_CATEGORY = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_train_category"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getTrainCategory(in).ifPresent(x -> nbt.put(DataAccessorType.DEFAULT_NBT_DATA, x.toNbt()));
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? TrainCategory.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)) : null);
        }
    ));

    public static final DataAccessorType<UpdateTrainCategoryColorData, Void, Void> UPDATE_TRAIN_CATEGORY_COLOR = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_train_category_color"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.putUUID("Id", in.id());
            nbt.putInt("Color", in.color());
        }, (nbt) -> {
            return new UpdateTrainCategoryColorData(nbt.getUUID("Id"), nbt.getInt("Color"));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getTrainCategory(in.id()).ifPresent(x -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                x.setColor(in.color());
            });
            return false;
        }
    ));

    public static final DataAccessorType<UpdateTrainCategoryNameData, Optional<TrainCategory>, Optional<TrainCategory>> UPDATE_TRAIN_CATEGORY_NAME = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_train_category_name"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID("Id", in.id());
            nbt.putString("Name", in.name());
        }, (nbt) -> {
            return new UpdateTrainCategoryNameData(nbt.getUUID("Id"), nbt.getString("Name"));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getTrainCategory(in.id()).ifPresent((x) -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                x.setName(in.name());
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, x.toNbt());
            });
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? TrainCategory.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)) : null);
        }
    )); 

    public static final DataAccessorType<String, Optional<TrainCategory>, Optional<TrainCategory>> CREATE_TRAIN_CATEGORY = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "create_train_category"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putString(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getString(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            if (!GlobalSettings.modificationsAllowed(player)) {
                return false;
            }
            TrainCategory category = GlobalSettings.getInstance().createOrGetTrainCategory(in, new Owner(player));
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, category.toNbt());
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? TrainCategory.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)) : null);
        }
    ));

    public static final DataAccessorType<PermissionsUpdateData, Optional<TrainCategory>, Optional<TrainCategory>> UPDATE_TRAIN_CATEGORY_PERMISSIONS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_train_category_permissions"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return PermissionsUpdateData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getTrainCategory(in.id()).ifPresent((tag) -> {
                if (!tag.getOwner().isAdmin(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                if (in.state() != null) tag.getOwner().set(in.state());
                if (in.trusted() != null) tag.getOwner().updateTrusted(in.trusted());
                if (in.newOwner() != null) {
                    tag.getOwner().setOwner(in.newOwner());
                    tag.getOwner().addTrusted(new Owner(player));
                }
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, tag.toNbt());
            });
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? TrainCategory.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)) : null);
        }
    ));
    
//#endregion
//#region STATION BLACKLIST

    public static final DataAccessorType<String, Collection<String>, Collection<String>> ADD_STATION_TO_BLACKLIST = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "add_station_to_blacklist"), DataAccessorType.Builder.createChunked(
        (in, nbt) -> {
            nbt.putString(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getString(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            if (!GlobalSettings.modificationsAllowed(player)) {
                return false;
            }
            if (temp.getFirst() == null) {
                GlobalSettings.getInstance().blacklistStation(in);
                temp.setFirst(new ConcurrentLinkedQueue<>(GlobalSettings.getInstance().getAllBlacklistedStations()));
            }
            @SuppressWarnings("unchecked")
            Queue<String> tags = (Queue<String>)((MutableSingle<Object>)temp).getFirst();
            for (int i = 0; i < 256 && !tags.isEmpty(); i++) {
                nbt.putString(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<Collection<String>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<String>();
            }
            final Collection<String> l = list;
            nbt.getAllKeys().forEach(x -> l.add(nbt.getString(x)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));

    public static final DataAccessorType<String, Collection<String>, Collection<String>> REMOVE_STATION_FROM_BLACKLIST = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "remove_station_from_blacklist"), DataAccessorType.Builder.createChunked(
        (in, nbt) -> {
            nbt.putString(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getString(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            if (!GlobalSettings.modificationsAllowed(player)) {
                return false;
            }
            if (temp.getFirst() == null) {
                GlobalSettings.getInstance().removeStationFromBlacklist(in);
                temp.setFirst(new ConcurrentLinkedQueue<>(GlobalSettings.getInstance().getAllBlacklistedStations()));
            }
            @SuppressWarnings("unchecked")
            Queue<String> tags = (Queue<String>)((MutableSingle<Object>)temp).getFirst();
            for (int i = 0; i < 256 && !tags.isEmpty(); i++) {
                nbt.putString(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<Collection<String>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<String>();
            }
            final Collection<String> l = list;
            nbt.getAllKeys().forEach(x -> l.add(nbt.getString(x)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));
    
    public static final DataAccessorType<Void, List<String>, List<String>> GET_BLACKLISTED_STATIONS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_blacklisted_stations"), DataAccessorType.Builder.createNoInputChunked(
        (player, in, temp, nbt, iteration) -> {
            if (temp.getFirst() == null) {
                temp.setFirst(new ConcurrentLinkedQueue<>(GlobalSettings.getInstance().getAllBlacklistedStations()));
            }
            @SuppressWarnings("unchecked")
            Queue<String> tags = (Queue<String>)((MutableSingle<Object>)temp).getFirst();
            for (int i = 0; i < 256 && !tags.isEmpty(); i++) {
                nbt.putString(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<List<String>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<String>();
            }
            final List<String> l = list;
            nbt.getAllKeys().forEach(x -> l.add(nbt.getString(x)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));

//#endregion
//#region TRAIN BLACKLIST

    public static final DataAccessorType<String, Collection<String>, Collection<String>> ADD_TRAIN_TO_BLACKLIST = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "add_train_to_blacklist"), DataAccessorType.Builder.createChunked(
        (in, nbt) -> {
            nbt.putString(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getString(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            if (!GlobalSettings.modificationsAllowed(player)) {
                return false;
            }
            if (temp.getFirst() == null) {
                GlobalSettings.getInstance().blacklistTrain(in);
                temp.setFirst(new ConcurrentLinkedQueue<>(GlobalSettings.getInstance().getAllBlacklistedTrains()));
            }
            @SuppressWarnings("unchecked")
            Queue<String> tags = (Queue<String>)((MutableSingle<Object>)temp).getFirst();
            for (int i = 0; i < 256 && !tags.isEmpty(); i++) {
                nbt.putString(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<Collection<String>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<String>();
            }
            final Collection<String> l = list;
            nbt.getAllKeys().forEach(x -> l.add(nbt.getString(x)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));

    public static final DataAccessorType<String, Collection<String>, Collection<String>> REMOVE_TRAIN_FROM_BLACKLIST = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "remove_train_from_blacklist"), DataAccessorType.Builder.createChunked(
        (in, nbt) -> {
            nbt.putString(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getString(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            if (!GlobalSettings.modificationsAllowed(player)) {
                return false;
            }
            if (temp.getFirst() == null) {
                GlobalSettings.getInstance().removeTrainFromBlacklist(in);
                temp.setFirst(new ConcurrentLinkedQueue<>(GlobalSettings.getInstance().getAllBlacklistedTrains()));
            }
            @SuppressWarnings("unchecked")
            Queue<String> tags = (Queue<String>)((MutableSingle<Object>)temp).getFirst();
            for (int i = 0; i < 256 && !tags.isEmpty(); i++) {
                nbt.putString(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<Collection<String>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<String>();
            }
            final Collection<String> l = list;
            nbt.getAllKeys().forEach(x -> l.add(nbt.getString(x)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));
    
    public static final DataAccessorType<Void, List<String>, List<String>> GET_BLACKLISTED_TRAINS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_blacklisted_trains"), DataAccessorType.Builder.createNoInputChunked(
        (player, in, temp, nbt, iteration) -> {
            if (temp.getFirst() == null) {
                temp.setFirst(new ConcurrentLinkedQueue<>(GlobalSettings.getInstance().getAllBlacklistedTrains()));
            }
            @SuppressWarnings("unchecked")
            Queue<String> tags = (Queue<String>)((MutableSingle<Object>)temp).getFirst();
            for (int i = 0; i < 256 && !tags.isEmpty(); i++) {
                nbt.putString(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<List<String>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<String>();
            }
            final List<String> l = list;
            nbt.getAllKeys().forEach(x -> l.add(nbt.getString(x)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));



    public static final DataAccessorType<UUID, TrainRealTimeData, TrainRealTimeData> UPDATE_REALTIME = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_realtime"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            TrainListener.getTrainData(in).ifPresent(data -> {
                List<TrainPrediction> predictions = data.getPredictions();
                Map<Integer, TrainStopRealTimeData> values = new HashMap<>();
                for (TrainPrediction prediction : predictions) {
                    TrainStopRealTimeData realTimeData = new TrainStopRealTimeData(
                        prediction.getStationTag().getClientTag(prediction.getTargetedStationName()),
                        prediction.getEntryIndex(),
                        prediction.scheduled().arrivalTime(),
                        prediction.scheduled().departureTime(),
                        prediction.realTime().arrivalTime(),
                        prediction.realTime().departureTime(),
                        (int)prediction.realTime().arrivalIn(),
                        prediction.getCurrentCycle()
                    );
                    values.put(realTimeData.entryIndex(), realTimeData);
                }
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, TrainRealTimeData.createServer(data.getSessionId(), values, data.getStatus(), data.isCancelled()).toNbt());
            });
            return false;
        }, (hasMore, previousData, iteration, nbt) -> {
            return nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? TrainRealTimeData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)) : null;
        }
    ));



    public static final DataAccessorType<UUID, UserSettings, UserSettings> GET_USER_SETTINGS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_user_settings"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, UserSettings.getSettingsFor(in, false).toNbt());
            nbt.putUUID("Id", in);
            return false;
        }, (hasMore, list, iteration, nbt) -> {
            return UserSettings.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), nbt.getUUID("Id"), false);
        }
    ));

    public static final DataAccessorType<UserSettings, Void, Void> SAVE_USER_SETTINGS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "save_user_settings"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
            nbt.putUUID("Id", in.getOwnerId());
        }, (nbt) -> {
            return UserSettings.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), nbt.getUUID("Id"), false);
        }, (player, in, temp, nbt, iteration) -> {
            in.save();
            return false;
        }
    ));

    public static final DataAccessorType<BlockPos, NearestTrackStationResult, NearestTrackStationResult> GET_NEAREST_STATION = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_nearest_station"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putInt("x", in.getX());
            nbt.putInt("y", in.getY());
            nbt.putInt("z", in.getZ());
        }, (nbt) -> {
            return new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
        }, (player, in, temp, nbt, iteration) -> {  
            NearestTrackStationResult result = NearestTrackStationResult.empty();
            try {
                result = TrainUtils.getNearestTrackStation(player.level(), in);                    
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("Error while trying to find nearest track station.", e);
                CreateRailwaysNavigator.net().CHANNEL.sendToPlayer((ServerPlayer)player, new ServerErrorPacket(e.getMessage()));
            }
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, result.toNbt());
            return false;
        }, (hasMore, list, iteration, nbt) -> {
            return NearestTrackStationResult.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }
    ));

    public static final DataAccessorType<UUID, TrainDisplayData, TrainDisplayData> GET_TRAIN_DISPLAY_DATA_FROM_SERVER = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_train_display_data"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            Optional<Train> trainOpt = TrainUtils.getTrain(in);
            if (!trainOpt.isPresent() || !TrainUtils.isTrainUsable(trainOpt.get()) || GlobalSettings.getInstance().isTrainBlacklisted(trainOpt.get())) {
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, TrainDisplayData.empty().toNbt());
                return false;
            }
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, TrainDisplayData.of(trainOpt.get()).toNbt());
            return false;
        }, (hasMore, list, iteration, nbt) -> {
            return TrainDisplayData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }
    ));
    
    public static final DataAccessorType<NextConnectionsRequestData, NextConnectionsDisplayData, NextConnectionsDisplayData> GET_NEXT_CONNECTIONS_DISPLAY_DATA = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_next_connections_display_data"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return NextConnectionsRequestData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, NextConnectionsDisplayData.at(in.stationName(), in.selfTrainId()).toNbt());
            return false;
        }, (hasMore, list, iteration, nbt) -> {
            return NextConnectionsDisplayData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }
    ));

    
    public static final DataAccessorType<Void, Collection<String>, Collection<String>> GET_ALL_TRAIN_NAMES = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_all_train_names"), DataAccessorType.Builder.createNoInputChunked(
        (player, in, temp, nbt, iteration) -> {
            if (temp.getFirst() == null) {
                temp.setFirst(new ConcurrentLinkedQueue<>(TrainUtils.getTrains(false).stream().map(x -> x.name.getString()).toList()));
            }
            @SuppressWarnings("unchecked")
            Queue<String> tags = (Queue<String>)((MutableSingle<Object>)temp).getFirst();
            for (int i = 0; i < 256 && !tags.isEmpty(); i++) {
                nbt.putString(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<Collection<String>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<String>();
            }
            final Collection<String> l = list;
            nbt.getAllKeys().forEach(x -> l.add(nbt.getString(x)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));

    public static final DataAccessorType<Void, Collection<String>, Collection<String>> GET_ALL_STATION_NAMES = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_all_station_names"), DataAccessorType.Builder.createNoInputChunked(
        (player, in, temp, nbt, iteration) -> {
            if (temp.getFirst() == null) {
                temp.setFirst(new ConcurrentLinkedQueue<>(TrainUtils.getAllStations().stream().map(x -> x.name).toList()));
            }
            @SuppressWarnings("unchecked")
            Queue<String> tags = (Queue<String>)((MutableSingle<Object>)temp).getFirst();
            for (int i = 0; i < 256 && !tags.isEmpty(); i++) {
                nbt.putString(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<Collection<String>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<String>();
            }
            final Collection<String> l = list;
            nbt.getAllKeys().forEach(x -> l.add(nbt.getString(x)));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));

    //#region TRAIN LINES

    public static final DataAccessorType<Void, List<TrainLine>, List<TrainLine>> GET_ALL_TRAIN_LINES = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_all_train_lines"), DataAccessorType.Builder.createNoInputChunked(
        (player, in, temp, nbt, iteration) -> {
            if (temp.getFirst() == null) {
                temp.setFirst(new ConcurrentLinkedQueue<>(GlobalSettings.getInstance().getAllTrainLines()));
            }
            @SuppressWarnings("unchecked")
            Queue<TrainLine> tags = (Queue<TrainLine>)((MutableSingle<Object>)temp).getFirst();
            if (tags.isEmpty()) {
                return false;
            }
            
            for (int i = 0; i < 256 && !tags.isEmpty(); i++) {
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA + i, tags.poll().toNbt());
            }
            return !tags.isEmpty();
        }, (IChunkReceiver<List<TrainLine>>)(hasMore, list, iteration, nbt) -> {
            if (list == null) {
                list = new ArrayList<TrainLine>();
            }
            final List<TrainLine> l = list;
            nbt.getAllKeys().forEach(x -> l.add(TrainLine.fromNbt(nbt.getCompound(x))));
            return l;
        }, (chunks) -> {
            return chunks;
        }
    ));

    public static final DataAccessorType<UUID, Void, Void> DELETE_TRAIN_LINE = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "delete_train_line"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {            
            GlobalSettings.getInstance().getTrainLine(in).ifPresent(x -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                GlobalSettings.getInstance().removeTrainLine(in);
            });
            return false;
        }
    ));
    

    public static final DataAccessorType<UUID, Optional<TrainLine>, Optional<TrainLine>> GET_TRAIN_LINE = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_train_line"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getTrainLine(in).ifPresent(x -> nbt.put(DataAccessorType.DEFAULT_NBT_DATA, x.toNbt()));
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? TrainLine.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)) : null);
        }
    ));

    public static final DataAccessorType<UpdateTrainLineColorData, Void, Void> UPDATE_TRAIN_LINE_COLOR = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_train_line_color"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.putUUID("Id", in.id());
            nbt.putInt("Color", in.color());
        }, (nbt) -> {
            return new UpdateTrainLineColorData(nbt.getUUID("Id"), nbt.getInt("Color"));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getTrainLine(in.id()).ifPresent(x -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                x.setColor(in.color());
            });
            return false;
        }
    )); 

    public static final DataAccessorType<UpdateTrainLineNameData, Optional<TrainLine>, Optional<TrainLine>> UPDATE_TRAIN_LINE_NAME = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_train_line_name"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID("Id", in.id());
            nbt.putString("Name", in.name());
        }, (nbt) -> {
            return new UpdateTrainLineNameData(nbt.getUUID("Id"), nbt.getString("Name"));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getTrainLine(in.id()).ifPresent((x) -> {
                if (!x.getOwner().isAllowed(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                x.setName(in.name());
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, x.toNbt());
            });
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? TrainLine.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)) : null);
        }
    )); 

    public static final DataAccessorType<String, Optional<TrainLine>, Optional<TrainLine>> CREATE_TRAIN_LINE = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "create_train_line"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putString(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getString(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            if (!GlobalSettings.modificationsAllowed(player)) {
                return false;
            }
            TrainLine line = GlobalSettings.getInstance().createOrGetTrainLine(in, new Owner(player));
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, line.toNbt());
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA)? TrainLine.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)) : null);
        }
    ));    

    public static final DataAccessorType<PermissionsUpdateData, Optional<TrainLine>, Optional<TrainLine>> UPDATE_TRAIN_LINE_PERMISSIONS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "update_train_line_permissions"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return PermissionsUpdateData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            GlobalSettings.getInstance().getTrainLine(in.id()).ifPresent((tag) -> {
                if (!tag.getOwner().isAdmin(new Owner(player)) || !GlobalSettings.modificationsAllowed(player)) {
                    return;
                }
                if (in.state() != null) tag.getOwner().set(in.state());
                if (in.trusted() != null) tag.getOwner().updateTrusted(in.trusted());
                if (in.newOwner() != null) {
                    tag.getOwner().setOwner(in.newOwner());
                    tag.getOwner().addTrusted(new Owner(player));
                }
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, tag.toNbt());
            });
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return Optional.ofNullable(nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? TrainLine.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)) : null);
        }
    ));

    //#endregion

    public static record NavigationData(String start, String end, UUID player) {}
    public static final DataAccessorType<NavigationData, List<ClientRoute>, List<ClientRoute>> NAVIGATE = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "navigate"), DataAccessorType.Builder.createChunked(
        (in, nbt) -> {
            nbt.putString("Start", in.start());
            nbt.putString("End", in.end());
            nbt.putUUID("Player", in.player());
        }, (nbt) -> {
            return new NavigationData(nbt.getString("Start"), nbt.getString("End"), nbt.getUUID("Player"));
        }, (player, in, temp, nbt, iteration) -> {
            try {
                if (temp.getFirst() == null) {
                    GlobalSettings settings = GlobalSettings.getInstance();
                    List<Route> routes = NavigatableGraph.searchRoutes(
                        settings.getTagByName(TagName.of(in.start())).orElse(settings.getOrCreateStationTagFor(in.start())),
                        settings.getTagByName(TagName.of(in.end())).orElse(settings.getOrCreateStationTagFor(in.end())),
                        in.player(),
                        true
                    );
                    temp.setFirst(new ConcurrentLinkedQueue<>(routes));
                }
                @SuppressWarnings("unchecked")
                Queue<Route> tags = (Queue<Route>)((MutableSingle<Object>)temp).getFirst();
                if (tags.isEmpty()) {
                    return false;
                }
                Route r = tags.poll();
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, r.toNbt());
                return !tags.isEmpty();                
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("Navigation error.", e);
            }
            return false;
        }, (IChunkReceiver<List<ClientRoute>>)(hasMore, list, iteration, nbt) -> {
            if (!nbt.contains(DataAccessorType.DEFAULT_NBT_DATA)) {
                return List.of();
            }
            if (list == null) {
                list = new ArrayList<ClientRoute>();
            }
            list.add(ClientRoute.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), true));
            return list;
        }, (chunks) -> {
            return chunks;
        }
    ));

    public static record DepartureRoutesData(String stationTagName, UUID player) {}
    public static final DataAccessorType<DepartureRoutesData, List<Pair<Boolean, ClientRoute>>, List<Pair<Boolean, ClientRoute>>> GET_DEPARTURE_AND_ARRIVAL_ROUTES_AT = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_departure_and_arrival_routes_at"), DataAccessorType.Builder.createChunked(
        (in, nbt) -> {
            nbt.putString("Station", in.stationTagName());
            nbt.putUUID("Player", in.player());
        }, (nbt) -> {
            return new DepartureRoutesData(nbt.getString("Station"), nbt.getUUID("Player"));
        }, (player, in, temp, nbt, iteration) -> {
            try {
                if (temp.getFirst() == null) {
                    UserSettings settings = UserSettings.getSettingsFor(in.player(), true);
                    StationTag station = GlobalSettings.getInstance().getOrCreateStationTagFor(TagName.of(in.stationTagName()));
                    Set<Train> trains = TrainUtils.getDepartingTrainsAt(station);
                    trains.removeIf(x -> 
                        !TrainUtils.isTrainUsable(x) ||
                        GlobalSettings.getInstance().isTrainBlacklisted(x) ||
                        !TrainListener.hasTrainData(x)
                    );

                    List<Pair<Boolean, Route>> routesL = new LinkedList<>();
                    for (Train train : trains) {
                        TrainData data = TrainListener.getTrainData(train.id).get();
                        List<TrainPrediction> matchingPredictions = data.getPredictionsChronologically();
                        
                        for (int i = 0; i < matchingPredictions.size(); i++) {
                            TrainPrediction prediction = matchingPredictions.get(i);
                            if (!prediction.getStationTag().equals(station)) {
                                continue;
                            }

                            ScheduleSection section = prediction.getSection();
                            if ((!section.isUsable() && !(section.isFirstStop(prediction) && section.previousSection().isUsable() && section.previousSection().shouldIncludeNextStationOfNextSection())) || (section.getTrainCategory().map(x -> settings.searchExcludedTrainCaegories.getValue().contains(x.getId())).orElse(false))) {
                                continue;
                            }

                            ScheduleSection previousSection = section.previousSection();

                            boolean isStart = section.isFirstStop(prediction); 
                            boolean isLast = section.isFinalStop(prediction); 
                            boolean isStartAndFinal = isStart && previousSection.isUsable() && previousSection.shouldIncludeNextStationOfNextSection() && (previousSection.getTrainCategory().map(x -> !settings.searchExcludedTrainCaegories.getValue().contains(x.getId())).orElse(true)); 
                            
                            TrainStop stop = new TrainStop(prediction);
                            stop.simulateTicks(settings.searchDepartureInTicks.getValue());
                            TrainPrediction fromPrediction = section.getFirstStop().get();
                            TrainStop from = new TrainStop(fromPrediction);

                            Route route = new Route(List.of(new RoutePart(data.getSessionId(), train.id, List.of(stop /* current/target */, from /* from */), section.getAllStops(settings.searchDepartureInTicks.getValue(), prediction.getEntryIndex()))), false);
                            
                            if ((!isStart || isStartAndFinal) && (section.getTrainCategory().map(x -> !settings.searchExcludedTrainCaegories.getValue().contains(x.getId())).orElse(true))) {
                                
                                Route selectedRoute = route;
                                if (isStartAndFinal) {
                                    TrainPrediction frPred = previousSection.getFirstStop().get();
                                    TrainStop fr = new TrainStop(frPred);
                                    selectedRoute = new Route(List.of(new RoutePart(data.getSessionId(), train.id, List.of(stop /* current/target */, fr /* from */), previousSection.getAllStops(settings.searchDepartureInTicks.getValue(), prediction.getEntryIndex()))), false);
                                }
                                routesL.add(Pair.of(true, selectedRoute)); // Arrival
                            }
                            if ((section.isUsable() && (!isLast || section.shouldIncludeNextStationOfNextSection())) && (section.getTrainCategory().map(x -> !settings.searchExcludedTrainCaegories.getValue().contains(x.getId())).orElse(true))) {
                                routesL.add(Pair.of(false, route)); // Departure
                            }
                        }
                    }
                    
                    Collections.sort(routesL, (a, b) -> {
                        long val1 = a.getFirst() ? a.getSecond().getStart().getScheduledArrivalTime() : a.getSecond().getStart().getScheduledDepartureTime();
                        long val2 = b.getFirst() ? b.getSecond().getStart().getScheduledArrivalTime() : b.getSecond().getStart().getScheduledDepartureTime();
                        return Long.compare(val1, val2);
                    });
                    temp.setFirst(new ConcurrentLinkedQueue<>(routesL));
                }

                @SuppressWarnings("unchecked")
                Queue<Pair<Boolean, Route>> tags = (Queue<Pair<Boolean, Route>>)((MutableSingle<Object>)temp).getFirst();
                if (tags.isEmpty()) {
                    return false;
                }
                Pair<Boolean, Route> r = tags.poll();
                nbt.putBoolean("IsArrival", r.getFirst());
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, r.getSecond().toNbt());
                return !tags.isEmpty();
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("Schedule board generation error.", e);
            }
            return false;
        }, (IChunkReceiver<List<Pair<Boolean, ClientRoute>>>)(hasMore, list, iteration, nbt) -> {
            if (!nbt.contains(DataAccessorType.DEFAULT_NBT_DATA)) {
                return List.of();
            }
            if (list == null) {
                list = new ArrayList<Pair<Boolean, ClientRoute>>();
            }
            list.add(Pair.of(nbt.getBoolean("IsArrival"), ClientRoute.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA), false)));
            return list;
        }, (chunks) -> {
            return chunks;
        }
    ));

    
    public static record DeparturesData(UUID stationTagId, UUID trainId, boolean realTimeOnly) {}
    public static final DataAccessorType<DeparturesData, List<ClientTrainStop>, List<ClientTrainStop>> GET_DEPARTURES_AT = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_departures_at"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putUUID("Tag", in.stationTagId());
            nbt.putUUID("Train", in.trainId());
            nbt.putBoolean("RealTimeOnly", in.realTimeOnly());
        }, (nbt) -> {
            return new DeparturesData(nbt.getUUID("Tag"), nbt.getUUID("Train"), nbt.getBoolean("RealTimeOnly"));
        }, (player, in, temp, nbt, iteration) -> {
            try {
                if (!GlobalSettings.getInstance().stationTagExists(in.stationTagId())) {
                    return false;
                }
                StationTag tag = GlobalSettings.getInstance().getStationTag(in.stationTagId()).get();
                ListTag list = new ListTag();
                for (TrainStop stop : TrainUtils.getDeparturesAt(tag, in.trainId(), in.realTimeOnly())) {
                    list.add(stop.toNbt(true));
                }
                nbt.put(DataAccessorType.DEFAULT_NBT_DATA, list);
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("Next connections error.", e);
            }
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return nbt.contains(DataAccessorType.DEFAULT_NBT_DATA) ? nbt.getList(DataAccessorType.DEFAULT_NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> (ClientTrainStop)ClientTrainStop.fromNbt((CompoundTag)x)).toList() : List.of();
        }
    ));

    public static final DataAccessorType<Void, Boolean, Boolean> ALL_TRAINS_INITIALIZED = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "all_trains_initialized"), DataAccessorType.Builder.createNoInput(
        (player, in, temp, nbt, iteration) -> {            
            nbt.putBoolean(DataAccessorType.DEFAULT_NBT_DATA, TrainListener.allTrainsInitialized());
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return nbt.getBoolean(DataAccessorType.DEFAULT_NBT_DATA);
        }
    ));

    public static final DataAccessorType<Void, List<TrainDebugData>, List<TrainDebugData>> GET_ALL_TRAINS_DEBUG_DATA = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_all_trains_debug_data"), DataAccessorType.Builder.createNoInput(
        (player, in, temp, nbt, iteration) -> {
            ListTag list = new ListTag();
            for (TrainData x : TrainListener.getAllTrainData()) {
                list.add(TrainDebugData.fromTrain(x).toNbt());
            }
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, list);
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return nbt.getList(DataAccessorType.DEFAULT_NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> TrainDebugData.fromNbt((CompoundTag)x)).toList();
        }
    ));

    public static final DataAccessorType<Void, Void, Void> SHOW_TRAIN_DEBUG_SCREEN = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "show_train_debug_screen"), DataAccessorType.Builder.createNoIO(
        (player, in, temp, nbt, iteration) -> {
            ClientWrapper.showTrainDebugScreen();
            return false;
        }
    ));

    public static final DataAccessorType<UUID, Void, Void> TRAIN_SOFT_RESET = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "train_soft_reset"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            TrainListener.getTrainData(in).ifPresent(TrainData::softResetPredictions);
            return false;
        }
    ));

    public static final DataAccessorType<UUID, Void, Void> TRAIN_HARD_RESET = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "train_hard_reset"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.putUUID(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getUUID(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            TrainListener.getTrainData(in).ifPresent(TrainData::hardResetPredictions);
            return false;
        }
    ));
    
    public static final DataAccessorType<String, DepartureHistory.Stats, DepartureHistory.Stats> GET_STATION_DEPARTURE_HISTORY = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_station_departure_history"), DataAccessorType.Builder.create(
        (in, nbt) -> {
            nbt.putString(DataAccessorType.DEFAULT_NBT_DATA, in);
        }, (nbt) -> {
            return nbt.getString(DataAccessorType.DEFAULT_NBT_DATA);
        }, (player, in, temp, nbt, iteration) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, DepartureHistory.Stats.ofStation(in).toNbt());
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return DepartureHistory.Stats.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }
    ));

    public static final DataAccessorType<Void, List<Owner>, List<Owner>> GET_ONLINE_PLAYERS = DataAccessorType.register(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "get_online_players"), DataAccessorType.Builder.createNoInput(
        (player, in, temp, nbt, iteration) -> {
            ListTag list = new ListTag();
            list.addAll(CRNPlatformSpecific.getAllKnownPlayers().entrySet().stream().map(e -> new Owner(e.getKey()).toNbt()).toList());
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, list);
            return false;
        }, (hasMore, data, iteration, nbt) -> {
            return nbt.getList(DataAccessorType.DEFAULT_NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> Owner.fromNbt((CompoundTag)x)).sorted((a, b) -> a.name().compareToIgnoreCase(b.name())).toList();
        }
    ));

    public static void init() {}
}
