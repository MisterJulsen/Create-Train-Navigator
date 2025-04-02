package de.mrjulsen.crn.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
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
}
