package de.mrjulsen.crn.proxy;

import com.mojang.blaze3d.platform.NativeImage;

import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.registry.ModDisplayTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public class ClientInit {

    private static ResourceLocation blankTextureLocation;

    public static void setup(Minecraft minecraft) {        
        ModKeys.init();
        //OverlayRegistry.registerOverlayBottom("route_details_overlay", HudOverlays.HUD_ROUTE_DETAILS);
        ModDisplayTags.register();
    }

    public static ResourceLocation getBlankTexture() {
        if (blankTextureLocation == null) {
            NativeImage img = new NativeImage(1, 1, false);
            img.setPixelRGBA(0, 0, 0xFFFFFFFF);
            blankTextureLocation = Minecraft.getInstance().getTextureManager().register("blank", new DynamicTexture(img));
        }

        return blankTextureLocation;
    }
}
