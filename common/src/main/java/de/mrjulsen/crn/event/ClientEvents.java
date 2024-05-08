package de.mrjulsen.crn.event;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.event.listeners.TrainListener;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.registry.ModDisplayTags;
import de.mrjulsen.crn.registry.ModExtras;
import de.mrjulsen.mcdragonlib.client.OverlayManager;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

public class ClientEvents {

    private static int langCheckerTicks = 0;

    @SuppressWarnings("resource")
    public static void init() {

        ClientLifecycleEvent.CLIENT_SETUP.register((mc) -> {
            ModKeys.init();
            ModDisplayTags.register();
        });

        ClientTickEvent.CLIENT_POST.register((mc) -> {
            JourneyListenerManager.tick();
            langCheckerTicks++;

            if ((langCheckerTicks %= 20) == 0) {
                ClientWrapper.updateLanguage(ModClientConfig.LANGUAGE.get());
            }
        });

        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register((level) -> {
            ModExtras.register();
        });

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register((player) -> {
            JourneyListenerManager.start();
        });

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((player) -> {
            InstanceManager.removeRouteOverlay();
            CreateRailwaysNavigator.LOGGER.info("Removed all overlays.");

            ClientTrainStationSnapshot.getInstance().dispose();        
            InstanceManager.clearAll();
            JourneyListenerManager.stop();
            GlobalSettingsManager.close();
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
