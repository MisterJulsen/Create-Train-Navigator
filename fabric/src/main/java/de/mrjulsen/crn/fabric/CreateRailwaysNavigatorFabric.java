package de.mrjulsen.crn.fabric;


import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.registry.ModExtras;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.github.fabricators_of_create.porting_lib.event.client.ClientWorldEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public class CreateRailwaysNavigatorFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CreateRailwaysNavigator.load();
        CreateRailwaysNavigator.init();
        CreateRailwaysNavigator.REGISTRATE.register();
        
        if (Platform.getEnvironment() == Env.CLIENT) {
            ClientWorldEvents.LOAD.register((mc, level) -> ModExtras.init());
        }
		ServerWorldEvents.LOAD.register((server, level) -> ModExtras.init());
        ModExtras.init();
    }
}
