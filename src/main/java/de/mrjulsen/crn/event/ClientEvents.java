package de.mrjulsen.crn.event;

import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.Type;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import de.mrjulsen.crn.client.gui.overlay.HudOverlays;
import net.minecraftforge.api.distmarker.Dist;

@EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {
    
    @SubscribeEvent
	public static void onTick(ClientTickEvent event) {
        if (event.phase != Phase.END && event.side != LogicalSide.CLIENT && event.type != Type.CLIENT) {
            return;
        }
        HudOverlays.tick();
    }
}
