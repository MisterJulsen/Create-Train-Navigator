package de.mrjulsen.crn.proxy;

import de.mrjulsen.crn.util.ModGuiUtils;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientInit {
    public static void setup(final FMLClientSetupEvent event) {        
        ModGuiUtils.init();
    }
}
