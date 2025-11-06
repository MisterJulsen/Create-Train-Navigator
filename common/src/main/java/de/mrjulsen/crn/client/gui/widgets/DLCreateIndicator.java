package de.mrjulsen.crn.client.gui.widgets;

import com.simibubi.create.foundation.gui.AllGuiTextures;

import de.mrjulsen.mcdragonlib.client.gui.properties.Property;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

public class DLCreateIndicator extends DLGuiComponent {
    
	public enum State {
		OFF, ON,
		RED, YELLOW, GREEN;
	}

    public final Property<State> state = new Property<DLCreateIndicator.State>(State.OFF);

    public DLCreateIndicator(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (!visible.get())
			return;

		AllGuiTextures toDraw;
		switch (state.get()) {
			case ON: toDraw = AllGuiTextures.INDICATOR_WHITE; break;
			case OFF: toDraw = AllGuiTextures.INDICATOR; break;
			case RED: toDraw = AllGuiTextures.INDICATOR_RED; break;
			case YELLOW: toDraw = AllGuiTextures.INDICATOR_YELLOW; break;
			case GREEN: toDraw = AllGuiTextures.INDICATOR_GREEN; break;
			default: toDraw = AllGuiTextures.INDICATOR; break;
		}
		toDraw.render(graphics.graphics(), 0, 0);
    }
}
