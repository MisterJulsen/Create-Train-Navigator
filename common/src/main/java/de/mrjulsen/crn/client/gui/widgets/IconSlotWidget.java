package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLRenderable;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.util.DLUtils;

public class IconSlotWidget extends DLRenderable {
    
    private Sprite icon;

    public IconSlotWidget(int x, int y, Sprite icon) {
        super(x, y, 18, 18);
        this.icon = icon;
    }

    public void setIcon(Sprite icon) {
        this.icon = icon;
    }

    public Sprite getIcon() {
        return icon;
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        CreateDynamicWidgets.renderIconSlot(graphics, x(), y(), width(), height());
        DLUtils.doIfNotNull(icon, x -> x.render(graphics, x() + (width() / 2 - icon.getWidth() / 2), y() + (height() / 2 - icon.getHeight() / 2)));
    }
}
