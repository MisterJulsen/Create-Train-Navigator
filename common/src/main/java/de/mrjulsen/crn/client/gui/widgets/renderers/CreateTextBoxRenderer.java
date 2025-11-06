package de.mrjulsen.crn.client.gui.widgets.renderers;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextEditBox.TextBoxState;
import de.mrjulsen.mcdragonlib.client.gui.widgets.render.IStateRenderer;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;

public class CreateTextBoxRenderer implements IStateRenderer<TextBoxState> {

    public static final CreateTextBoxRenderer INSTANCE = new CreateTextBoxRenderer();
    
    public CreateTextBoxRenderer() {
    }

    @Override
    public void renderSprite(DLGuiGraphics graphics, int x, int y, int w, int h, DLGuiComponent component, TextBoxState state) {
        CreateDynamicWidgets.renderTextBox(graphics, x, y, w);
    }
}
