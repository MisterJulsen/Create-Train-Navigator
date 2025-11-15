package de.mrjulsen.crn.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.UsernameCache;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.file.Path;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.tterrag.registrate.util.entry.RegistryEntry;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
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

    public static Optional<String> getLastKnownPlayerName(UUID uuid) {
        return Optional.ofNullable(UsernameCache.getLastKnownUsername(uuid));
    }
    
    public static Map<UUID, String> getAllKnownPlayers() {
        return UsernameCache.getMap();
    }
    
    public static GlobalStation getStationFromBlockEntity(BlockEntity be) {
        if (!(be instanceof StationBlockEntity stationBe))
			return null;
		
        return stationBe.getStation();
    }

    public static BlockEntity getClientContraptionBlockEntity(Contraption contraption, BlockPos localPos) {
        return contraption.getBlockEntityClientSide(localPos);
    }

    public static final RegistryEntry<AdvancedDisplaySource> registerDisplaySource() {
        return CreateRailwaysNavigator.REGISTRATE.displaySource("advanced_display", AdvancedDisplaySource::new)
        .onRegisterAfter(ForgeRegistries.BLOCK_ENTITY_TYPES.getRegistryKey(), (src) -> {            
            DisplaySource.BY_BLOCK_ENTITY.add(AllBlockEntityTypes.TRACK_STATION.get(), src);
        })
        .register();
    }
    
}
 