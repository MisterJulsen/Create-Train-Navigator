package de.mrjulsen.crn.client.gui.flyout;

import java.util.function.Consumer;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.ColorPickerWidget;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.util.DLColor;

public class FlyoutColorPickerWidget extends AbstractFlyoutWidget {

    private final ColorPickerWidget colorPicker;

    public FlyoutColorPickerWidget(DLWindowManager manager, DLGuiComponent parentComponent, FlyoutPointer pointer, ColorShade pointerShade, DLColor initialColor, DLColor[] colors, int colorsPerLine, boolean allowCustom, boolean allowNone, Consumer<DLColor> onAccept) {
        super(manager, parentComponent, 1, 60, pointer, pointerShade);
        colorPicker = addComponent(new ColorPickerWidget(10, 10, colors, colorsPerLine, initialColor, allowCustom, allowNone, (color) -> {
            onAccept.accept(color);
            if (!isClosed()) {
                close();
            }
        }));
        setWidth(colorPicker.width() + 20);
        setHeight(colorPicker.height() + 20);
    }

    @Override
    protected void onOpen() {
    }
    
    @Override
    protected void onClose() {
    }
    
}
