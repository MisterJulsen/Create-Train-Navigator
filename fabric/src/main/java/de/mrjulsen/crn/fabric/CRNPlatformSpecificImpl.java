package de.mrjulsen.crn.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import io.github.fabricators_of_create.porting_lib.util.UsernameCache;

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

    public static Optional<String> getLastKnownPlayerName(UUID uuid) {
        return Optional.ofNullable(UsernameCache.getLastKnownUsername(uuid));
    }
    
    public static Map<UUID, String> getAllKnownPlayers() {
        return UsernameCache.getMap();
    }
}
