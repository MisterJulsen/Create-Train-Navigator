package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;
import java.util.function.Supplier;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.builtin.DLColorPickerWindow;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

public class ColorPickerWidget extends DLGuiComponent {

    private DLColor selectedColor = DLColor.TRANSPARENT;

    public ColorPickerWidget(int px, int py, DLColor[] sampleColors, int maxColorsPerLine, DLColor preselectedColor, boolean allowCustom, boolean allowNone, Consumer<DLColor> onAccept) {
        super(px, py, 1, 1);
        this.selectedColor = preselectedColor;

        int lines = (int)Math.ceil((double)sampleColors.length / (double)maxColorsPerLine);
        setHeight(18 * ((allowCustom ? 1 : 0) + (allowNone ? 1 : 0)) + lines * 13 - 1);
        setWidth(maxColorsPerLine * 13 - 1);
        int currentY = 0;
        if (allowCustom) {
            addComponent(new ColorBrowserButton(0, currentY, width(), () -> selectedColor, (btn) -> {
                getWindowManager().createModal(mgr -> new DLColorPickerWindow(mgr, false, selectedColor.withAlpha(255), (a) -> {
                    onAccept.accept(a);
                }));
            }));
            currentY += 18;
        }
        for (int i = 0, y = 0; y < lines && i < sampleColors.length; y++) {
            for (int x = 0; x < maxColorsPerLine && i < sampleColors.length; x++, i++) {
                final int j = i;
                addComponent(new ColorButton(x * 13, currentY + y * 13, sampleColors[j], btn -> {
                    onAccept.accept(sampleColors[j]);
                }));
            }
        }
        if (allowNone) {
            addComponent(new NoColorButton(0, height() - 16, width(), (btn) -> {
                onAccept.accept(DLColor.TRANSPARENT);
            }));
        }
    }

    public DLColor getSelectedColor() {
        return selectedColor;
    }
    

    private static class ColorBrowserButton extends DLButton {

        private final Supplier<DLColor> color;

        public ColorBrowserButton(int pX, int pY, int pWidth, Supplier<DLColor> color, Consumer<ColorBrowserButton> pOnPress) {
            super(pX, pY, pWidth, 16);
            this.color = color;
            addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                pOnPress.accept(this);
                return false;
            });
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            GuiUtils.drawBox(graphics, 0, 0, width(), height(), color.get(), isSelected() ? DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR : DragonLib.VANILLA_BUTTON_DISABLED_FONT_COLOR);
            GuiUtils.drawString(graphics, graphics.defaultFont(), x() + width() / 2, y() + height() / 2 - graphics.defaultFont().lineHeight / 2, TextUtils.text("Custom..."), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.CENTER, true);
        }
    }

    private static class ColorButton extends DLButton {

        private final DLColor color;

        public ColorButton(int pX, int pY, DLColor color, Consumer<ColorButton> pOnPress) {
            super(pX, pY, 12, 12);
            this.color = color;
            addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                pOnPress.accept(this);
                return false;
            });
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            GuiUtils.drawBox(graphics, 0, 0, width(), height(), color, isSelected() ? DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR : DragonLib.VANILLA_BUTTON_DISABLED_FONT_COLOR);
            GuiUtils.drawString(graphics, graphics.defaultFont(), x() + width() / 2, y() + height() / 2 - graphics.defaultFont().lineHeight / 2, TextUtils.empty(), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.CENTER, true);
        }
    }
    
    private static class NoColorButton extends DLButton {
        public NoColorButton(int pX, int pY, int pWidth, Consumer<NoColorButton> pOnPress) {
            super(pX, pY, pWidth, 16);
            addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                pOnPress.accept(this);
                return false;
            });
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            GuiUtils.drawBox(graphics, 0, 0, width(), height(), DLColor.TRANSPARENT, isSelected() ? DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR : DragonLib.VANILLA_BUTTON_DISABLED_FONT_COLOR);
            GuiUtils.drawString(graphics, graphics.defaultFont(), x() + width() / 2, y() + height() / 2 - graphics.defaultFont().lineHeight / 2, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".color_picker.no_color"), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.CENTER, true);
        }
    }
}
