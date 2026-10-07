package de.mrjulsen.crn.network.packets;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.core.history.DepartureStats;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class GetStationDepartureHistoryPacketData {

    private static final String NBT_DATA = "Data";
    private static final String NBT_NAME = "Name";

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
            nbt.putString(NBT_NAME, name);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.name = nbt.getString(NBT_NAME);
        }
    }

    public static class Response extends NetworkPacketData {
        private DepartureStats history;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(DepartureStats history) {
            super(DLStatus.OK);
            this.history = history;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.put(NBT_DATA, history.toNbt());
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.history = DepartureStats.fromNbt(nbt.getCompound(NBT_DATA));
        }

        public DepartureStats getHistory() {
            return history;
        }

    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        return new Response(RailwayBackendApi.getDepartureStats(packet.name));
    }

}
