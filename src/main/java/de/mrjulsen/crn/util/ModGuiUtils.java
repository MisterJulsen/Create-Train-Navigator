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

import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;

public class ModGuiUtils {
	
	/**
	 * @see https://github.com/Creators-of-Create/Create/blob/mc1.18/dev/src/main/java/com/simibubi/create/foundation/gui/UIRenderHelper.java
	 */
	protected static CustomRenderTarget framebuffer;

	/**
	 * @see https://github.com/Creators-of-Create/Create/blob/mc1.18/dev/src/main/java/com/simibubi/create/foundation/gui/UIRenderHelper.java
	 */
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
	 * @see https://github.com/Creators-of-Create/Create/blob/mc1.18/dev/src/main/java/com/simibubi/create/foundation/gui/UIRenderHelper.java
	 */
	public static void swapAndBlitColor(RenderTarget src, RenderTarget dst) {
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.frameBufferId);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.frameBufferId);
		GlStateManager._glBlitFrameBuffer(0, 0, src.viewWidth, src.viewHeight, 0, 0, dst.viewWidth, dst.viewHeight, GL30.GL_COLOR_BUFFER_BIT, GL20.GL_LINEAR);

		GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, dst.frameBufferId);
	}

	/**
	 * @see https://github.com/Creators-of-Create/Create/blob/mc1.18/dev/src/main/java/com/simibubi/create/content/trains/schedule/ScheduleScreen.java
	 */
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

	/**
	 * @see https://github.com/Creators-of-Create/Create/blob/mc1.18/dev/src/main/java/com/simibubi/create/content/trains/schedule/ScheduleScreen.java
	 */
	public static void endStencil() {
		GL11.glDisable(GL11.GL_STENCIL_TEST);
	}

	@SuppressWarnings("resource")
	public static <T extends FormattedText> boolean renderTooltipAtFixedPos(Screen screen, GuiAreaDefinition area, List<T> lines, int maxWidth, PoseStack stack, int mouseX, int mouseY, int xOffset, int yOffset, int xPos, int yPos) {
		if (area.isInBounds((double)(mouseX + xOffset), (double)(mouseY + yOffset))) {
			screen.renderComponentTooltip(stack, GuiUtils.getTooltipData(screen, lines, maxWidth), xPos - 8, yPos + 16, screen.getMinecraft().font);
			return true;
		} else {
			return false;
		}
   	}

	public static void playButtonSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

	public static int applyTint(int color, int tint) {
        int originalRed = (color >> 16) & 0xFF;
        int originalGreen = (color >> 8) & 0xFF;
        int originalBlue = color & 0xFF;

        int tintRed = (tint >> 16) & 0xFF;
        int tintGreen = (tint >> 8) & 0xFF;
        int tintBlue = tint & 0xFF;

        int mixedRed = (originalRed + tintRed) / 2;
        int mixedGreen = (originalGreen + tintGreen) / 2;
        int mixedBlue = (originalBlue + tintBlue) / 2;

        return 0xFF000000 | (mixedRed << 16) | (mixedGreen << 8) | mixedBlue;
    }

	public static int lightenColor(int color, float factor) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        red = (int) ((red * (1 - factor)) + (255 * factor));
        green = (int) ((green * (1 - factor)) + (255 * factor));
        blue = (int) ((blue * (1 - factor)) + (255 * factor));

        red = Math.min(255, Math.max(0, red));
        green = Math.min(255, Math.max(0, green));
        blue = Math.min(255, Math.max(0, blue));

        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
}
