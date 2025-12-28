package de.mrjulsen.crn.fabric;


import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.fabricmc.api.ModInitializer;

public class CreateRailwaysNavigatorFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CreateRailwaysNavigator.load();
        CreateRailwaysNavigator.init();
        CreateRailwaysNavigator.REGISTRATE.register();
    }
}
