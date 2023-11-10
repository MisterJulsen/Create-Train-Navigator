package de.mrjulsen.crn.util;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.UIRenderHelper.CustomRenderTarget;

import de.mrjulsen.crn.client.gui.GuiAreaDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;

public class GuiUtils {
	
	protected static CustomRenderTarget framebuffer;

	public static void init() {
		RenderSystem.recordRenderCall(() -> {
			Window mainWindow = Minecraft.getInstance().getWindow();
			framebuffer = CustomRenderTarget.create(mainWindow);
		});
	}

	public static CustomRenderTarget getFramebuffer() {
		return framebuffer;
	}

	/**
	 * Switch from src to dst, after copying the contents of src to dst.
	 */
	public static void swapAndBlitColor(RenderTarget src, RenderTarget dst) {
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.frameBufferId);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.frameBufferId);
		GlStateManager._glBlitFrameBuffer(0, 0, src.viewWidth, src.viewHeight, 0, 0, dst.viewWidth, dst.viewHeight, GL30.GL_COLOR_BUFFER_BIT, GL20.GL_LINEAR);

		GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, dst.frameBufferId);
	}

    public static void startStencil(PoseStack matrixStack, float x, float y, float w, float h) {
		RenderSystem.clear(GL30.GL_STENCIL_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

		GL11.glDisable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilMask(~0);
		RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilOp(GL11.GL_REPLACE, GL11.GL_KEEP, GL11.GL_KEEP);
		RenderSystem.stencilMask(0xFF);
		RenderSystem.stencilFunc(GL11.GL_NEVER, 1, 0xFF);

		matrixStack.pushPose();
		matrixStack.translate(x, y, 0);
		matrixStack.scale(w, h, 1);
		net.minecraftforge.client.gui.GuiUtils.drawGradientRect(matrixStack.last()
			.pose(), -100, 0, 0, 1, 1, 0xff000000, 0xff000000);
		matrixStack.popPose();

		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
	}

	public static void endStencil() {
		GL11.glDisable(GL11.GL_STENCIL_TEST);
	}

	@SuppressWarnings("resource")
    public static <W extends AbstractWidget> boolean renderTooltip(Screen s, W w, List<FormattedCharSequence> lines, PoseStack stack, int mouseX, int mouseY, int xOffset, int yOffset) {
        if (w.isMouseOver(mouseX + xOffset, mouseY + yOffset)) {
            s.renderTooltip(stack, lines, mouseX, mouseY, s.getMinecraft().font);
			return true;
        }
		return false;
    }

	@SuppressWarnings("resource")
    public static boolean renderTooltip(Screen s, GuiAreaDefinition w, List<FormattedCharSequence> lines, PoseStack stack, int mouseX, int mouseY, int xOffset, int yOffset) {
        if (w.isInBounds(mouseX + xOffset, mouseY + yOffset)) {
            s.renderTooltip(stack, lines, mouseX, mouseY, s.getMinecraft().font);
			return true;
        }
		return false;
    }
}
