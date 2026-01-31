package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.crn.data.train.portable.NextConnectionsDisplayData;
import de.mrjulsen.crn.registry.data.NextConnectionsRequestData;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class GetNextConnectionsDisplayDataPacketData {

    private static final String NBT_DATA = "Data";

    public static class Request extends NetworkPacketData {
        
        private NextConnectionsRequestData data;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(NextConnectionsRequestData data) {
            super(DLStatus.OK);
            this.data = data;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.put(NBT_DATA, data.toNbt());
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = NextConnectionsRequestData.fromNbt(nbt.getCompound(NBT_DATA));
        }
    }

    public static class Response extends NetworkPacketData {
        private NextConnectionsDisplayData data;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(NextConnectionsDisplayData data) {
            super(DLStatus.OK);
            this.data = data;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.put(NBT_DATA, data.toNbt());
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = NextConnectionsDisplayData.fromNbt(nbt.getCompound(NBT_DATA));
        }

        public NextConnectionsDisplayData getData() {
            return data;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        return new Response(NextConnectionsDisplayData.at(packet.data.stationName(), packet.data.selfTrainId(), packet.data.allowDuplicates()));
    }
    
}
