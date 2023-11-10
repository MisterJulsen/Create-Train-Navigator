package de.mrjulsen.crn.proxy;

import de.mrjulsen.crn.util.GuiUtils;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ClientProxy implements IProxy {

    @Override
    public void setup(FMLCommonSetupEvent event) {
        GuiUtils.init();
    }
    
}
