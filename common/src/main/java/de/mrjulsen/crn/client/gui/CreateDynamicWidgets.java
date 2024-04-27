package de.mrjulsen.crn.client.gui;

import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;

public class CreateDynamicWidgets {
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

    public static void renderWidgetInner(Graphics graphics, int x, int y, int w, int h, int color) {
        GuiUtils.fill(graphics, x, y, w, h, color); // plane
        
        GuiUtils.fill(graphics, x, y, 1, h, ColorShade.LIGHT.getColor()); // left border
        GuiUtils.fill(graphics, x + 1, y, 1, h, COLOR_BORDER); // left border
        GuiUtils.fill(graphics, x + w - 1, y, 1, h, ColorShade.LIGHT.getColor()); // right border
        GuiUtils.fill(graphics, x + w - 2, y, 1, h, COLOR_BORDER); // right border
    }

    public static void renderWidgetInner(Graphics graphics, int x, int y, int w, int h, ColorShade color) {
        renderWidgetInner(graphics, x, y, w, h, color.getColor());
    }

    public static void renderWidgetTopBorder(Graphics graphics, int x, int y, int w) {
        GuiUtils.fill(graphics, x, y, w, BORDER_WEIGHT, ColorShade.LIGHT.getColor()); // left border
        GuiUtils.fill(graphics, x + 1, y + 1, w - 2, BORDER_WEIGHT - 1, COLOR_BORDER); // left border
    }

    public static void renderWidgetBottomBorder(Graphics graphics, int x, int y, int w) {
        GuiUtils.fill(graphics, x, y, w, BORDER_WEIGHT, ColorShade.LIGHT.getColor()); // left border
        GuiUtils.fill(graphics, x + 1, y, w - 2, 1, COLOR_BORDER); // left border
    }

    public static void renderSingleShadeWidget(Graphics graphics, int x, int y, int w, int h, int color) {
        renderWidgetInner(graphics, x, y, w, h, color);
        renderWidgetTopBorder(graphics, x, y, w);
        renderWidgetBottomBorder(graphics, x, y + h - BORDER_WEIGHT, w);
    }

    public static void renderSingleShadeWidget(Graphics graphics, int x, int y, int w, int h, ColorShade color) {
        renderSingleShadeWidget(graphics, x, y, w, h, color.getColor());
    }

    public static void renderDuoShadeWidget(Graphics graphics, int x, int y, int w, int h1, int color1, int h2, int color2) {
        renderWidgetInner(graphics, x, y, w, h1, color1);
        renderWidgetInner(graphics, x, y + h1, w, h2, color2);        
        renderWidgetTopBorder(graphics, x, y, w);
        renderWidgetBottomBorder(graphics, x, y + h1 + h2 - BORDER_WEIGHT, w);
    }

    public static void renderDuoShadeWidget(Graphics graphics, int x, int y, int w, int h1, ColorShade color1, int h2, ColorShade color2) {
        renderDuoShadeWidget(graphics, x, y, w, h1, color1.getColor(), h2, color2.getColor());
    }

    public static void renderTextSlotOverlay(Graphics graphics, int x, int y, int w, int h) {
        GuiUtils.fill(graphics, x, y, w, 1, COLOR_3D_SHADOW); // top line
        GuiUtils.fill(graphics, x, y + h - 1, w, 1, COLOR_3D_HIGHLIGHT); // bottom line        
        GuiUtils.fill(graphics, x, y + h - 1, 1, 1, COLOR_3D_NEUTRAL); // bottom left corner
        GuiUtils.fill(graphics, x + w - 1, y + h - 1, 1, 1, COLOR_3D_NEUTRAL); // bottom right corner
        
        GuiUtils.fill(graphics, x + 1, y + 2, 1, 1, COLOR_3D_SHADOW); // top left dot
        GuiUtils.fill(graphics, x + w - 2, y + 2, 1, 1, COLOR_3D_NEUTRAL); // top right dot
        GuiUtils.fill(graphics, x + 1, y + h - 3, 1, 1, COLOR_3D_NEUTRAL); // bottom left dot
        GuiUtils.fill(graphics, x + w - 2, y + h - 3, 1, 1, COLOR_3D_HIGHLIGHT); // bottom right dot
    }
    
    public static void renderTextBox(Graphics graphics, int x, int y, int w) {
        int h = TEXTBOX_HEIGHT;
        GuiUtils.fill(graphics, x, y, w, h, COLOR_TEXTBOX_COLOR_ACCENT); // bg
        GuiUtils.fill(graphics, x, y + 2, w, 2, COLOR_TEXTBOX_COLOR_BG);
        GuiUtils.fill(graphics, x, y + h - 4, w, 2, COLOR_TEXTBOX_COLOR_BG);
        GuiUtils.fill(graphics, x, y, w, 1, COLOR_TEXTBOX_3D_SHADOW); // top
        GuiUtils.fill(graphics, x, y + h - 1, w, 1, COLOR_TEXTBOX_3D_LIGHT); // bottom
        GuiUtils.fill(graphics, x, y, 1, h, COLOR_TEXTBOX_3D_SHADOW); // left
        GuiUtils.fill(graphics, x + w - 1, y, 1, h, COLOR_TEXTBOX_3D_LIGHT); // right
        GuiUtils.fill(graphics, x + w - 1, y, 1, 1, COLOR_TEXTBOX_3D_NEUTRAL); // top right
        GuiUtils.fill(graphics, x, y + h - 1, 1, 1, COLOR_TEXTBOX_3D_NEUTRAL); // bottom left
    }

    public static void renderHorizontalSeparator(Graphics graphics, int x, int y, int w) {
        GuiUtils.fill(graphics, x + 1, y, w - 1, 1, ColorShade.LIGHT.getColor());
        GuiUtils.fill(graphics, x, y + 1, w - 1, 1, COLOR_BORDER);
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
