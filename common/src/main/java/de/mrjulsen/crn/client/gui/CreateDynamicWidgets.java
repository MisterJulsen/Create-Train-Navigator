package de.mrjulsen.crn.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.mcdragonlib.client.render.DefaultGuiTextures;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils.TextureFillMode;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CreateDynamicWidgets {
    

    private static final int BORDER_HEIGHT = 2;
    private static final int TEXTBOX_HEIGHT = 18;
    private static final DLColor COLOR_BORDER = DLColor.fromInt(0xFF393939);
    private static final DLColor COLOR_3D_SHADOW = DLColor.fromInt(0xFF373737);
    private static final DLColor COLOR_3D_HIGHLIGHT = DLColor.fromInt(0xFF8b8b8b);
    private static final DLColor COLOR_3D_NEUTRAL = DLColor.fromInt(0xFF747474);
    private static final DLColor COLOR_TEXTBOX_3D_SHADOW = DLColor.fromInt(0xFF373737);
    private static final DLColor COLOR_TEXTBOX_3D_LIGHT = DLColor.fromInt(0xFFFFFFFF);
    private static final DLColor COLOR_TEXTBOX_3D_NEUTRAL = DLColor.fromInt(0xFF747474);
    private static final DLColor COLOR_TEXTBOX_COLOR_BG = DLColor.fromInt(0xFFa3a3a3);
    private static final DLColor COLOR_TEXTBOX_COLOR_ACCENT = DLColor.fromInt(0xFF8b8b8b);

    public static void renderWidgetInner(DLGuiGraphics graphics, int x, int y, int w, int h, DLColor color) {
        GuiUtils.fill(graphics, x, y, w, h, color); // plane
        
        GuiUtils.fill(graphics, x, y, 1, h, ColorShade.LIGHT.getColor()); // left border
        GuiUtils.fill(graphics, x + 1, y, 1, h, COLOR_BORDER); // left border
        GuiUtils.fill(graphics, x + w - 1, y, 1, h, ColorShade.LIGHT.getColor()); // right border
        GuiUtils.fill(graphics, x + w - 2, y, 1, h, COLOR_BORDER); // right border
    }

    public static void renderWidgetInner(DLGuiGraphics graphics, int x, int y, int w, int h, ColorShade color) {
        renderWidgetInner(graphics, x, y, w, h, color.getColor());
    }

    public static void renderWidgetTopBorder(DLGuiGraphics graphics, int x, int y, int w) {
        GuiUtils.fill(graphics, x + 1, y, w - 2, 1, ColorShade.LIGHT.getColor()); // left border
        GuiUtils.fill(graphics, x, y + 1, w, 1, ColorShade.LIGHT.getColor()); // left border
        GuiUtils.fill(graphics, x + 1, y + 1, w - 2, BORDER_HEIGHT - 1, COLOR_BORDER); // left border
    }

    public static void renderWidgetBottomBorder(DLGuiGraphics graphics, int x, int y, int w) {
        GuiUtils.fill(graphics, x, y, w, 1, ColorShade.LIGHT.getColor()); // left border
        GuiUtils.fill(graphics, x + 1, y + 1, w - 2, 1, ColorShade.LIGHT.getColor()); // left border
        GuiUtils.fill(graphics, x + 1, y, w - 2, 1, COLOR_BORDER); // left border
    }

    public static void renderSingleShadeWidget(DLGuiGraphics DLGuiGraphics, int x, int y, int w, int h, DLColor color) {
        renderWidgetInner(DLGuiGraphics, x, y + 2, w, h - 4, color);
        renderWidgetTopBorder(DLGuiGraphics, x, y, w);
        renderWidgetBottomBorder(DLGuiGraphics, x, y + h - BORDER_HEIGHT, w);
    }

    public static void renderSingleShadeWidget(DLGuiGraphics graphics, int x, int y, int w, int h, ColorShade color) {
        renderSingleShadeWidget(graphics, x, y, w, h, color.getColor());
    }

    public static void renderDuoShadeWidget(DLGuiGraphics graphics, int x, int y, int w, int h1, DLColor color1, int h2, DLColor color2) {
        renderWidgetInner(graphics, x, y + 2, w, h1 - 2, color1);
        renderWidgetInner(graphics, x, y + h1, w, h2 - 2, color2);        
        renderWidgetTopBorder(graphics, x, y, w);
        renderWidgetBottomBorder(graphics, x, y + h1 + h2 - BORDER_HEIGHT, w);
    }

    public static void renderDuoShadeWidget(DLGuiGraphics graphics, int x, int y, int w, int h1, ColorShade color1, int h2, ColorShade color2) {
        renderDuoShadeWidget(graphics, x, y, w, h1, color1.getColor(), h2, color2.getColor());
    }

    public static void renderGrabber(DLGuiGraphics graphics, int x, int y) {
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 4; k++) {
                GuiUtils.fill(graphics, x + 1 + i * 4, y + 1 + k * 4, 2, 2, COLOR_3D_SHADOW);
                GuiUtils.fill(graphics, x + 1 + i * 4, y + 1 + k * 4, 1, 1, COLOR_3D_HIGHLIGHT);
            }
        }
    }

    public static void renderTextSlotOverlay(DLGuiGraphics graphics, int x, int y, int w, int h) {
        GuiUtils.fill(graphics, x, y, w, 1, COLOR_3D_SHADOW); // top line
        GuiUtils.fill(graphics, x, y + h - 1, w, 1, COLOR_3D_HIGHLIGHT); // bottom line        
        GuiUtils.fill(graphics, x, y + h - 1, 1, 1, COLOR_3D_NEUTRAL); // bottom left corner
        GuiUtils.fill(graphics, x + w - 1, y + h - 1, 1, 1, COLOR_3D_NEUTRAL); // bottom right corner
        
        GuiUtils.fill(graphics, x + 1, y + 2, 1, 1, COLOR_3D_SHADOW); // top left dot
        GuiUtils.fill(graphics, x + w - 2, y + 2, 1, 1, COLOR_3D_NEUTRAL); // top right dot
        GuiUtils.fill(graphics, x + 1, y + h - 3, 1, 1, COLOR_3D_NEUTRAL); // bottom left dot
        GuiUtils.fill(graphics, x + w - 2, y + h - 3, 1, 1, COLOR_3D_HIGHLIGHT); // bottom right dot
    }
    
    public static void renderTextBox(DLGuiGraphics graphics, int x, int y, int w) {
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

    public static void renderHorizontalSeparator(DLGuiGraphics graphics, int x, int y, int w) {
        GuiUtils.fill(graphics, x + 1, y, w - 1, 1, ColorShade.LIGHT.getColor());
        GuiUtils.fill(graphics, x, y + 1, w - 1, 1, COLOR_BORDER);
    }

    public static void renderContainerBackground(DLGuiGraphics graphics, int x, int y, int w, int h, ContainerColor color) {        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, color.res);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        float f = 2f;
        bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.vertex(x, y + (double)h, 0.0).uv(0.0F, (float)h / f).color(1f, 1f, 1f, 1f).endVertex();
        bufferbuilder.vertex(x + (double)w, y + (double)h, 0.0).uv((float)w / f, (float)h / f).color(1f, 1f, 1f, 1f).endVertex();
        bufferbuilder.vertex(x + (double)w, y, 0.0).uv((float)w / f, 0).color(1f, 1f, 1f, 1f).endVertex();
        bufferbuilder.vertex(x, y, 0.0).uv(0.0F, 0).color(1f, 1f, 1f, 1f).endVertex();
        tesselator.end();
    }

    protected static void renderNineSliced(DLGuiGraphics graphics, int x, int y, int w, int h, int u, int v, int textureWidth, int textureHeight, int cornerSliceSize, ResourceLocation location, boolean renderCenter) {
        GuiUtils.drawTexture(location, graphics, x, y, cornerSliceSize, cornerSliceSize, u, v, cornerSliceSize, cornerSliceSize, TextureFillMode.STRETCH, textureWidth, textureHeight); // Top left
        GuiUtils.drawTexture(location, graphics, x + w - cornerSliceSize, y, cornerSliceSize, cornerSliceSize, u + 1 + cornerSliceSize, v, cornerSliceSize, cornerSliceSize, TextureFillMode.STRETCH, textureWidth, textureHeight); // Top right
        GuiUtils.drawTexture(location, graphics, x, y + h - cornerSliceSize, cornerSliceSize, cornerSliceSize, u, v + 1 + cornerSliceSize, cornerSliceSize, cornerSliceSize, TextureFillMode.STRETCH, textureWidth, textureHeight); // bottom left
        GuiUtils.drawTexture(location, graphics, x + w - cornerSliceSize, y + h - cornerSliceSize, cornerSliceSize, cornerSliceSize, u + 1 + cornerSliceSize, v + 1 + cornerSliceSize, cornerSliceSize, cornerSliceSize, TextureFillMode.STRETCH, textureWidth, textureHeight); // bottom right

        GuiUtils.drawTexture(location, graphics, x + cornerSliceSize, y, w - cornerSliceSize * 2, cornerSliceSize, u + cornerSliceSize, v, 1, cornerSliceSize, TextureFillMode.STRETCH, textureWidth, textureHeight); // top
        GuiUtils.drawTexture(location, graphics, x + cornerSliceSize, y + h - cornerSliceSize, w - cornerSliceSize * 2, cornerSliceSize, u + cornerSliceSize, v + 1 + cornerSliceSize, 1, cornerSliceSize, TextureFillMode.STRETCH, textureWidth, textureHeight); // bottom
        GuiUtils.drawTexture(location, graphics, x, y + cornerSliceSize, cornerSliceSize, h - cornerSliceSize * 2, u, v + cornerSliceSize, cornerSliceSize, 1, TextureFillMode.STRETCH, textureWidth, textureHeight); // left
        GuiUtils.drawTexture(location, graphics, x + w - cornerSliceSize, y + cornerSliceSize, cornerSliceSize, h - cornerSliceSize * 2, u + 1 + cornerSliceSize, v + cornerSliceSize, cornerSliceSize, 1, TextureFillMode.STRETCH, textureWidth, textureHeight); // right

        if (renderCenter) {
            GuiUtils.drawTexture(location, graphics, x + cornerSliceSize, y + cornerSliceSize, w - cornerSliceSize * 2, h - cornerSliceSize * 2, u + cornerSliceSize, v + cornerSliceSize, 1, 1, TextureFillMode.STRETCH, textureWidth, textureHeight);
        }
    }

    public static void renderContainer(DLGuiGraphics graphics, int x, int y, int w, int h, ContainerColor color) {
        switch (color) {
            case GOLD -> CRNGui.GUI_SPRITES.getSprite("content_gold").render(graphics, x, y, w, h);
            case PURPLE -> CRNGui.GUI_SPRITES.getSprite("content_purple").render(graphics, x, y, w, h);
            case BLUE -> CRNGui.GUI_SPRITES.getSprite("content_blue").render(graphics, x, y, w, h);
            default -> CRNGui.GUI_SPRITES.getSprite("content_gray").render(graphics, x, y, w, h);
        }
        //renderContainerBackground(graphics, x + 2, y + 2, w - 4, h - 4, color);
        //renderNineSliced(graphics, x, y, w, h, 0, 7, CRNGui.GUI.width(), CRNGui.GUI.height(), 2, CRNGui.GUI.getTexture().get(), false);
    }

    public static void renderTitleBar(DLGuiGraphics graphics, int x, int y, int w, int h, BarColor color) {
        switch (color) {
            case GOLD -> CRNGui.GUI_SPRITES.getSprite("header_gold").render(graphics, x, y, w, h);
            case PURPLE -> CRNGui.GUI_SPRITES.getSprite("header_purple").render(graphics, x, y, w, h);
            default -> CRNGui.GUI_SPRITES.getSprite("header_gray").render(graphics, x, y, w, h);
        }
        //renderNineSliced(graphics, x, y, w, h, color.u, color.v, CRNGui.GUI.width(), CRNGui.GUI.height(), 3, CRNGui.GUI.getTexture().get(), true);
    }

    
    public static void renderWindow(DLGuiGraphics graphics, int x, int y, int w, int h, ContainerColor color, BarColor bar, int headerSize, int footerSize, boolean renderContent) {
        renderWindow(graphics, x, y, w, h, color, bar, bar, headerSize, footerSize, renderContent);
    }
    
    public static void renderWindow(DLGuiGraphics graphics, int x, int y, int w, int h, ContainerColor color, BarColor header, BarColor footer, int headerSize, int footerSize, boolean renderContent) {
        renderTitleBar(graphics, x, y, w, headerSize, header);
        renderTitleBar(graphics, x, y + h - footerSize, w, footerSize, footer);
        
        if (renderContent) {
            renderContainer(graphics, x + 1, y + headerSize - 1, w - 2, h - headerSize - footerSize + 2, color);
        }
    }

    public static void renderShadow(DLGuiGraphics graphics, int x, int y, int w, int h) {
        renderNineSliced(graphics, x - 5, y - 5, w + 10, h + 10, 0, 0, 11, 11, 5, new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/shadow.png"), true);
    }

    public static void renderTextHighlighted(DLGuiGraphics graphics, int x, int y, Font font, Component text, DLColor color) {
        int width = font.width(text) + 6;
        int height = font.lineHeight + 6;
        GuiUtils.fill(graphics, x, y + 1, width, height - 3, color);
        GuiUtils.fill(graphics, x + 1, y, width - 2, 1, color);
        GuiUtils.fill(graphics, x + 1, y + height - 2, width - 2, 1, color);
        GuiUtils.drawString(graphics, font, x + 3, y + 3, text, DLColor.pickBasedOnBrightness(color, DLColor.WHITE, DLColor.BLACK, 0.5f), ETextAlignment.LEFT, false);
    }

    public static void renderIconSlot(DLGuiGraphics graphics, int x, int y, int w, int h) {
        renderNineSliced(graphics, x, y, w, h, 0, 7, CRNGui.GUI.width(), CRNGui.GUI.height(), 1, CRNGui.GUI.getTexture().get(), true);
    }

    public static void renderTextBoxArrow(DLGuiGraphics graphics, int x, int y) {
        GuiUtils.drawTexture(CRNGui.GUI, graphics, x - 3, y + 5, 4, 8, 60, 54, 4, 8, TextureFillMode.STRETCH);
    }

    public static void renderVerticalSeparator(DLGuiGraphics graphics, int x, int y, int h, BarColor color) {
        GuiUtils.drawTexture(CRNGui.GUI, graphics, x, y, 1, h, color.u + 5, color.v + 3, 1, 1, TextureFillMode.STRETCH);
        GuiUtils.drawTexture(CRNGui.GUI, graphics, x + 1, y, 1, h, color.u + 1, color.v + 3, 1, 1, TextureFillMode.STRETCH);
    }


    public static enum ColorShade {
        LIGHT(DLColor.fromInt(0xFF6f6f6f)),
        DARK(DLColor.fromInt(0xFF575757));

        private final DLColor color;

        ColorShade(DLColor color) {
            this.color = color;
        }

        public DLColor getColor() {
            return color;
        }
    }

    
    public static enum ContainerColor {
        GRAY(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/container_gray.png")),
        PURPLE(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/container_purple.png")),
        BLUE(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/container_blue.png")),
        GOLD(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/container_gold.png"));

        private final ResourceLocation res;

        ContainerColor(ResourceLocation res) {
            this.res = res;
        }
    }

    public static enum BarColor {
        GRAY(7, 0),
        PURPLE(14, 0),
        GOLD(0, 0);

        private final int u;
        private final int v;

        BarColor(int u, int v) {
            this.u = u;
            this.v = v;
        }
    }
    

    
    public static enum FooterSize {
        DEFAULT(15),
        SMALL(30),
        EXTENDED(36);

        private final int size;

        FooterSize(int size) {
            this.size = size;
        }

        public int size() {
            return size;
        }
    }
}
