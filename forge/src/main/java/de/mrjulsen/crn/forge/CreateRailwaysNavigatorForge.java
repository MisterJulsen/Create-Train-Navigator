package de.mrjulsen.crn.forge;

import java.util.Objects;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CreateRailwaysNavigator.MOD_ID)
public class CreateRailwaysNavigatorForge {
    public CreateRailwaysNavigatorForge() {
        EventBuses.registerModEventBus(CreateRailwaysNavigator.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        registerVersionCheck();
        CreateRailwaysNavigator.load();
        CreateRailwaysNavigator.REGISTRATE.registerEventListeners(FMLJavaModLoadingContext.get().getModEventBus());
        CreateRailwaysNavigator.init();
    }

    private static void registerVersionCheck() {
        final String version = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();
        ModLoadingContext.get().registerExtensionPoint(
            IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(
                () -> version,
                (remoteVersion, isFromServer) -> Objects.equals(version, remoteVersion)));
    }
}
