package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

public class FlatCheckBox extends DLCheckBox {
    
    public FlatCheckBox(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    public void setHeight(double height) {        
        super.setHeight(GuiIcons.ICON_SIZE);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (checked.get()) {
            GuiIcons.CHECKMARK.render(graphics, 0, 0);
        }
        GuiUtils.drawString(graphics, graphics.defaultFont(), GuiIcons.ICON_SIZE + 2, height() / 2 - graphics.defaultFont().lineHeight / 2, text.get(), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);

        if (isSelected()) {
            GuiUtils.fill(graphics, 0, 0, width(), height(), DLColor.fromInt(0x44FFFFFF));
        }
    }
}
