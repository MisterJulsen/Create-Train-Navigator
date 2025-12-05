package de.mrjulsen.crn.network.packets.pain;

import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.data.train.portable.TrainDisplayData;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class GetTrainDisplayDataPacketData {

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
        private TrainDisplayData data;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(TrainDisplayData data) {
            super(DLStatus.OK);
            this.data = data;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.put(NBT_DATA, data.toNbt());
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = TrainDisplayData.fromNbt(nbt.getCompound(NBT_DATA));
        }

        public TrainDisplayData getData() {
            return data;
        }

    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        Optional<Train> trainOpt = TrainUtils.getTrain(packet.id);
        if (!trainOpt.isPresent() || !TrainUtils.isTrainUsable(trainOpt.get()) || GlobalSettings.getInstance().isTrainBlacklisted(trainOpt.get())) {
            return new Response(TrainDisplayData.empty());
        }
        Response r = new Response(TrainDisplayData.of(trainOpt.get()));
        return r;
    }
    
}
