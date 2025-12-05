package de.mrjulsen.crn.client.gui.widgets.vanilla;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ResizableButton extends Button {

    public ResizableButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        DLGuiGraphics graphics = new DLGuiGraphics(guiGraphics, guiGraphics.pose(), Minecraft.getInstance().font, partialTick);
        //DynamicGuiRenderer.renderArea(graphics, GuiAreaDefinition.of(this), AreaStyle.NATIVE, isActive() ? (isHoveredOrFocused() ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
        DLColor j = isActive() ? DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR : DragonLib.VANILLA_BUTTON_DISABLED_FONT_COLOR;
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, this.getMessage(), j, ETextAlignment.CENTER, true);
    }
}
