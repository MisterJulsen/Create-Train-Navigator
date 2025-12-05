package de.mrjulsen.crn.client.gui.widgets.create;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLItemPicker;
import de.mrjulsen.mcdragonlib.client.gui.widgets.render.IStateRenderer;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;

public class CreateItemPickerRenderer implements IStateRenderer<DLItemPicker.ItemPickerState> {

    public static final CreateItemPickerRenderer INSTANCE = new CreateItemPickerRenderer();

    @Override
    public void renderSprite(DLGuiGraphics graphics, int x, int y, int w, int h, DLGuiComponent component, DLItemPicker.ItemPickerState state) {
        CreateDynamicWidgets.renderTextBox(graphics, x, y, w);
    }
    
}
