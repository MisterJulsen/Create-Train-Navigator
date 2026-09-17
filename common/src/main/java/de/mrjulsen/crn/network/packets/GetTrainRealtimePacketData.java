package de.mrjulsen.crn.network.packets;

import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class GetTrainRealtimePacketData {

    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_INCLUDE_JOURNEY = "IncludeJourney";
    private static final String NBT_TRAIN = "Train";
    private static final String NBT_JOURNEY = "Journey";

    public static class Request extends NetworkPacketData {

        private UUID trainId;
        private boolean includeJourney;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(UUID trainId, boolean includeJourney) {
            super(DLStatus.OK);
            this.trainId = trainId;
            this.includeJourney = includeJourney;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putUUID(NBT_TRAIN_ID, trainId);
            nbt.putBoolean(NBT_INCLUDE_JOURNEY, includeJourney);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.trainId = nbt.getUUID(NBT_TRAIN_ID);
            this.includeJourney = nbt.getBoolean(NBT_INCLUDE_JOURNEY);
        }
    }

    public static class Response extends NetworkPacketData {

        private Optional<TrainSnapshot> train = Optional.empty();
        private Optional<JourneySnapshot> journey = Optional.empty();

        public Response(DLStatus status) {
            super(status);
        }

        public Response(Optional<TrainSnapshot> train, Optional<JourneySnapshot> journey) {
            super(DLStatus.OK);
            this.train = train;
            this.journey = journey;
        }

        @Override
        protected void write(CompoundTag nbt) {
            train.ifPresent(x -> nbt.put(NBT_TRAIN, x.toNbt()));
            journey.ifPresent(x -> nbt.put(NBT_JOURNEY, x.toNbt()));
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.train = nbt.contains(NBT_TRAIN) ? Optional.of(TrainSnapshot.fromNbt(nbt.getCompound(NBT_TRAIN))) : Optional.empty();
            this.journey = nbt.contains(NBT_JOURNEY) ? Optional.of(JourneySnapshot.fromNbt(nbt.getCompound(NBT_JOURNEY))) : Optional.empty();
        }

        public Optional<TrainSnapshot> getTrain() {
            return train;
        }

        public Optional<JourneySnapshot> getJourney() {
            return journey;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        return new Response(
            RailwayBackendApi.getTrain(packet.trainId),
            packet.includeJourney ? RailwayBackendApi.getJourney(packet.trainId) : Optional.empty()
        );
    }
}
