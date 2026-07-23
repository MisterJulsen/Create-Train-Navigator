package de.mrjulsen.crn.network.packets.pain;

import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.api.BoardEntry;
import de.mrjulsen.crn.backend.api.BoardQuery;
import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

/**
 * Asks the backend for a station's departure board.
 * <p>
 * Every call is sent once. Whether it belongs on a board as an arrival, as a departure or as both
 * follows from {@link BoardEntry#originating()} and {@link BoardEntry#terminus()} and is decided
 * where the rows are laid out: a train ending here has no departure to offer, one starting here has
 * no arrival, and everything in between has both.
 * <p>
 * The excluded train is the one the caller already knows about - the connections page shows what else
 * leaves besides the train the traveller just got off.
 */
public class GetStationBoardPacketData {

    /** No cap on how many entries to return. */
    public static final int UNLIMITED = 0;

    private static final String NBT_STATION_TAG_ID = "StationTagId";
    private static final String NBT_EXCLUDED_TRAIN_ID = "ExcludedTrainId";
    private static final String NBT_PLAYER_ID = "PlayerId";
    private static final String NBT_LIMIT = "Limit";
    private static final String NBT_ENTRIES = "Entries";

    public static class Request extends NetworkPacketData {

        private UUID stationTagId;
        private UUID excludedTrainId;
        private UUID playerId;
        private int limit;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(UUID stationTagId, UUID excludedTrainId, int limit) {
            this(stationTagId, excludedTrainId, null, limit);
        }

        /**
         * @param playerId Whose search settings to apply - the categories they have hidden, and how
         *                 far ahead they want to look. {@code null} asks for the board as it is.
         */
        public Request(UUID stationTagId, UUID excludedTrainId, UUID playerId, int limit) {
            super(DLStatus.OK);
            this.stationTagId = stationTagId;
            this.excludedTrainId = excludedTrainId;
            this.playerId = playerId;
            this.limit = limit;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putUUID(NBT_STATION_TAG_ID, stationTagId);
            NbtHelper.putNullableUUID(nbt, NBT_EXCLUDED_TRAIN_ID, excludedTrainId);
            NbtHelper.putNullableUUID(nbt, NBT_PLAYER_ID, playerId);
            nbt.putInt(NBT_LIMIT, limit);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.stationTagId = nbt.getUUID(NBT_STATION_TAG_ID);
            this.excludedTrainId = NbtHelper.readNullableUUID(nbt, NBT_EXCLUDED_TRAIN_ID);
            this.playerId = NbtHelper.readNullableUUID(nbt, NBT_PLAYER_ID);
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
        try {
            BoardQuery query = BoardQuery.defaults().withDuplicates();
            if (packet.limit > UNLIMITED) {
                query = query.withLimit(packet.limit);
            }
            if (packet.excludedTrainId != null) {
                query = query.matching(entry -> !packet.excludedTrainId.equals(entry.trainId()));
            }
            if (packet.playerId != null) {
                UserSettings settings = UserSettings.getSettingsFor(packet.playerId, true);
                query = query
                    .from(RailwayBackendApi.currentTime() + settings.searchDepartureInTicks.getValue())
                    .matching(entry -> !settings.searchExcludedTrainCaegories.getValue().contains(entry.category().id()));
            }
            return new Response(RailwayBackendApi.getDepartures(packet.stationTagId, query));
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Station board generation error.", e);
        }
        return new Response(List.of());
    }
}
