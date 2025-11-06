package de.mrjulsen.crn.mixin;

import java.util.List;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.foundation.utility.Lang;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.train.DepartureHistory;
import de.mrjulsen.crn.network.packets.pain.GetStationDepartureHistoryPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.format.TimeFormatDigitalDuration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Environment(EnvType.CLIENT)
@Mixin(StationBlockEntity.class)
public class StationBlockEntityMixin implements IHaveGoggleInformation {

    private static final int MAX_ENTRIES = 5;

    private StationBlockEntity self() {
        return (StationBlockEntity)(Object)this;
    }

    private DepartureHistory.Stats stats;

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (CRNPlatformSpecific.getStationFromBlockEntity(self()) == null) {
            return false;
        }

        if (Minecraft.getInstance().level.getGameTime() % 100 == 0) {
            ModNetworkManager.GET_STATIONDEPARTURE_HISTORY.send(NetworkDirection.toServer(), new GetStationDepartureHistoryPacketData.Request(CRNPlatformSpecific.getStationFromBlockEntity(self()).name), (response) -> {
                this.stats = response.getHistory();
            }, () -> {});
        }
        
        Lang.builder(CreateRailwaysNavigator.MOD_ID)
            .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.title"))
            .forGoggles(tooltip);

        

        if (this.stats == null || this.stats.isEmpty()) {
            Lang.builder(CreateRailwaysNavigator.MOD_ID)
                .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.nothing").withStyle(ChatFormatting.RED))
                .forGoggles(tooltip);
            return false;
        }

        Lang.builder(CreateRailwaysNavigator.MOD_ID)
            .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.any").withStyle(ChatFormatting.GRAY))
            .forGoggles(tooltip);
        Lang.builder(CreateRailwaysNavigator.MOD_ID)
            .add(formatTime(Minecraft.getInstance().level.getGameTime() - stats.getLastDeparture()))
            .forGoggles(tooltip, 1);
          
        if (!stats.getDeparturesByCategory().isEmpty()) {
            Lang.builder(CreateRailwaysNavigator.MOD_ID)
                .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.category").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);
                             
            Map<String, Long> data = stats.getDeparturesByCategory();
            int i = 0;
            for (Map.Entry<String, Long> d : data.entrySet()) {            
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.empty().append(TextUtils.text(d.getKey() + ": ").withStyle(ChatFormatting.DARK_AQUA)).append(formatTime(Minecraft.getInstance().level.getGameTime() - d.getValue())))
                    .forGoggles(tooltip, 1);

                i++;
                if (i >= MAX_ENTRIES) {
                    break;
                }
            }
            if (data.size() > MAX_ENTRIES) {                    
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.has_more", data.size() - MAX_ENTRIES).withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip);
            }
        }
        
        if (!stats.getDeparturesByLine().isEmpty()) {
            Lang.builder(CreateRailwaysNavigator.MOD_ID)
                .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.line").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);   
                             
            Map<String, Long> data = stats.getDeparturesByLine();
            int i = 0;
            for (Map.Entry<String, Long> d : data.entrySet()) {            
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.empty().append(TextUtils.text(d.getKey() + ": ").withStyle(ChatFormatting.DARK_AQUA)).append(formatTime(Minecraft.getInstance().level.getGameTime() - d.getValue())))
                    .forGoggles(tooltip, 1);

                i++;
                if (i >= MAX_ENTRIES) {
                    break;
                }
            }
            if (data.size() > MAX_ENTRIES) {                    
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.has_more", data.size() - MAX_ENTRIES).withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip);
            }
        }
        
        if (!stats.getDeparturesByName().isEmpty()) {
            Lang.builder(CreateRailwaysNavigator.MOD_ID)
                .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.name").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);   
                             
            Map<String, Long> data = stats.getDeparturesByName();
            int i = 0;
            for (Map.Entry<String, Long> d : data.entrySet()) {            
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.empty().append(TextUtils.text(d.getKey() + ": ").withStyle(ChatFormatting.DARK_AQUA)).append(formatTime(Minecraft.getInstance().level.getGameTime() - d.getValue())))
                    .forGoggles(tooltip, 1);

                i++;
                if (i >= MAX_ENTRIES) {
                    break;
                }
            }
            if (data.size() > MAX_ENTRIES) {                    
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.has_more", data.size() - MAX_ENTRIES).withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip);
            }
        }
        
        
        Lang.builder(CreateRailwaysNavigator.MOD_ID)
            .add(TextUtils.text(" "))
            .forGoggles(tooltip);

        Lang.builder(CreateRailwaysNavigator.MOD_ID)
            .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.press_shift_for_in_game_time").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC))
            .forGoggles(tooltip);

        return true;
    }

    private MutableComponent formatTime(long ticks) {
        DLTime time = DLTime.fromTicks(ticks, VanillaTimeSystem.INSTANCE);
        return TextUtils.text(Minecraft.getInstance().player.isShiftKeyDown()
            ? time.format(new TimeFormatDigitalDuration(Constants.NULL_TIME, false, false, true, true, false), TimeContext.INGAME)
            : time.format(new TimeFormatDigitalDuration(Constants.NULL_TIME, false, true, true, true, false), TimeContext.REAL)
        ).withStyle(ChatFormatting.AQUA);
    }
}
