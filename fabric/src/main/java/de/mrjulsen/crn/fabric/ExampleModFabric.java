package de.mrjulsen.crn.fabric;

import de.mrjulsen.crn.ExampleMod;
import net.fabricmc.api.ModInitializer;

public class ExampleModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleMod.load();
        ExampleMod.init();
        ExampleMod.REGISTRATE.register();
    }
}
