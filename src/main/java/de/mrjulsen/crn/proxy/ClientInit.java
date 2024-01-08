package de.mrjulsen.crn.proxy;

import de.mrjulsen.crn.client.gui.overlay.HudOverlays;
import de.mrjulsen.crn.util.GuiUtils;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientInit {
    public static void setup(final FMLClientSetupEvent event) {        
        GuiUtils.init();

        OverlayRegistry.registerOverlayTop("route_details_overlay", HudOverlays.HUD_ROUTE_DETAILS);
    }
}
