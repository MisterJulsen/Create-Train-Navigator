package de.mrjulsen.crn.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public class DynamicWidgets {
    private static final int BORDER_WEIGHT = 2;
    private static final int TEXTBOX_HEIGHT = 18;
    private static final int COLOR_BORDER = 0xFF393939;
    private static final int COLOR_3D_SHADOW = 0xFF373737;
    private static final int COLOR_3D_HIGHLIGHT = 0xFF8b8b8b;
    private static final int COLOR_3D_NEUTRAL = 0xFF747474;
    private static final int COLOR_TEXTBOX_3D_SHADOW = 0xFF373737;
    private static final int COLOR_TEXTBOX_3D_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_TEXTBOX_3D_NEUTRAL = 0xFF747474;
    private static final int COLOR_TEXTBOX_COLOR_BG = 0xFFa3a3a3;
    private static final int COLOR_TEXTBOX_COLOR_ACCENT = 0xFF8b8b8b;

    public static void renderWidgetInner(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, color); // plane
        
        graphics.fill(x, y, x + 1, y + h, ColorShade.LIGHT.getColor()); // left border
        graphics.fill(x + 1, y, x + 2, y + h, COLOR_BORDER); // left border
        graphics.fill(x + w - 1, y, x + w, y + h, ColorShade.LIGHT.getColor()); // right border
        graphics.fill(x + w - 2, y, x + w - 1, y + h, COLOR_BORDER); // right border
    }

    public static void renderWidgetInner(GuiGraphics graphics, int x, int y, int w, int h, ColorShade color) {
        renderWidgetInner(graphics, x, y, w, h, color.getColor());
    }

    public static void renderWidgetTopBorder(GuiGraphics graphics, int x, int y, int w) {
        graphics.fill(x, y, x + w, y + BORDER_WEIGHT, ColorShade.LIGHT.getColor()); // left border
        graphics.fill(x + 1, y + 1, x + w - 1, y + BORDER_WEIGHT, COLOR_BORDER); // left border
    }

    public static void renderWidgetBottomBorder(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, int w) {
        graphics.fill(x, y, x + w, y + BORDER_WEIGHT, ColorShade.LIGHT.getColor()); // left border
        graphics.fill(x + 1, y, x + w - 1, y + 1, COLOR_BORDER); // left border
    }

    public static void renderSingleShadeWidget(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        renderWidgetInner(graphics, x, y, w, h, color);
        renderWidgetTopBorder(graphics, x, y, w);
        renderWidgetBottomBorder(graphics, x, y + h - BORDER_WEIGHT, w);
    }

    public static void renderSingleShadeWidget(GuiGraphics graphics, int x, int y, int w, int h, ColorShade color) {
        renderSingleShadeWidget(graphics, x, y, w, h, color.getColor());
    }

    public static void renderDuoShadeWidget(GuiGraphics graphics, int x, int y, int w, int h1, int color1, int h2, int color2) {
        renderWidgetInner(graphics, x, y, w, h1, color1);
        renderWidgetInner(graphics, x, y + h1, w, h2, color2);        
        renderWidgetTopBorder(graphics, x, y, w);
        renderWidgetBottomBorder(graphics, x, y + h1 + h2 - BORDER_WEIGHT, w);
    }

    public static void renderDuoShadeWidget(GuiGraphics graphics, int x, int y, int w, int h1, ColorShade color1, int h2, ColorShade color2) {
        renderDuoShadeWidget(graphics, x, y, w, h1, color1.getColor(), h2, color2.getColor());
    }

    public static void renderTextSlotOverlay(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + 1, COLOR_3D_SHADOW); // top line
        graphics.fill(x, y + h - 1, x + w, y + h, COLOR_3D_HIGHLIGHT); // bottom line        
        graphics.fill(x, y + h - 1, x + 1, y + h, COLOR_3D_NEUTRAL); // bottom left corner
        graphics.fill(x + w - 1, y + h - 1, x + w, y + h, COLOR_3D_NEUTRAL); // bottom right corner
        
        graphics.fill(x + 1, y + 2, x + 2, y + 3, COLOR_3D_SHADOW); // top left dot
        graphics.fill(x + w - 2, y + 2, x + w - 1, y + 3, COLOR_3D_NEUTRAL); // top right dot
        graphics.fill(x + 1, y + h - 3, x + 2, y + h - 2, COLOR_3D_NEUTRAL); // bottom left dot
        graphics.fill(x + w - 2, y + h - 3, x + w - 1, y + h - 2, COLOR_3D_HIGHLIGHT); // bottom right dot
    }
    
    public static void renderTextBox(GuiGraphics graphics, int x, int y, int w) {
        int h = TEXTBOX_HEIGHT;
        graphics.fill(x, y, x + w, y + h, COLOR_TEXTBOX_COLOR_ACCENT); // bg
        graphics.fill(x, y + 2, x + w, y + 4, COLOR_TEXTBOX_COLOR_BG);
        graphics.fill(x, y + h - 4, x + w, y + h - 2, COLOR_TEXTBOX_COLOR_BG);
        graphics.fill(x, y, x + w, y + 1, COLOR_TEXTBOX_3D_SHADOW); // top
        graphics.fill(x, y + h - 1, x + w, y + h, COLOR_TEXTBOX_3D_LIGHT); // bottom
        graphics.fill(x, y, x + 1, y + h, COLOR_TEXTBOX_3D_SHADOW); // left
        graphics.fill(x + w - 1, y, x + w, y + h, COLOR_TEXTBOX_3D_LIGHT); // right
        graphics.fill(x + w - 1, y, x + w, y + 1, COLOR_TEXTBOX_3D_NEUTRAL); // top right
        graphics.fill(x, y + h - 1, x + 1, y + h, COLOR_TEXTBOX_3D_NEUTRAL); // bottom left
    }

    public static void renderHorizontalSeparator(GuiGraphics graphics, int x, int y, int w) {
        graphics.fill(x + 1, y, x + w, y + 1, ColorShade.LIGHT.getColor());
        graphics.fill(x, y + 1, x + w - 1, y + 2, COLOR_BORDER);
    }

    public static enum ColorShade {
        LIGHT(0xFF6f6f6f),
        DARK(0xFF575757);

        private int color;

        ColorShade(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

}
