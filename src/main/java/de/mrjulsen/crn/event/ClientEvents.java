package de.mrjulsen.crn.event;

import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.overlay.HudOverlays;
import de.mrjulsen.crn.client.input.KeyBinding;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.event.listeners.TrainListener;
import de.mrjulsen.crn.network.InstanceManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
	public static void onTick(ClientTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }
        HudOverlays.tick();
        JourneyListenerManager.tick();
    }

    @SubscribeEvent
    public static void onWorldLeave(LoggingOut event) {
        int count = HudOverlays.removeAll();
        ModMain.LOGGER.info("Removed all " + count + " overlays.");

        ClientTrainStationSnapshot.getInstance().dispose();        
        InstanceManager.clearAll();
        JourneyListenerManager.stop();
    }

    @SubscribeEvent
    public static void onWorldJoin(LoggingIn event) {        
        JourneyListenerManager.start();
    }

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onDebugOverlay(CustomizeGuiOverlayEvent.DebugText event) {
        boolean b1 = TrainListener.getInstance() != null;
        boolean b2 = ClientTrainStationSnapshot.getInstance() != null;
        
        if (Minecraft.getInstance().options.renderDebug) {
            event.getLeft().add(String.format("CRN | T: %s/%s, JL: %s, O: %s, I: %s",
                b1 ? TrainListener.getInstance().getListeningTrainCount() : (b2 ? ClientTrainStationSnapshot.getInstance().getListeningTrainCount() : 0),
                b1 ? TrainListener.getInstance().getTotalTrainCount() : (b2 ? ClientTrainStationSnapshot.getInstance().getTrainCount() : 0),
                JourneyListenerManager.getInstance() == null ? 0 : JourneyListenerManager.getInstance().getCacheSize(),
                HudOverlays.overlayCount(),
                InstanceManager.getInstancesCountString()
            ));
        }
    }

    @Mod.EventBusSubscriber(modid = ModMain.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void registerGuiOverlay(final RegisterGuiOverlaysEvent event) {        
            event.registerAboveAll("route_details_overlay", HudOverlays.HUD_ROUTE_DETAILS);
        }

        
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeyBinding.OPEN_SETTINGS_KEY);
        }
    }
}
