package de.mrjulsen.crn.proxy;

import de.mrjulsen.crn.ModMain;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ServerInit {

    public static void setup(FMLCommonSetupEvent event) {
        ModMain.LOGGER.info("Welcome to the CREATE RAILWAYS NAVIGATOR mod by MRJULSEN.");
    }
    
}
