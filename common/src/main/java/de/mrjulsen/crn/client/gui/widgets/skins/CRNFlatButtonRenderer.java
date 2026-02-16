package de.mrjulsen.crn.client.gui.widgets.skins;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton.ButtonState;
import de.mrjulsen.mcdragonlib.client.gui.widgets.render.IStateRenderer;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;

public class CRNFlatButtonRenderer implements IStateRenderer<ButtonState> {

    public static final CRNFlatButtonRenderer INSTANCE = new CRNFlatButtonRenderer();
    
    @Override
    public void renderSprite(DLGuiGraphics graphics, int x, int y, int w, int h, DLGuiComponent component, ButtonState state) {
        switch (state) {
            case SELECTED -> GuiUtils.fill(graphics, x, y, w, h, DLColor.fromInt(0x44FFFFFF));
            case DOWN -> GuiUtils.fill(graphics, x, y, w, h, DLColor.fromInt(0x22000000));
            case DOWN_SELECTED -> GuiUtils.fill(graphics, x, y, w, h, DLColor.fromInt(0x22000000));
            default -> {}
        }
    }
}
