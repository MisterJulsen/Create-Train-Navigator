package de.mrjulsen.crn.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.mixin.ContraptionAccessor;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import fuzs.forgeconfigapiport.impl.config.ForgeConfigRegistryImpl;
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
            ForgeConfigRegistryImpl.INSTANCE.register(CreateRailwaysNavigator.MOD_ID, ModConfig.Type.CLIENT, ModClientConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-client.toml");
        }
        ForgeConfigRegistryImpl.INSTANCE.register(CreateRailwaysNavigator.MOD_ID, ModConfig.Type.COMMON, ModCommonConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-common.toml");
    }
    
    public static GlobalStation getStationFromBlockEntity(BlockEntity be) {
        if (!(be instanceof StationBlockEntity stationBe))
			return null;
		
        return stationBe.getStation();
    }

    public static Optional<String> getLastKnownPlayerName(UUID uuid) {
        return Optional.ofNullable(UsernameCache.getLastKnownUsername(uuid));
    }
    
    public static Map<UUID, String> getAllKnownPlayers() {
        return UsernameCache.getMap();
    }

    public static BlockEntity getClientContraptionBlockEntity(Contraption contraption, BlockPos localPos) {
        var maybeNullClientContraption = ((ContraptionAccessor)contraption).crn$clientContraption().getAcquire();
        if (maybeNullClientContraption == null) {
            return null;
        }
        return maybeNullClientContraption.getBlockEntity(localPos);
    }
}
