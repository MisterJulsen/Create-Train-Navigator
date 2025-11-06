package de.mrjulsen.crn.network.packets.pain;

import java.util.Optional;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class CreateTrainLinePacketData {

    private static final String NBT_DATA = "Data";

    public static class Request extends NetworkPacketData {
        
        private String name;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(String name) {
            super(DLStatus.OK);
            this.name = name;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putString(NBT_DATA, name);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.name = nbt.getString(NBT_DATA);
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
        if (!GlobalSettings.modificationsAllowed(context.getPlayer())) {
            return new Response(Optional.empty());
        }
        return new Response(Optional.ofNullable(GlobalSettings.getInstance().createOrGetTrainLine(packet.name, new Owner(context.getPlayer()))));
    }
    
}
