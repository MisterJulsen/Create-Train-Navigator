package de.mrjulsen.crn.forge;

import de.mrjulsen.crn.ExampleMod;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ExampleMod.MOD_ID)
public class ExampleModForge {
    public ExampleModForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(ExampleMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        ExampleMod.load();
        ExampleMod.REGISTRATE.registerEventListeners(FMLJavaModLoadingContext.get().getModEventBus());
        ExampleMod.init();
    }
}
