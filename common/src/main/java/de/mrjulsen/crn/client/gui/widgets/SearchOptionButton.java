package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;
import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.ButtonState;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class SearchOptionButton extends DLButton {

    private final Supplier<String> value;

    public SearchOptionButton(int pX, int pY, int pWidth, int pHeight, Component text, Supplier<String> value, Consumer<SearchOptionButton> clickAction) {
        super(pX, pY, pWidth, pHeight, text, clickAction);
        this.value = value;
        setRenderStyle(AreaStyle.FLAT);
        setBackColor(0x00000000);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        DynamicGuiRenderer.renderArea(graphics, x(), y(), width, height, getBackColor(), style, isActive() ? (isFocused() || isMouseSelected() ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
        
        int j = active ? getFontColor() : DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED;
        
        GuiUtils.fill(graphics, x() + width() - 1, y() + 2, 1, height() - 4, DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED);
        
        final float scale = 0.75f;
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, 1);
        graphics.poseStack().translate(x() / scale, y() / scale, 0);
        GuiUtils.drawString(graphics, font, 5, 3, TextUtils.empty().append(getMessage()).withStyle(ChatFormatting.BOLD), j, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, 5, 3 + font.lineHeight + 1, TextUtils.text(value.get()).withStyle(ChatFormatting.GRAY), j, EAlignment.LEFT, false);
        graphics.poseStack().popPose();
        //GuiIcons.ARROW_DOWN.render(graphics, x() + width() - DROP_DOWN_BUTTON_WIDTH + 3, y() + height() / 2 - GuiIcons.ICON_SIZE / 2 + 5);
    }
    
}
