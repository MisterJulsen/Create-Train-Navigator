package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.flyout.FlyoutColorPickerWidget;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.render.DefaultGuiTextures;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

public class ColorSlotWidget extends DLButton {

    public static final int SLOT_SIZE = 18;

    private DLColor selectedColor;
    
    public ColorSlotWidget(int x, int y, DLColor color, DLColor[] defaultColors, boolean allowCustom, boolean allowNone, Consumer<DLColor> onColorSelected) {
        super(x, y, CreateButton.WIDTH, CreateButton.HEIGHT);
        this.selectedColor = color;

        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal((mgr) -> new FlyoutColorPickerWidget(mgr, this, FlyoutPointer.RIGHT, ColorShade.DARK, color, defaultColors, 8, allowCustom, allowNone, (col) -> {
                this.selectedColor = col;
                onColorSelected.accept(col);
            }));
            return false;
        });
    }

    public DLColor getSelectedColor() {
        return selectedColor;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        DefaultGuiTextures.DRAGONLIB_UI.getSprite("slot").render(graphics, 0, 0, width(), height());
        GuiUtils.fill(graphics, 1, 1, width() - 2, height() - 2, selectedColor);
        if (isSelected()) {
            GuiUtils.fill(graphics, 1, 1, width() - 2, height() - 2, DLColor.fromInt(0x40FFFFFF));
        }
    }
}
