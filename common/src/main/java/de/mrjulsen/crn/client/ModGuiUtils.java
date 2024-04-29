package de.mrjulsen.crn.client;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public class ModGuiUtils {

    private static ResourceLocation blankTextureLocation;

    public static ResourceLocation getBlankTexture() {
        if (blankTextureLocation == null) {
            NativeImage img = new NativeImage(1, 1, false);
            img.setPixelRGBA(0, 0, 0xFFFFFFFF);
            blankTextureLocation = Minecraft.getInstance().getTextureManager().register("blank", new DynamicTexture(img));
        }

        return blankTextureLocation;
    }
	
    /**
	 * @see https://github.com/Creators-of-Create/Create/blob/mc1.18/dev/src/main/java/com/simibubi/create/content/trains/schedule/ScheduleScreen.java
	 */
    public static void startStencil(Graphics graphics, float x, float y, float w, float h) {
		RenderSystem.clear(GL30.GL_STENCIL_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

		GL11.glDisable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilMask(~0);
		RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilOp(GL11.GL_REPLACE, GL11.GL_KEEP, GL11.GL_KEEP);
		RenderSystem.stencilMask(0xFF);
		RenderSystem.stencilFunc(GL11.GL_NEVER, 1, 0xFF);

		graphics.poseStack().pushPose();
		graphics.poseStack().translate(x, y, 0);
		graphics.poseStack().scale(w, h, 1);
		GuiUtils.fillGradient(graphics, 0, 0, -100, 1, 1, 0xff000000, 0xff000000);
		graphics.poseStack().popPose();

		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
	}

	/**
	 * @see https://github.com/Creators-of-Create/Create/blob/mc1.18/dev/src/main/java/com/simibubi/create/content/trains/schedule/ScheduleScreen.java
	 */
	public static void endStencil() {
		GL11.glDisable(GL11.GL_STENCIL_TEST);
	}
	
}
