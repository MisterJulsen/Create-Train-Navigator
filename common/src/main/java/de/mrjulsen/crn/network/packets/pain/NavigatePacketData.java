package de.mrjulsen.crn.network.packets.pain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.NavigatorRoutes;
import de.mrjulsen.crn.data.navigation.Route;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.navigator.NavigationQuery;
import de.mrjulsen.crn.navigator.NavigationResult;
import de.mrjulsen.crn.navigator.Navigator;
import de.mrjulsen.crn.navigator.RouteOptimization;
import de.mrjulsen.crn.navigator.route.RouteJourney;
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
        private List<RouteJourney> data;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(List<RouteJourney> rawData) {
            super(DLStatus.OK);
            this.data = rawData;
        }

        @Override
        protected void write(CompoundTag nbt) {
            ListTag list = new ListTag();
            for (RouteJourney route : data) {
                list.add(route.toNbt());
            }
            nbt.put(NBT_DATA, list);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = nbt.getList(NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> RouteJourney.fromNbt((CompoundTag)x)).toList();
        }

        public List<RouteJourney> getData() {
            return data;
        }        
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        try {
            GlobalSettings settings = GlobalSettings.getInstance();
            UserSettings userSettings = UserSettings.getSettingsFor(packet.playerId, true);

            NavigationQuery query = NavigationQuery
                    .from(settings.getTagByName(TagName.of(packet.start)).orElse(settings.getOrCreateStationTagFor(packet.start)).getTagName().get())
                    .to(settings.getTagByName(TagName.of(packet.end)).orElse(settings.getOrCreateStationTagFor(packet.end)).getTagName().get())
                    .departingIn(userSettings.navigationDepartureInTicks.getValue())
                    .withMinTransferTime(userSettings.navigationTransferTime.getValue())
                    .excludingCategories(userSettings.navigationExcludedTrainCategories.getValue())
                    .preferring(RouteOptimization.FEWEST_TRANSFERS);

            NavigationResult result = Navigator.search(query);
            if (ModCommonConfig.ADVANCED_LOGGING.get()) {
                CreateRailwaysNavigator.LOGGER.info(String.format("%s route(s) calculated. Took %sms. Searched %s nodes, %s trips.",
                        result.size(),
                        result.durationMs(),
                        result.stationsSearched(),
                        result.tripsScanned()
                ));
            }

            return new Response(result.byDeparture());
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Navigation error.", e);
        }
        return new Response(List.of());
    }
    
}
