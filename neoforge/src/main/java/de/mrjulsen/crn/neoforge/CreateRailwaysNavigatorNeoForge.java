package de.mrjulsen.crn.neoforge;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

@Mod(CreateRailwaysNavigator.MOD_ID)
public class CreateRailwaysNavigatorNeoForge {
    private static ModContainer modContainer;

    public CreateRailwaysNavigatorNeoForge(ModContainer container) {
        modContainer = container;
        CreateRailwaysNavigator.load();
        CreateRailwaysNavigator.REGISTRATE.registerEventListeners(ModLoadingContext.get().getActiveContainer().getEventBus());
        CreateRailwaysNavigator.init();
    }

    static ModContainer getModContainer() {
        return modContainer;
    }
}
