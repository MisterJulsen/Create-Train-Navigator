package de.mrjulsen.crn.event;

import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.event.listeners.TrainListener;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.registry.ModExtras;
import de.mrjulsen.mcdragonlib.client.OverlayManager;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.client.Minecraft;

public class ClientEvents {

    @SuppressWarnings("resource")
    public static void init() {
        TickEvent.PLAYER_POST.register((mc) -> {
            JourneyListenerManager.tick();
        });

        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register((level) -> {
            ModExtras.register();
        });

        LifecycleEvent.SERVER_LEVEL_LOAD.register((server) -> {
            ModExtras.register();
        });

        PlayerEvent.PLAYER_JOIN.register((player) -> {
            JourneyListenerManager.start();
        });

        PlayerEvent.PLAYER_QUIT.register((player) -> {
            OverlayManager.remove(0);
            ExampleMod.LOGGER.info("Removed all overlays.");

            ClientTrainStationSnapshot.getInstance().dispose();        
            InstanceManager.clearAll();
            JourneyListenerManager.stop();
        });

        ClientGuiEvent.DEBUG_TEXT_LEFT.register((texts) -> {
            boolean b1 = TrainListener.getInstance() != null;
            boolean b2 = ClientTrainStationSnapshot.getInstance() != null;
            
            if (Minecraft.getInstance().options.renderDebug) {
                texts.add(String.format("CRN | T: %s/%s, JL: %s, O: %s, I: %s",
                    b1 ? TrainListener.getInstance().getListeningTrainCount() : (b2 ? ClientTrainStationSnapshot.getInstance().getListeningTrainCount() : 0),
                    b1 ? TrainListener.getInstance().getTotalTrainCount() : (b2 ? ClientTrainStationSnapshot.getInstance().getTrainCount() : 0),
                    JourneyListenerManager.getInstance() == null ? 0 : JourneyListenerManager.getInstance().getCacheSize(),
                    OverlayManager.count(),
                    InstanceManager.getInstancesCountString()
                ));
            }
        });
    }
}
