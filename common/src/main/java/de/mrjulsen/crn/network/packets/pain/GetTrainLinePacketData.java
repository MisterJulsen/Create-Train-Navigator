package de.mrjulsen.crn.network.packets.pain;

import java.util.Optional;
import java.util.UUID;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class GetTrainLinePacketData {

    private static final String NBT_DATA = "Data";    
    private static final String NBT_ID = "Id";

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
            nbt.putUUID(NBT_ID, id);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.id = nbt.getUUID(NBT_ID);
        }
    }

    public static class Response extends NetworkPacketData {
        private Optional<TrainLine> line;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(Optional<TrainLine> line) {
            super(DLStatus.OK);
            this.line = line;
        }

        @Override
        protected void write(CompoundTag nbt) {
            this.line.ifPresent(x -> nbt.put(NBT_DATA, x.toNbt()));
        }

        @Override
        protected void read(CompoundTag nbt) {
            if (nbt.contains(NBT_DATA)) {
                this.line = Optional.ofNullable(TrainLine.fromNbt(nbt.getCompound(NBT_DATA)));
            } else {
                this.line = Optional.empty();
            }
        }

        public Optional<TrainLine> getLine() {
            return line;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        return new Response(GlobalSettings.getInstance().getTrainLine(packet.id));
    }
    
}
