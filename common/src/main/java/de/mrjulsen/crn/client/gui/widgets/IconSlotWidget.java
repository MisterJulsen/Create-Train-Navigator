package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.Property;

public class IconSlotWidget extends DLGuiComponent {
    
    public static final int WIDTH = 18;
    public static final int HEIGHT = 18;
    
    public final Property<DLSprite> icon = new Property<>(DLSprite.empty());

    public IconSlotWidget(int x, int y) {
        super(x, y, 18, 18);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderIconSlot(graphics, 0, 0, width(), height());
        DLUtils.doIfNotNull(icon.get(), x -> x.render(graphics, (width() / 2 - x.getWidth() / 2), (height() / 2 - x.getHeight() / 2)));
    }
}
