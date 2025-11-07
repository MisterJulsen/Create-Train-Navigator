package de.mrjulsen.crn.event;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.SavedRoutesManager;
import de.mrjulsen.crn.data.navigation.ClientTrainListener;
import de.mrjulsen.crn.data.train.DepartureHistory;
import de.mrjulsen.crn.event.events.DefaultTrainDataRefreshEvent;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.registry.ModExtras;
import de.mrjulsen.mcdragonlib.util.Holder.MutableHolder;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;

public class ModClientEvents {

    private static int tickTime;

    private static int langCheckerTicks = 0;
    private static MutableHolder<Boolean> inGame = new MutableHolder<Boolean>(false);

    public static void init() {

        ClientLifecycleEvent.CLIENT_SETUP.register((mc) -> {
            ModKeys.init();
        });

        ClientTickEvent.CLIENT_POST.register((mc) -> {
            langCheckerTicks++;

            if ((langCheckerTicks %= 20) == 0) {
                ClientWrapper.updateLanguage(ModClientConfig.LANGUAGE.get(), false);
            }

            if (!inGame.get()) return;

            tickTime++;
            if ((tickTime %= 100) == 0) {
                ClientTrainListener.tick(() -> {
                    CRNEventsManager.getEvent(DefaultTrainDataRefreshEvent.class).run();
                });
            }
        });

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register((player) -> {
            ClientWrapper.updateLanguage(ModClientConfig.LANGUAGE.get(), true);

            // Register Events
            CRNEventsManager.registerEvent(DefaultTrainDataRefreshEvent::new);
            //CRNEventsManager.registerEvent(RouteDetailsActionsEvent::new);
            
            CRNEventsManager.getEvent(CRNClientEventsRegistryEvent.class).run();

            SavedRoutesManager.pull(true, null);

            inGame.set(true);
        });

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((player) -> {
            inGame.set(false);
            CreateRailwaysNavigator.LOGGER.info("Removed all overlays.");
            SavedRoutesManager.removeAllRoutes();
            CRNEventsManager.clearEvents();
            InstanceManager.removeRouteOverlay();
            ClientTrainListener.clear();
        });

        ClientGuiEvent.DEBUG_TEXT_LEFT.register((texts) -> {
            if (ModCommonConfig.ADVANCED_LOGGING.get()) {
                texts.add(String.format("CRN | RL: %s, DH: %s",
                    ClientTrainListener.debug_registeredListenersCount(),
                    DepartureHistory.debug_dataCount()
                ));
            }
        });
    }
}
