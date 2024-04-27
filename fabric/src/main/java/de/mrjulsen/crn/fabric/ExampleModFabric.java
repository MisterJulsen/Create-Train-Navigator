package de.mrjulsen.crn.fabric;


import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.registry.ModExtras;
import io.github.fabricators_of_create.porting_lib.event.client.ClientWorldEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public class ExampleModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleMod.load();
        ExampleMod.init();
        ExampleMod.REGISTRATE.register();
        
		ClientWorldEvents.LOAD.register((mc, level) -> ModExtras.register());
		ServerWorldEvents.LOAD.register((server, level) -> ModExtras.register());
        ModExtras.register();
    }
}
