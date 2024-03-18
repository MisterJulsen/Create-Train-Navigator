package de.mrjulsen.crn.proxy;

import de.mrjulsen.crn.client.gui.overlay.HudOverlays;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.registry.ModDisplayTags;
import de.mrjulsen.crn.util.ModGuiUtils;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientInit {
    public static void setup(final FMLClientSetupEvent event) {        
        ModGuiUtils.init();
        ModKeys.init();
        OverlayRegistry.registerOverlayBottom("route_details_overlay", HudOverlays.HUD_ROUTE_DETAILS);
        //BlockEntityRenderers.register(ModBlockEntities.ADVANCED_DISPLAY_BLOCK_ENTITY.get(), StaticBlockEntityRenderer::new);
        ModDisplayTags.register();
    }
}
