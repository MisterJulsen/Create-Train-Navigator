package de.mrjulsen.crn.event;

import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.event.listeners.TrainListener;
import de.mrjulsen.crn.network.packets.stc.TimeCorrectionPacket;
import de.mrjulsen.crn.registry.ModExtras;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ModEvents {

    private static long lastTicks = 0;
    private static ServerLevel serverLevel;

    public static void init() {

        LifecycleEvent.SETUP.register(() -> {
            ExampleMod.LOGGER.info("Welcome to the CREATE RAILWAYS NAVIGATOR mod by MRJULSEN.");
        });

        LifecycleEvent.SERVER_LEVEL_LOAD.register((level) -> {
            ModExtras.register();
        });        

        LifecycleEvent.SERVER_STARTED.register((server) -> {
            TrainListener.start(server.overworld());
            serverLevel = server.overworld();
        });

        LifecycleEvent.SERVER_STOPPING.register((server) -> {
            TrainListener.stop();
        });

        TickEvent.SERVER_POST.register((server) -> {
            if (serverLevel != null) {
                long currentTicks = serverLevel.dayTime();
                if (Math.abs(currentTicks - lastTicks) > 1) {
                    serverLevel.players().stream().filter(p -> p instanceof ServerPlayer).forEach(x -> ExampleMod.net().CHANNEL.sendToPlayer(x, new TimeCorrectionPacket((int)(currentTicks - lastTicks))));
                }
                lastTicks = currentTicks;
            }
        });
    }
}


