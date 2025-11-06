package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.widgets.renderers.CreateTextBoxRenderer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextEditBox;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;

public class DLCreateTextBox extends DLRichTextEditBox {

    public final BooleanProperty drawArrow = new BooleanProperty(false, false);

    public DLCreateTextBox(int x, int y, int w, int h) {
        super(x, y, w, h);
        this.componentRenderer.set(CreateTextBoxRenderer.INSTANCE);
    }

    protected boolean renderArrow;
    

    public DLCreateTextBox setRenderArrow(boolean b) {
        this.renderArrow = b;
        return this;
    }

    public boolean shouldRenderArrow() {
        return renderArrow;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (shouldRenderArrow()) CreateDynamicWidgets.renderTextBoxArrow(graphics, x() - 5, y() - 5);
    }
}
