package de.mrjulsen.crn.network.packets;

import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.api.core.JourneySnapshot;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.TrainSnapshot;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class GetTrainDisplayDataPacketData {

    private static final String NBT_DATA = "Data";
    private static final String NBT_TRAIN = "Train";
    private static final String NBT_JOURNEY = "Journey";

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
        private TrainSnapshot train;
        private JourneySnapshot journey;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(TrainSnapshot train, JourneySnapshot journey) {
            super(DLStatus.OK);
            this.train = train;
            this.journey = journey;
        }

        @Override
        protected void write(CompoundTag nbt) {
            if (train != null) {
                nbt.put(NBT_TRAIN, train.toNbt());
            }
            if (journey != null) {
                nbt.put(NBT_JOURNEY, journey.toNbt());
            }
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.train = nbt.contains(NBT_TRAIN) ? TrainSnapshot.fromNbt(nbt.getCompound(NBT_TRAIN)) : null;
            this.journey = nbt.contains(NBT_JOURNEY) ? JourneySnapshot.fromNbt(nbt.getCompound(NBT_JOURNEY)) : null;
        }

        public Optional<TrainSnapshot> getTrain() {
            return Optional.ofNullable(train);
        }

        public Optional<JourneySnapshot> getJourney() {
            return Optional.ofNullable(journey);
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        Optional<Train> trainOpt = TrainUtils.getTrain(packet.id);
        if (trainOpt.isEmpty() || !TrainUtils.isTrainUsable(trainOpt.get()) || GlobalSettings.getInstance().isTrainBlacklisted(trainOpt.get())) {
            return new Response(null, null);
        }
        return new Response(
            RailwayBackendApi.getTrain(packet.id).orElse(null),
            RailwayBackendApi.getJourney(packet.id).orElse(null)
        );
    }

}
