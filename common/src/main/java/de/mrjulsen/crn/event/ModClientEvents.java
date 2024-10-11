package de.mrjulsen.crn.event;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.SavedRoutesManager;
import de.mrjulsen.crn.data.navigation.ClientTrainListener;
import de.mrjulsen.crn.event.events.DefaultTrainDataRefreshEvent;
import de.mrjulsen.crn.event.events.RouteDetailsActionsEvent;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.registry.ModDisplayTags;
import de.mrjulsen.crn.registry.ModExtras;
import de.mrjulsen.mcdragonlib.client.OverlayManager;
import de.mrjulsen.mcdragonlib.data.Single.MutableSingle;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;

public class ModClientEvents {

    private static int tickTime;

    private static int langCheckerTicks = 0;
    private static MutableSingle<Boolean> inGame = new MutableSingle<Boolean>(false);

    public static void init() {

        ClientLifecycleEvent.CLIENT_SETUP.register((mc) -> {
            ModKeys.init();
            ModDisplayTags.register();
        });

        ClientTickEvent.CLIENT_POST.register((mc) -> {
            langCheckerTicks++;

            if ((langCheckerTicks %= 20) == 0) {
                ClientWrapper.updateLanguage(ModClientConfig.LANGUAGE.get(), false);
            }

            if (!inGame.getFirst()) return;

            tickTime++;
            if ((tickTime %= 100) == 0) {
                ClientTrainListener.tick(() -> {
                    CRNEventsManager.getEvent(DefaultTrainDataRefreshEvent.class).run();
                });
            }
        });

        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register((level) -> {
            ModExtras.init();
        });

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register((player) -> {
            ClientWrapper.updateLanguage(ModClientConfig.LANGUAGE.get(), true);

            // Register Events
            CRNEventsManager.registerEvent(DefaultTrainDataRefreshEvent::new);
            CRNEventsManager.registerEvent(RouteDetailsActionsEvent::new);
            
            CRNEventsManager.getEvent(CRNClientEventsRegistryEvent.class).run();

            SavedRoutesManager.pull(true, null);

            inGame.setFirst(true);
        });

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((player) -> {
            inGame.setFirst(false);
            OverlayManager.clear();
            CreateRailwaysNavigator.LOGGER.info("Removed all overlays.");
            SavedRoutesManager.removeAllRoutes();
            CRNEventsManager.clearEvents();
            InstanceManager.removeRouteOverlay();
            ClientTrainListener.clear();
        });

        ClientGuiEvent.DEBUG_TEXT_LEFT.register((texts) -> {
            texts.add(String.format("CRN | RL: %s",
                ClientTrainListener.debug_registeredListenersCount()
            ));
        });
    }
}
