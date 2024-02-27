package de.mrjulsen.crn.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public interface IForegroundRendering {
    void renderForeground(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTicks);
}
