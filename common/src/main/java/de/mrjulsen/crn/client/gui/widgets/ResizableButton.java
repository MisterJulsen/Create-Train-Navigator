package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.ButtonState;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ResizableButton extends Button {
    
    public ResizableButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }
    
    @SuppressWarnings("resource")
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Graphics graphics = new Graphics(guiGraphics, guiGraphics.pose());
        DynamicGuiRenderer.renderArea(graphics, GuiAreaDefinition.of(this), AreaStyle.NATIVE, isActive() ? (isHoveredOrFocused() ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
        int j = isActive() ? DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED;
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, this.getMessage(), j, EAlignment.CENTER, true);
    }
}
