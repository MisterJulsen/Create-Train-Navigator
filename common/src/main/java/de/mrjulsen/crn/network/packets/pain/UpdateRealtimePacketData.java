package de.mrjulsen.crn.network.packets.pain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.backend.api.JourneySnapshot;
import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.backend.api.StopSnapshot;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.navigation.ClientRoutePart.TrainRealTimeData;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.ClientTrainStop.TrainStopRealTimeData;
import de.mrjulsen.crn.data.train.TrainStatus;
import de.mrjulsen.crn.registry.ModDelayCauses;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class UpdateRealtimePacketData {

    private static final String NBT_DATA = "Data";

    public static class Request extends NetworkPacketData {
        
        private UUID id;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(UUID id) {
            super(DLStatus.OK);
            this.id = id;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putUUID(NBT_DATA, id);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.id = nbt.getUUID(NBT_DATA);
        }
    }

    public static class Response extends NetworkPacketData {
        private Optional<TrainRealTimeData> data;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(Optional<TrainRealTimeData> data) {
            super(DLStatus.OK);
            this.data = data;
        }

        @Override
        protected void write(CompoundTag nbt) {
            data.ifPresent(x -> nbt.put(NBT_DATA, x.toNbt()));
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = Optional.ofNullable(nbt.contains(NBT_DATA) ? TrainRealTimeData.fromNbt(nbt.getCompound(NBT_DATA)) : null);
        }

        public Optional<TrainRealTimeData> getData() {
            return data;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        return new Response(RailwayBackendApi.getTrain(packet.id).map(train -> {
            long now = RailwayBackendApi.currentTime();
            Map<Integer, TrainStopRealTimeData> values = new HashMap<>();

            for (StopSnapshot stop : RailwayBackendApi.getJourney(packet.id).map(JourneySnapshot::stops).orElse(List.of())) {
                StationTag tag = GlobalSettings.getInstance().getOrCreateStationTagFor(stop.realtimeStationName());
                TrainStopRealTimeData realTimeData = new TrainStopRealTimeData(
                    tag.getClientTag(stop.realtimeStationName()),
                    stop.entryIndex(),
                    stop.scheduled().arrival(),
                    stop.scheduled().departure(),
                    stop.realtime().arrival(),
                    stop.realtime().departure(),
                    (int) stop.arrivalIn(now),
                    stop.completedVisits()
                );
                values.put(realTimeData.entryIndex(), realTimeData);
            }

            return TrainRealTimeData.createServer(train.sessionId(), values, statusOf(train), train.isCancelled());
        }));
    }

    private static Set<ResourceLocation> statusOf(TrainSnapshot train) {
        Set<ResourceLocation> status = new HashSet<>();
        if (train.isCancelled()) {
            status.add(TrainStatus.CANCELLED.getLocation());
        }
        for (DelayInstance delay : train.delays()) {
            if (!delay.severity().isDelay()) {
                continue;
            }
            status.add(delay.causeId().equals(ModDelayCauses.PREVIOUS_JOURNEY.id())
                ? TrainStatus.DELAY_FROM_PREVIOUS_JOURNEY.getLocation()
                : TrainStatus.DEFAULT_DELAY.getLocation());
        }
        return status;
    }
    
}
