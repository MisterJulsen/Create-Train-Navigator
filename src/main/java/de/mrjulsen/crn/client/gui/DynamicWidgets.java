package de.mrjulsen.crn.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiComponent;

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

    public static void renderWidgetInner(PoseStack poseStack, int x, int y, int w, int h, int color) {
        GuiComponent.fill(poseStack, x, y, x + w, y + h, color); // plane
        
        GuiComponent.fill(poseStack, x, y, x + 1, y + h, ColorShade.LIGHT.getColor()); // left border
        GuiComponent.fill(poseStack, x + 1, y, x + 2, y + h, COLOR_BORDER); // left border
        GuiComponent.fill(poseStack, x + w - 1, y, x + w, y + h, ColorShade.LIGHT.getColor()); // right border
        GuiComponent.fill(poseStack, x + w - 2, y, x + w - 1, y + h, COLOR_BORDER); // right border
    }

    public static void renderWidgetInner(PoseStack poseStack, int x, int y, int w, int h, ColorShade color) {
        renderWidgetInner(poseStack, x, y, w, h, color.getColor());
    }

    public static void renderWidgetTopBorder(PoseStack poseStack, int x, int y, int w) {
        GuiComponent.fill(poseStack, x, y, x + w, y + BORDER_WEIGHT, ColorShade.LIGHT.getColor()); // left border
        GuiComponent.fill(poseStack, x + 1, y + 1, x + w - 1, y + BORDER_WEIGHT, COLOR_BORDER); // left border
    }

    public static void renderWidgetBottomBorder(PoseStack poseStack, int x, int y, int w) {
        GuiComponent.fill(poseStack, x, y, x + w, y + BORDER_WEIGHT, ColorShade.LIGHT.getColor()); // left border
        GuiComponent.fill(poseStack, x + 1, y, x + w - 1, y + 1, COLOR_BORDER); // left border
    }

    public static void renderSingleShadeWidget(PoseStack poseStack, int x, int y, int w, int h, int color) {
        renderWidgetInner(poseStack, x, y, w, h, color);
        renderWidgetTopBorder(poseStack, x, y, w);
        renderWidgetBottomBorder(poseStack, x, y + h - BORDER_WEIGHT, w);
    }

    public static void renderSingleShadeWidget(PoseStack poseStack, int x, int y, int w, int h, ColorShade color) {
        renderSingleShadeWidget(poseStack, x, y, w, h, color.getColor());
    }

    public static void renderDuoShadeWidget(PoseStack poseStack, int x, int y, int w, int h1, int color1, int h2, int color2) {
        renderWidgetInner(poseStack, x, y, w, h1, color1);
        renderWidgetInner(poseStack, x, y + h1, w, h2, color2);        
        renderWidgetTopBorder(poseStack, x, y, w);
        renderWidgetBottomBorder(poseStack, x, y + h1 + h2 - BORDER_WEIGHT, w);
    }

    public static void renderDuoShadeWidget(PoseStack poseStack, int x, int y, int w, int h1, ColorShade color1, int h2, ColorShade color2) {
        renderDuoShadeWidget(poseStack, x, y, w, h1, color1.getColor(), h2, color2.getColor());
    }

    public static void renderTextSlotOverlay(PoseStack poseStack, int x, int y, int w, int h) {
        GuiComponent.fill(poseStack, x, y, x + w, y + 1, COLOR_3D_SHADOW); // top line
        GuiComponent.fill(poseStack, x, y + h - 1, x + w, y + h, COLOR_3D_HIGHLIGHT); // bottom line        
        GuiComponent.fill(poseStack, x, y + h - 1, x + 1, y + h, COLOR_3D_NEUTRAL); // bottom left corner
        GuiComponent.fill(poseStack, x + w - 1, y + h - 1, x + w, y + h, COLOR_3D_NEUTRAL); // bottom right corner
        
        GuiComponent.fill(poseStack, x + 1, y + 2, x + 2, y + 3, COLOR_3D_SHADOW); // top left dot
        GuiComponent.fill(poseStack, x + w - 2, y + 2, x + w - 1, y + 3, COLOR_3D_NEUTRAL); // top right dot
        GuiComponent.fill(poseStack, x + 1, y + h - 3, x + 2, y + h - 2, COLOR_3D_NEUTRAL); // bottom left dot
        GuiComponent.fill(poseStack, x + w - 2, y + h - 3, x + w - 1, y + h - 2, COLOR_3D_HIGHLIGHT); // bottom right dot
    }
    
    public static void renderTextBox(PoseStack poseStack, int x, int y, int w) {
        int h = TEXTBOX_HEIGHT;
        GuiComponent.fill(poseStack, x, y, x + w, y + h, COLOR_TEXTBOX_COLOR_ACCENT); // bg
        GuiComponent.fill(poseStack, x, y + 2, x + w, y + 4, COLOR_TEXTBOX_COLOR_BG);
        GuiComponent.fill(poseStack, x, y + h - 4, x + w, y + h - 2, COLOR_TEXTBOX_COLOR_BG);
        GuiComponent.fill(poseStack, x, y, x + w, y + 1, COLOR_TEXTBOX_3D_SHADOW); // top
        GuiComponent.fill(poseStack, x, y + h - 1, x + w, y + h, COLOR_TEXTBOX_3D_LIGHT); // bottom
        GuiComponent.fill(poseStack, x, y, x + 1, y + h, COLOR_TEXTBOX_3D_SHADOW); // left
        GuiComponent.fill(poseStack, x + w - 1, y, x + w, y + h, COLOR_TEXTBOX_3D_LIGHT); // right
        GuiComponent.fill(poseStack, x + w - 1, y, x + w, y + 1, COLOR_TEXTBOX_3D_NEUTRAL); // top right
        GuiComponent.fill(poseStack, x, y + h - 1, x + 1, y + h, COLOR_TEXTBOX_3D_NEUTRAL); // bottom left
    }

    public static void renderHorizontalSeparator(PoseStack poseStack, int x, int y, int w) {
        GuiComponent.fill(poseStack, x + 1, y, x + w, y + 1, ColorShade.LIGHT.getColor());
        GuiComponent.fill(poseStack, x, y + 1, x + w - 1, y + 2, COLOR_BORDER);
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
