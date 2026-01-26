package de.mrjulsen.crn.client.gui.widgets.skins;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.ScrollBarState;
import de.mrjulsen.mcdragonlib.client.gui.widgets.render.IStateRenderer;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;

public class ModernScrollbarComponentRenderer implements IStateRenderer<ScrollBarState> {

    public static final ModernScrollbarComponentRenderer INSTANCE = new ModernScrollbarComponentRenderer();

    @Override
    public void renderSprite(DLGuiGraphics graphics, int x, int y, int w, int h, DLGuiComponent component, ScrollBarState state) {
        switch (state) {
            case BACKGROUND -> {
                if (component.isSelected()) {
                    GuiUtils.fill(graphics, x, y, w, h, DLColor.fromInt(0xFF444444));
                }
            }
            case SCROLLER_VERTICAL_DOWN_SELECTED -> {
                GuiUtils.fill(graphics, x, y, w, h, DLColor.fromInt(0x88FFFFFF));
            }
            case SCROLLER_VERTICAL_SELECTED -> {
                GuiUtils.fill(graphics, x, y, w, h, DLColor.fromInt(0x88FFFFFF));
            }
            case SCROLLER_VERTICAL_NORMAL -> {
                GuiUtils.fill(graphics, x + 2, y + 2, w - 4, h - 4, DLColor.fromInt(0x88FFFFFF));
            }
            case SCROLLER_HORIZONTAL_DOWN_SELECTED -> {
                GuiUtils.fill(graphics, x, y, w, h, DLColor.fromInt(0x88FFFFFF));
            }
            case SCROLLER_HORIZONTAL_SELECTED -> {
                GuiUtils.fill(graphics, x, y, w, h, DLColor.fromInt(0x88FFFFFF));
            }
            case SCROLLER_HORIZONTAL_NORMAL -> {
                GuiUtils.fill(graphics, x + 2, y + 2, w - 4, h - 4, DLColor.fromInt(0x88FFFFFF));
            }
            default -> {}
        };
    }
    
}
