package de.mrjulsen.crn.network.packets.pain;

import java.util.List;

import de.mrjulsen.crn.backend.api.BoardEntry;
import de.mrjulsen.crn.backend.api.BoardQuery;
import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.util.NbtHelper;
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
        private List<BoardEntry> data = List.of();

        public Response(DLStatus status) {
            super(status);
        }

        public Response(List<BoardEntry> data) {
            super(DLStatus.OK);
            this.data = data;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.put(NBT_DATA, NbtHelper.writeList(data, BoardEntry::toNbt));
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = NbtHelper.readList(nbt, NBT_DATA, BoardEntry::fromNbt);
        }

        public List<BoardEntry> getData() {
            return data;
        }
    }

    /** The departures a passenger changing at the given station could take, excluding this train. */
    public static Response handle(Request packet, NetworkPacketContext context) {
        BoardQuery query = BoardQuery.defaults()
            .matching(x -> !x.trainId().equals(packet.data.selfTrainId()));
        if (packet.data.allowDuplicates()) {
            query = query.withDuplicates();
        }
        return new Response(RailwayBackendApi.getDepartures(packet.data.stationName(), query));
    }
    
}
