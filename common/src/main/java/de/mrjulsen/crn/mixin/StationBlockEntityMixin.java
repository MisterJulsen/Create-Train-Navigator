package de.mrjulsen.crn.mixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.foundation.utility.Lang;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.train.StationDepartureHistory.StationStats;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Environment(EnvType.CLIENT)
@Mixin(StationBlockEntity.class)
public class StationBlockEntityMixin implements IHaveGoggleInformation {

    private StationBlockEntity self() {
        return (StationBlockEntity)(Object)this;
    }

    private StationStats stats;

    @SuppressWarnings("resource")
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (CRNPlatformSpecific.getStationFromBlockEntity(self()) == null) {
            return false;
        }

        if (Minecraft.getInstance().level.getGameTime() % 100 == 0) {
            DataAccessor.getFromServer(CRNPlatformSpecific.getStationFromBlockEntity(self()).name, ModAccessorTypes.GET_STATION_DEPARTURE_HISTORY, x -> this.stats = x);
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
            .add(formatTime(Minecraft.getInstance().level.getGameTime() - stats.getLastDepartureTime()))
            .forGoggles(tooltip, 1);
          
        if (stats.hasDeparturesByLine()) {
            Lang.builder(CreateRailwaysNavigator.MOD_ID)
                .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.line").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);   
                             
            List<Map.Entry<String, Long>> data = stats.getDeparturesByLine();
            for (Map.Entry<String, Long> d : data) {            
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.empty().append(TextUtils.text(d.getKey() + ": ").withStyle(ChatFormatting.DARK_AQUA)).append(formatTime(Minecraft.getInstance().level.getGameTime() - d.getValue())))
                    .forGoggles(tooltip, 1);
            }
            if (stats.getDeparturesByLineTotalCount() > data.size()) {                    
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.has_more", stats.getDeparturesByLineTotalCount() - data.size()).withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip);
            }
        }
        
        if (stats.hasDeparturesByGroup()) {
            Lang.builder(CreateRailwaysNavigator.MOD_ID)
                .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.group").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);

            List<Map.Entry<String, Long>> data = stats.getDeparturesByGroup();
            for (Map.Entry<String, Long> d : data) {            
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.empty().append(TextUtils.text(d.getKey() + ": ").withStyle(ChatFormatting.DARK_AQUA)).append(formatTime(Minecraft.getInstance().level.getGameTime() - d.getValue())))
                    .forGoggles(tooltip, 1);
            }
            if (stats.getDeparturesByGroupTotalCount() > data.size()) {                    
                Lang.builder(CreateRailwaysNavigator.MOD_ID)
                    .add(TextUtils.translate("goggles." + CreateRailwaysNavigator.MOD_ID + ".train_listener.departures.has_more", stats.getDeparturesByGroupTotalCount() - data.size()).withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip);
            }
        }

        return true;
    }

    private MutableComponent formatTime(long ticks) {
        return TextUtils.text(TimeUtils.formatDurationMs(TimeUnit.SECONDS.toMillis((long)(ticks / DragonLib.tps())))).withStyle(ChatFormatting.AQUA);
    }
}
