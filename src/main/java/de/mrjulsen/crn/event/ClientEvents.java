package de.mrjulsen.crn.event;

import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.overlay.HudOverlays;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.event.listeners.TrainListener;
import de.mrjulsen.crn.network.InstanceManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedInEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

    private static long lastTicks = 0;

    @SuppressWarnings("resource")
    @SubscribeEvent
	public static void onTick(ClientTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }
        HudOverlays.tick();
        JourneyListenerManager.tick();

        if (JourneyListenerManager.hasInstance() && Minecraft.getInstance().level != null) {
            long currentTicks = Minecraft.getInstance().level.dayTime();
            if (Math.abs(currentTicks - lastTicks) > 1)  {
                JourneyListenerManager.getInstance().getAllListeners().forEach(x -> x.getListeningRoute().shiftTime((int)(currentTicks - lastTicks)));
            }
            lastTicks = currentTicks;
        }
    }

    @SubscribeEvent
    public static void onWorldLeave(LoggedOutEvent event) {
        int count = HudOverlays.removeAll();
        ModMain.LOGGER.info("Removed all " + count + " overlays.");

        ClientTrainStationSnapshot.getInstance().dispose();        
        InstanceManager.clearAll();
        JourneyListenerManager.stop();
    }

    @SubscribeEvent
    public static void onWorldJoin(LoggedInEvent event) {        
        JourneyListenerManager.start();
    }

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onDebugOverlay(RenderGameOverlayEvent.Text event) {
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
}
