package de.mrjulsen.crn.event;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.RealtimeTrains;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.SavedRoutesManager;
import de.mrjulsen.crn.data.navigation.ClientTrainListener;
import de.mrjulsen.crn.event.events.DefaultTrainDataRefreshEvent;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.mcdragonlib.util.Holder.MutableHolder;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;

public class ModClientEvents {

    private static int tickTime;

    private static int langCheckerTicks = 0;

    public static void init() {

        ClientLifecycleEvent.CLIENT_SETUP.register((mc) -> {
            ModKeys.init();
        });

        ClientTickEvent.CLIENT_LEVEL_POST.register((mc) -> {
            langCheckerTicks++;

            if ((langCheckerTicks %= 20) == 0) {
                ClientWrapper.updateLanguage(ModClientConfig.LANGUAGE.get(), false);
            }

            tickTime++;
            if ((tickTime %= 100) == 0) {
                ClientTrainListener.tick(() -> {
                    CRNEventsManager.getEvent(DefaultTrainDataRefreshEvent.class).run();
                });
                RealtimeTrains.tick();
            }
        });

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register((player) -> {
            ClientWrapper.updateLanguage(ModClientConfig.LANGUAGE.get(), true);

            // Register Events
            CRNEventsManager.registerEvent(DefaultTrainDataRefreshEvent::new);
            //CRNEventsManager.registerEvent(RouteDetailsActionsEvent::new);
            
            CRNEventsManager.getEvent(CRNClientEventsRegistryEvent.class).run();

            SavedRoutesManager.pull(true, null);
        });

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((player) -> {
            CreateRailwaysNavigator.LOGGER.info("Removed all overlays.");
            SavedRoutesManager.removeAllRoutes();
            CRNEventsManager.clearEvents();
            InstanceManager.removeRouteOverlay();
            ClientTrainListener.clear();
            RealtimeTrains.clear();
        });

        ClientGuiEvent.DEBUG_TEXT_LEFT.register((texts) -> {
            if (ModCommonConfig.ADVANCED_LOGGING.get()) {
                texts.add(String.format("CRN | RL: %s",
                    ClientTrainListener.debug_registeredListenersCount()
                ));
            }
        });
    }
}
