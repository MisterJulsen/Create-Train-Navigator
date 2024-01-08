package de.mrjulsen.crn.event;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.event.listeners.TrainListener;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID)
public class ModEvents {
    
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) { 
        TrainListener.start();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppingEvent event) {
        TrainListener.stop();
    }
}


