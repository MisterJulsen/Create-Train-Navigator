package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.function.Consumer;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.ButtonState;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class OptionEntryHeader extends DLButton {

public static final int DEFAULT_HEIGHT = 20;

    private final OptionEntry<?> parent;

    public OptionEntryHeader(OptionEntry<?> parent, int pX, int pY, int pWidth, Component pMessage, Consumer<OptionEntryHeader> pOnPress) {
        super(pX, pY, pWidth, DEFAULT_HEIGHT, pMessage, pOnPress);
        this.parent = parent;
        setRenderStyle(AreaStyle.FLAT);
        setBackColor(0x00000000);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, x(), y(), width(), height(), ColorShade.LIGHT);
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, x(), y(), width(), 20, ColorShade.DARK);
        DynamicGuiRenderer.renderArea(graphics, x(), y(), width, height, getBackColor(), style, isActive() ? (isFocused() || isMouseSelected() ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
        int j = active ? (isMouseSelected() ? DragonLib.NATIVE_BUTTON_FONT_COLOR_HIGHLIGHT : DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE) : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED;
        GuiUtils.drawString(graphics, font, x() + 5, this.y() + (20 - 8) / 2, this.getMessage(), j, EAlignment.LEFT, false);
        if (parent.isExpanded()) {
            GuiIcons.ARROW_UP.render(graphics, x() + width() - 2 - GuiIcons.ICON_SIZE, y() + 2);
        } else {
            if (parent.getContentContainer() == null) {
                GuiIcons.ARROW_RIGHT.render(graphics, x() + width() - 2 - GuiIcons.ICON_SIZE, y() + 2);
            } else {
                GuiIcons.ARROW_DOWN.render(graphics, x() + width() - 2 - GuiIcons.ICON_SIZE, y() + 2);
            }
        }
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);
        
        if (isMouseSelected()) {
            renderDescriptionTooltip(graphics, mouseX, mouseY, partialTicks);
        }
    }

    protected void renderDescriptionTooltip(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        DLUtils.doIfNotNull(parent.getTooltips(), t -> {            
            final float scale = 0.75f;
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(mouseX, mouseY, 0);
            graphics.poseStack().scale(scale, scale, 1);
            int maxLineWidth = t.stream().mapToInt(x -> font.width(x)).max().orElse(0);
            GuiUtils.fill(graphics, 8, 12, maxLineWidth + 6, (font.lineHeight + 2) * t.size() + 4, 0xAA000000);
            for (int i = 0; i < t.size(); i++) {
                FormattedText line = t.get(i);
                GuiUtils.drawString(graphics, font, 12, 16 + (font.lineHeight + 2) * i, line, DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
            }
            graphics.poseStack().popPose();
        });
    }
    
}
