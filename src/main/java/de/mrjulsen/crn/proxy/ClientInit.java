package de.mrjulsen.crn.proxy;

import com.mojang.blaze3d.platform.NativeImage;

import de.mrjulsen.crn.registry.ModDisplayTags;
import de.mrjulsen.crn.util.ModGuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientInit {

    public static ResourceLocation blankTextureLocation;

    @SuppressWarnings("resource")
    public static void setup(final FMLClientSetupEvent event) {        
        ModGuiUtils.init();
        ModDisplayTags.register();

        NativeImage img = new NativeImage(1, 1, false);
        img.setPixelRGBA(0, 0, 0xFFFFFFFF);
        blankTextureLocation = Minecraft.getInstance().textureManager.register("blank", new DynamicTexture(img));
    }
}
