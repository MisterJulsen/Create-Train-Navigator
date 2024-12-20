package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;
import java.util.function.Supplier;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLColorPickerScreen;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLWidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class ColorPickerWidget extends DLWidgetContainer {

    private int selectedColor = 0;

    public ColorPickerWidget(Screen parent, int px, int py, int[] sampleColors, int maxColorsPerLine, int preselectedColor, boolean allowCustom, boolean allowNone, Consumer<ColorPickerWidget> onPick) {
        super(px, py, 1, 1);
        this.selectedColor = preselectedColor;

        int lines = (int)Math.ceil((double)sampleColors.length / (double)maxColorsPerLine);
        set_height(18 * ((allowCustom ? 1 : 0) + (allowNone ? 1 : 0)) + lines * 13 - 1);
        set_width(maxColorsPerLine * 13 - 1);
        int currentY = 0;
        if (allowCustom) {            
            addRenderableWidget(new ColorBrowserButton(x(), y() + currentY, width(), () -> selectedColor, (btn) -> {
                DLScreen.setScreen(new DLColorPickerScreen(parent, selectedColor, c -> {
                    selectedColor = c.toInt();
                    onPick.accept(this);
                }, true));
            }));
            currentY += 18;
        }
        for (int i = 0, y = 0; y < lines && i < sampleColors.length; y++) {
            for (int x = 0; x < maxColorsPerLine && i < sampleColors.length; x++, i++) {
                final int j = i;
                addRenderableWidget(new ColorButton(x() + x * 13, y() + currentY + y * 13, sampleColors[j], btn -> {
                    selectedColor = sampleColors[j];                    
                    onPick.accept(this);
                }));
            }
        }
        if (allowNone) {                
            addRenderableWidget(new NoColorButton(x(), y() + height() - 16, width(), (btn) -> {
                selectedColor = 0;
                onPick.accept(this);
            }));
        }
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }
    

    private static class ColorBrowserButton extends DLButton {
        private final Supplier<Integer> color;
        public ColorBrowserButton(int pX, int pY, int pWidth, Supplier<Integer> color, Consumer<ColorBrowserButton> pOnPress) {
            super(pX, pY, pWidth, 16, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".color_picker.custom"), pOnPress);
            this.color = color;
        }

        @Override
        public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
            GuiUtils.drawBox(graphics, GuiAreaDefinition.of(this), color.get(), isMouseSelected() ? DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED);
            GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 - font.lineHeight / 2, getMessage(), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.CENTER, true);
        }
    }

    private static class ColorButton extends DLButton {
        private final int color;
        public ColorButton(int pX, int pY, int color, Consumer<ColorButton> pOnPress) {
            super(pX, pY, 12, 12, TextUtils.empty(), pOnPress);
            this.color = color;
        }

        @Override
        public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
            GuiUtils.drawBox(graphics, GuiAreaDefinition.of(this), color, isMouseSelected() ? DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED);
            GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 - font.lineHeight / 2, getMessage(), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.CENTER, true);
        }
    }
    
    private static class NoColorButton extends DLButton {
        public NoColorButton(int pX, int pY, int pWidth, Consumer<NoColorButton> pOnPress) {
            super(pX, pY, pWidth, 16, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".color_picker.no_color"), pOnPress);
        }

        @Override
        public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
            GuiUtils.drawBox(graphics, GuiAreaDefinition.of(this), 0, isMouseSelected() ? DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED);
            GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 - font.lineHeight / 2, getMessage(), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.CENTER, true);
        }
    }
}
