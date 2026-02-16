package de.mrjulsen.crn.client.gui.widgets.create;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.Indicator;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.Property;

public class CreateIndicator extends DLGuiComponent {

    private final Indicator indicator = new Indicator(0, 0, TextUtils.empty());

    public final Property<Indicator.State> state = new Property<Indicator.State>(Indicator.State.OFF)
        .withAfterPropertyChangedCallback((o, n) -> {
            indicator.state = n;
        });

    public CreateIndicator(int x, int y) {
        super(x, y, AllGuiTextures.INDICATOR.getWidth(), AllGuiTextures.INDICATOR.getHeight());
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        indicator.render(graphics.graphics(), (int)mouseX, (int)mouseY, graphics.partialTick());
    }
    
}
