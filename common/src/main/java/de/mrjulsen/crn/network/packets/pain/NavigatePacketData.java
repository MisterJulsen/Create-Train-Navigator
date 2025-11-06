package de.mrjulsen.crn.network.packets.pain;

import java.util.List;
import java.util.UUID;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.NavigableGraph;
import de.mrjulsen.crn.data.navigation.Route;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class NavigatePacketData {

    private static final String NBT_START = "Start";
    private static final String NBT_END = "End";
    private static final String NBT_ID = "Id";
    private static final String NBT_DATA = "Data";

    public static class Request extends NetworkPacketData {
        
        private String start;
        private String end;
        private UUID playerId;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(String start, String end, UUID playerId) {
            super(DLStatus.OK);
            this.start = start;
            this.end = end;
            this.playerId = playerId;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putString(NBT_START, start);
            nbt.putString(NBT_END, end);
            nbt.putUUID(NBT_ID, playerId);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.start = nbt.getString(NBT_START);
            this.end = nbt.getString(NBT_END);
            this.playerId = nbt.getUUID(NBT_ID);
        }
    }

    public static class Response extends NetworkPacketData {
        private List<Route> rawData;
        private List<ClientRoute> data;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(List<Route> rawData) {
            super(DLStatus.OK);
            this.rawData = rawData;
        }

        @Override
        protected void write(CompoundTag nbt) {
            ListTag list = new ListTag();
            for (Route route : rawData) {
                list.add(route.toNbt());
            }
            nbt.put(NBT_DATA, list);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = nbt.getList(NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> ClientRoute.fromNbt((CompoundTag)x, true)).toList();
        }

        public List<ClientRoute> getData() {
            return data;
        }        
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        try {
            GlobalSettings settings = GlobalSettings.getInstance();
            List<Route> routes = NavigableGraph.searchRoutes(
                settings.getTagByName(TagName.of(packet.start)).orElse(settings.getOrCreateStationTagFor(packet.start)),
                settings.getTagByName(TagName.of(packet.end)).orElse(settings.getOrCreateStationTagFor(packet.end)),
                packet.playerId,
                true
            );
            return new Response(routes);
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Navigation error.", e);
        }
        return new Response(List.of());
    }
    
}
