package de.mrjulsen.crn.network.packets.pain;

import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.backend.api.BoardEntry;
import de.mrjulsen.crn.backend.api.BoardQuery;
import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

/**
 * Asks the backend for a station's departure board.
 * <p>
 * The excluded train is the one the caller already knows about - the connections page is showing
 * what else leaves besides the train the traveller just got off.
 */
public class GetStationBoardPacketData {

    private static final String NBT_STATION_TAG_ID = "StationTagId";
    private static final String NBT_EXCLUDED_TRAIN_ID = "ExcludedTrainId";
    private static final String NBT_LIMIT = "Limit";
    private static final String NBT_ENTRIES = "Entries";

    public static class Request extends NetworkPacketData {

        private UUID stationTagId;
        private UUID excludedTrainId;
        private int limit;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(UUID stationTagId, UUID excludedTrainId, int limit) {
            super(DLStatus.OK);
            this.stationTagId = stationTagId;
            this.excludedTrainId = excludedTrainId;
            this.limit = limit;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putUUID(NBT_STATION_TAG_ID, stationTagId);
            NbtHelper.putNullableUUID(nbt, NBT_EXCLUDED_TRAIN_ID, excludedTrainId);
            nbt.putInt(NBT_LIMIT, limit);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.stationTagId = nbt.getUUID(NBT_STATION_TAG_ID);
            this.excludedTrainId = NbtHelper.readNullableUUID(nbt, NBT_EXCLUDED_TRAIN_ID);
            this.limit = nbt.getInt(NBT_LIMIT);
        }
    }

    public static class Response extends NetworkPacketData {

        private List<BoardEntry> entries = List.of();

        public Response(DLStatus status) {
            super(status);
        }

        public Response(List<BoardEntry> entries) {
            super(DLStatus.OK);
            this.entries = entries;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.put(NBT_ENTRIES, NbtHelper.writeList(entries, BoardEntry::toNbt));
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.entries = NbtHelper.readList(nbt, NBT_ENTRIES, BoardEntry::fromNbt);
        }

        public List<BoardEntry> getEntries() {
            return entries;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        BoardQuery query = BoardQuery.defaults().withLimit(packet.limit);
        List<BoardEntry> entries = RailwayBackendApi.getDepartures(packet.stationTagId, query).stream()
            .filter(entry -> packet.excludedTrainId == null || !packet.excludedTrainId.equals(entry.trainId()))
            .toList();
        return new Response(entries);
    }
}
