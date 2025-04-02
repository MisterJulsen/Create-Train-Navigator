package de.mrjulsen.crn.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
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
import io.github.fabricators_of_create.porting_lib.util.ServerLifecycleHooks;

public class CRNPlatformSpecificImpl {
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static void registerConfig() {        
        if (Platform.getEnvironment() == Env.CLIENT) {
            ModLoadingContext.registerConfig(CreateRailwaysNavigator.MOD_ID, ModConfig.Type.CLIENT, ModClientConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-client.toml");
        }
        ModLoadingContext.registerConfig(CreateRailwaysNavigator.MOD_ID, ModConfig.Type.COMMON, ModCommonConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-common.toml");
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

            DiscoveredPath selectedDestination = null;
            int selectedPainCount = Integer.MAX_VALUE;
            boolean anyMatch = false;


            MapCache<DiscoveredPath, GlobalStation, GlobalStation> navigationCache = new MapCache<>((station) -> {
                return train.navigation.findPathTo(station, Double.MAX_VALUE);
            }, GlobalStation::hashCode);

			if (!train.hasForwardConductor() && !train.hasBackwardConductor()) {
				train.status.missingConductor();
				accessor.crn$setCooldown(accessor.crn$getInterval());
				return null;
			}

            for (Pattern regex : patterns) {
                AtomicInteger painCount = new AtomicInteger(0);
                GlobalStation bestStation = null;
                DiscoveredPath bestPath = null;
                double bestCost = Double.MAX_VALUE;
                
                for (GlobalStation globalStation : train.graph.getPoints(EdgePointType.STATION)) {
                    if (!regex.matcher(globalStation.name).matches()) {
                        continue;
                    }
                    DiscoveredPath discoveredPath = navigationCache.get(globalStation, globalStation);

                    if (discoveredPath == null) {
                        continue;
                    }

                    if (discoveredPath.cost < 0)
                        continue;
                    if (discoveredPath.cost > bestCost)
                        continue;
                    bestStation = globalStation;
                    bestPath = discoveredPath;
                    bestCost = discoveredPath.cost;
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
                    selectedDestination = bestPath;

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
