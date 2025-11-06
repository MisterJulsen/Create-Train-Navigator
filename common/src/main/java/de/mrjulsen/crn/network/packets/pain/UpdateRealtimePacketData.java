package de.mrjulsen.crn.network.packets.pain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.data.navigation.ClientRoutePart.TrainRealTimeData;
import de.mrjulsen.crn.data.train.ClientTrainStop.TrainStopRealTimeData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

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
        return new Response(Optional.ofNullable(TrainListener.getTrainData(packet.id).map(data -> {
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
            return TrainRealTimeData.createServer(data.getSessionId(), values, data.getStatus(), data.isCancelled());
        }).orElse(null)));
    }
    
}
