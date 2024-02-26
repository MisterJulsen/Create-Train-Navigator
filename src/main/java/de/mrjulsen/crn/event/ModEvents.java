package de.mrjulsen.crn.event;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.event.listeners.TrainListener;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.stc.TimeCorrectionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID)
public class ModEvents {

    private static long lastTicks = 0;
    private static ServerLevel server;
    
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) { 
        TrainListener.start(event.getServer().overworld());
        server = event.getServer().overworld();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppingEvent event) {
        TrainListener.stop();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }

        if (server != null) {
            long currentTicks = server.dayTime();
            if (Math.abs(currentTicks - lastTicks) > 1) {
                server.players().stream().filter(p -> p instanceof ServerPlayer).forEach(x -> NetworkManager.getInstance().sendToClient(new TimeCorrectionPacket((int)(currentTicks - lastTicks)), x));
            }
            lastTicks = currentTicks;
        }
    }
}


