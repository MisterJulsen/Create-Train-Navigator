package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;

public class FlatCheckBox extends DLCheckBox {

    public FlatCheckBox(int pX, int pY, int pWidth, String pMessage, boolean checked, Consumer<DLCheckBox> onCheckedChanged) {
        super(pX, pY, pWidth, pMessage, checked, onCheckedChanged);
        set_height(GuiIcons.ICON_SIZE);
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (isChecked()) {
            GuiIcons.CHECKMARK.render(graphics, x(), y());
        }
        GuiUtils.drawString(graphics, font, x() + GuiIcons.ICON_SIZE + 2, y() + height() / 2 - font.lineHeight / 2, getMessage(), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);

        if (isMouseSelected()) {
            GuiUtils.fill(graphics, x(), y(), width(), height(), 0x44FFFFFF);
        }
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
    }
}
