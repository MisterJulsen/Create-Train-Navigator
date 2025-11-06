package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.gui.screens.Screen;

public class ModernVerticalScrollBar extends DLScrollBar {

    private final Screen parent;

    public ModernVerticalScrollBar(Screen parent, int x, int y, int h) {
        super(x, y, 5, h, null);
        this.parent = parent;
        this.scrollerSize.set(0);
        setWidth(5);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        // Render background
        if (isSelected() || (parent.isDragging() && isDragged())) {
            GuiUtils.fill(graphics, x(), y(), width(), height(), DLColor.fromInt(0xFF444444));
        }

        // Render scrollbar
        int x1 = (isSelected() || (parent.isDragging() && isDragged()) ? 0 : 2);
        int y1 = (int)(value.get() * (height() - scrollerSize.get()));
        int w = isSelected() || (parent.isDragging() && isDragged()) ? width() : 1;
        int h = scrollerSize.get();

        if (enabled.get()) {
            GuiUtils.fill(graphics, x1, y1 + (isSelected() || (parent.isDragging() && isDragged()) ? 0 : 2), w, h - (isSelected() || (parent.isDragging() && isDragged()) ? 0 : 4), DLColor.fromInt(0x88FFFFFF));
        }
    }    
}
