package de.mrjulsen.crn.network.packets.pain;

import java.util.List;
import java.util.UUID;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GetDeparturesAtPacketData {

    private static final String NBT_STATION_TAG_ID = "StationTagId";
    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_REAL_TIME_ONLY = "RealTimeOnly";
    private static final String NBT_ALLOW_DUPLICATES = "AllowDuplicates";
    private static final String NBT_DATA = "Data";

    public static class Request extends NetworkPacketData {
        
        private UUID stationTagId;
        private UUID trainId;
        private boolean realTimeOnly;
        private boolean allowDuplicates;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(UUID stationTagId, UUID trainId, boolean realTimeOnly, boolean allowDuplicates) {
            super(DLStatus.OK);
            this.stationTagId = stationTagId;
            this.trainId = trainId;
            this.realTimeOnly = realTimeOnly;
            this.allowDuplicates = allowDuplicates;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putUUID(NBT_STATION_TAG_ID, stationTagId);
            nbt.putUUID(NBT_TRAIN_ID, trainId);
            nbt.putBoolean(NBT_REAL_TIME_ONLY, realTimeOnly);
            nbt.putBoolean(NBT_ALLOW_DUPLICATES, allowDuplicates);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.stationTagId = nbt.getUUID(NBT_STATION_TAG_ID);
            this.trainId = nbt.getUUID(NBT_TRAIN_ID);
            this.realTimeOnly = nbt.getBoolean(NBT_REAL_TIME_ONLY);
            this.allowDuplicates = nbt.getBoolean(NBT_ALLOW_DUPLICATES);
        }
    }

    public static class Response extends NetworkPacketData {
        private List<TrainStop> rawData;
        private List<ClientTrainStop> data;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(List<TrainStop> rawData) {
            super(DLStatus.OK);
            this.rawData = rawData;
        }

        @Override
        protected void write(CompoundTag nbt) {
            ListTag list = new ListTag();
            for (TrainStop data : rawData) {
                list.add(data.toNbt(true));
            }
            nbt.put(NBT_DATA, list);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = nbt.getList(NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> (ClientTrainStop)ClientTrainStop.fromNbt((CompoundTag)x)).toList();
        }

        public List<ClientTrainStop> getData() {
            return data;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        try {
            if (!GlobalSettings.getInstance().stationTagExists(packet.stationTagId)) {
                return new Response(List.of());
            }
            StationTag tag = GlobalSettings.getInstance().getStationTag(packet.stationTagId).get();
            return new Response(TrainUtils.getDeparturesAt(tag, packet.trainId, packet.realTimeOnly, packet.allowDuplicates));
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Next connections error.", e);
        }
        return new Response(List.of());
    }
    
}
