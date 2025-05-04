package de.mrjulsen.crn.forge;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CreateRailwaysNavigator.MOD_ID)
public class CreateRailwaysNavigatorForge {
    public CreateRailwaysNavigatorForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(CreateRailwaysNavigator.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        CreateRailwaysNavigator.load();
        CreateRailwaysNavigator.REGISTRATE.registerEventListeners(FMLJavaModLoadingContext.get().getModEventBus());
        CreateRailwaysNavigator.init();
    }
}
