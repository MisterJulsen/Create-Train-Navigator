package de.mrjulsen.crn.forge;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.mixin.ScheduleRuntimeAccessor;
import de.mrjulsen.crn.util.PenaltyResult;
import de.mrjulsen.crn.util.PenaltyResult.Category;
import de.mrjulsen.crn.util.PenaltyResult.Type;
import de.mrjulsen.mcdragonlib.data.MapCache;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

public class CRNPlatformSpecificImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static void registerConfig() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-client.toml");
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-common.toml");
    }

    public static Object customDestinationInstructions(ScheduleRuntime runtime, ScheduleEntry entry, ScheduleInstruction instruction) {
        if (instruction instanceof PrioritizedDestinationInstruction destination) {
            ScheduleRuntimeAccessor accessor = (ScheduleRuntimeAccessor)runtime;
            Train train = accessor.crn$getTrain();   
            List<String> filters = destination.getFilters();         
            List<Pattern> patterns = filters.stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
            INavigationExtension ext = (INavigationExtension)train.navigation;

            GlobalStation selectedDestination = null;
            int selectedPainCount = Integer.MAX_VALUE;
            boolean anyMatch = false;


            MapCache<Double, GlobalStation, GlobalStation> navigationCache = new MapCache<>((station) -> {
                return train.navigation.startNavigation(station, Double.MAX_VALUE, true);
            }, GlobalStation::hashCode);

			if (!train.hasForwardConductor() && !train.hasBackwardConductor()) {
				train.status.missingConductor();
				accessor.crn$setCooldown(accessor.crn$getInterval());
				return null;
			}

            for (Pattern regex : patterns) {
                AtomicInteger painCount = new AtomicInteger(0);
                GlobalStation bestStation = null;
                double bestCost = Double.MAX_VALUE;
                
                for (GlobalStation globalStation : train.graph.getPoints(EdgePointType.STATION)) {
                    if (!regex.matcher(globalStation.name).matches()) {
                        continue;
                    }
                    boolean matchesCurrent = train.currentStation != null && train.currentStation.equals(globalStation.id);
                    double cost = matchesCurrent ? 0 : navigationCache.get(globalStation, globalStation);

                    if (cost < 0)
                        continue;
                    if (cost > bestCost)
                        continue;
                    bestStation = globalStation;
                    bestCost = cost;
                }

                if (bestStation == null) {
                    continue;
                }
                anyMatch = true;

                if (
                    (bestStation.getImminentTrain() == null || bestStation.getImminentTrain() == train) &&
                    (bestStation.getPresentTrain() == null || bestStation.getPresentTrain() == train) &&
                    (bestStation.getNearestTrain() == null || bestStation.getNearestTrain() == train)
                ) {
                    painCount.addAndGet(1);
                }

                ext.getPenaltiesByDirection().ifPresent(x -> {
                    for (PenaltyResult.Type type : x.getPenalties().keySet()) {
                        if (destination.shouldAvoidRedSignals() && type == Type.REDSTONE_RED_SIGNAL) {
                            painCount.addAndGet(1);
                        } else if (destination.shouldAvoidTrains() && type.getCategory() == Category.TRAINS) {
                            painCount.addAndGet(1);
                        }
                    }
                });

                if (painCount.get() < selectedPainCount) {
                    selectedPainCount = painCount.get();
                    selectedDestination = bestStation;

                    if (painCount.get() <= 0)
                        break;
                }
            }

			if (selectedDestination == null) {
				if (anyMatch) {
					train.status.failedNavigation();
                } else {
					train.status.failedNavigationNoTarget(String.join(", ", filters));
                }
                accessor.crn$setCooldown(accessor.crn$getInterval());
				return null;
			}

			return selectedDestination;
		}
        return null;
    }
}
 