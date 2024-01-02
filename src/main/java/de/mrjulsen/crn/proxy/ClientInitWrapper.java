package de.mrjulsen.crn.proxy;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientInitWrapper {
    public static void setup(final FMLClientSetupEvent event) {
        ClientInit.setup(event);
    }
}
