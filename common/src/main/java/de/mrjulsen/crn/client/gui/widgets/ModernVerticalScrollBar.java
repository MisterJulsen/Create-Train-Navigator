package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.client.ITickable;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLVerticalScrollBar;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import net.minecraft.client.gui.screens.Screen;

public class ModernVerticalScrollBar extends DLVerticalScrollBar implements ITickable {

    private final Screen parent;

    public ModernVerticalScrollBar(Screen parent, int x, int y, int h, GuiAreaDefinition scrollArea) {
        super(x, y, 5, h, null);
        this.parent = parent;
        setAutoScrollerSize(true);
        set_width(5);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        // Render background
        if (isMouseSelected() || (parent.isDragging() && isScrolling)) {
            GuiUtils.fill(graphics, x(), y(), width(), height(), 0xFF444444);
        }

        // Render scrollbar
        int x1 = x() + (isMouseSelected() || (parent.isDragging() && isScrolling) ? 0 : 2);
        int y1 = y() + (int)(scrollPercentage * (height - scrollerSize));
        int w = isMouseSelected() || (parent.isDragging() && isScrolling) ? width : 1;
        int h = scrollerSize;

        if (canScroll()) {
            GuiUtils.fill(graphics, x1, y1 + (isMouseSelected() || (parent.isDragging() && isScrolling) ? 0 : 2), w, h - (isMouseSelected() || (parent.isDragging() && isScrolling) ? 0 : 4), 0x88FFFFFF);
        }
    }

    @Override
    public void tick() {
        if (!parent.isDragging()) {
            isScrolling = false;
        }
    }
    
}
