package de.mrjulsen.crn.data.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import java.lang.StringBuilder;
import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.ISavableNavigatorData;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class Route implements ISavableNavigatorData {

    protected static final String NBT_PARTS = "Parts";

    protected final List<RoutePart> parts;
    protected final List<TransferConnection> connections;
    protected final Cache<Boolean> isCancelled = new Cache<>(() -> getParts().stream().anyMatch(x -> x.isCancelled()));

    public Route(List<RoutePart> parts, boolean realTimeTracker) {
        this.parts = parts;
        this.connections = TransferConnection.getConnections(parts);
    }

    public static Route empty(boolean realTimeTracker) {
        return new Route(List.of(), realTimeTracker);
    }

    public List<TransferConnection> getConnections() {
        return connections;
    }

    public Optional<TransferConnection> getConnectionWith(TrainStop stop) {
        for (TransferConnection connection : getConnections()) {
            if (connection.getArrivalStation() == stop || connection.getDepartureStation() == stop) {
                return Optional.ofNullable(connection);
            }
        }
        return Optional.empty();
    }

    public RoutePart getFirstPart() {
        return parts.get(0);
    }

    public RoutePart getLastPart() {
        return parts.get(parts.size() - 1);
    }

    public TrainStop getStart() {
        return getFirstPart().getFirstStop();
    }

    public TrainStop getEnd() {
        return getLastPart().getLastStop();
    }

    public ImmutableList<RoutePart> getParts() {
        return ImmutableList.copyOf(parts);
    }
    
    public int getTransferCount() {
        return parts.size() - 1;
    }

    public long departureIn() {
        return getFirstPart().departureIn();
    }

    public long arrivalAtDestinationIn() {
        return getLastPart().timeUntilEnd();
    }

    public long travelTime() {
        return arrivalAtDestinationIn() - departureIn();
    }

    public boolean isAnyCancelled() {
        return isCancelled.get();
    }

    /** Checks whether the connection to this part of the route is still reachable or not. The first part is always reachable because there is no transfer there. A part is marked as reachable if the train has not yet departed there. */
    public boolean isPartReachable(RoutePart part) {
        int idx = parts.indexOf(part);
        if (idx <= 0) {
            return true;
        }
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).isConnectionMissed()) {
                return !(i < idx);
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ROUTE[" + getStart().getRealTimeStationTag().tagName() + " -> " + getEnd().getRealTimeStationTag().tagName() + "]");
        return builder.toString();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (RoutePart part : parts) {
            list.add(part.toNbt());
        }
        nbt.put(NBT_PARTS, list);
        return nbt;
    }

    public static Route fromNbt(CompoundTag nbt, boolean realTimeTracker) {
        return new Route(
            nbt.getList(NBT_PARTS, Tag.TAG_COMPOUND).stream().map(x -> RoutePart.fromNbt((CompoundTag)x)).toList(),
            realTimeTracker
        );
    }

    @Override
    public List<SavableNavigatorDataLine> getOverviewData() {
        List<SavableNavigatorDataLine> lines = new ArrayList<>();
        lines.add(new SavableNavigatorDataLine(TextUtils.text(DLTime.fromTicks(getStart().getScheduledDepartureTime(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME) + "   " + getStart().getRealTimeStationTag().tagName()), ModGuiIcons.ROUTE_START.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE)));
        lines.add(new SavableNavigatorDataLine(TextUtils.text(DLTime.fromTicks(getEnd().getScheduledArrivalTime(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME) + "   " + getEnd().getRealTimeStationTag().tagName()), ModGuiIcons.ROUTE_END.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE)));
        lines.add(new SavableNavigatorDataLine(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.date", (long)DLTime.fromTicks(getStart().getScheduledDepartureTime(), new ConfiguredTimeSystem()).toGameDays(), DLTime.fromTicks(getStart().getScheduledDepartureTime(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)).append(" | ").append(DLTime.fromTicks(departureIn(), new ConfiguredTimeSystem()).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME)), ModGuiIcons.CALENDAR.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE)));
        lines.add(new SavableNavigatorDataLine(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.transfers", getTransferCount()).append(TextUtils.text(" | " + DLTime.fromTicks(travelTime(), new ConfiguredTimeSystem()).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME))), ModGuiIcons.INFO.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE)));
        if (isAnyCancelled()) {
            lines.add(new SavableNavigatorDataLine(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.cancelled").withStyle(ChatFormatting.RED), ModGuiIcons.IMPORTANT.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE)));
        }
        return lines;
    }

    @Override
    public SavableNavigatorDataLine getTitle() {
        return new SavableNavigatorDataLine(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.saved_route"), ModGuiIcons.BOOKMARK.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE));
    }

    @Override
    public long timeOrderValue() {
        return getStart().getScheduledDepartureTime();
    }
}
