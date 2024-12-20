package de.mrjulsen.crn.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class DLCreateTextBox extends DLEditBox {

    protected boolean renderArrow;
    
    public DLCreateTextBox(Font pFont, int pX, int pY, int pWidth, Component pMessage) {
        super(pFont, pX + 5, pY + 5, pWidth - 10, 13, pMessage);
        this.setBordered(false);
    }

    public DLCreateTextBox setRenderArrow(boolean b) {
        this.renderArrow = b;
        return this;
    }

    public boolean shouldRenderArrow() {
        return renderArrow;
    }

    @Override
    public void renderButton(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
        Graphics graphics = new Graphics(ms);
        CreateDynamicWidgets.renderTextBox(graphics, x() - 5, y() - 5, width() + 10);
        if (shouldRenderArrow()) CreateDynamicWidgets.renderTextBoxArrow(graphics, x() - 5, y() - 5);
        super.renderButton(ms, mouseX, mouseY, partialTicks);
    }
}
