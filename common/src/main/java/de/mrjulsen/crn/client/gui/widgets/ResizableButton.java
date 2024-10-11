package de.mrjulsen.crn.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.ButtonState;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ResizableButton extends Button {

    public ResizableButton(int x, int y, int width, int height, Component message, OnPress onPress, OnTooltip onTooltip) {
        super(x, y, width, height, message, onPress, onTooltip);
    }
    
    public ResizableButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, NO_TOOLTIP);
    }
    
    @SuppressWarnings("resource")
    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        Graphics graphics = new Graphics(poseStack);
        DynamicGuiRenderer.renderArea(graphics, GuiAreaDefinition.of(this), AreaStyle.NATIVE, isActive() ? (isHoveredOrFocused() ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
        int j = isActive() ? DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED;
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, this.x + this.width / 2, this.y + (this.height - 8) / 2, this.getMessage(), j, EAlignment.CENTER, true);
    }
}
