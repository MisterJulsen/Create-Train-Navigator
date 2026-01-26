package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class SearchOptionButton extends DLButton {

    private final Supplier<String> value;

    public SearchOptionButton(int pX, int pY, int pWidth, int pHeight, Component text, Supplier<String> value, Consumer<SearchOptionButton> clickAction) {
        super(pX, pY, pWidth, pHeight);
        this.text.set(text);
        this.value = value;
        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            clickAction.accept(this);
            return false;
        });
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {          
        if (enabled.get() && isMouseDown() && (getWindowManager() != null ? getWindowManager().getMouseDownButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT : true)) {
            GuiUtils.fill(graphics, getRenderBounds(), DLColor.fromInt(0x80000000));
        } else if (enabled.get() && isSelected()) {
            GuiUtils.fill(graphics, getRenderBounds(), DLColor.fromInt(0x44FFFFFF));
        }

        DLColor j = enabled.get() ? textColor.get() : DragonLib.VANILLA_BUTTON_DISABLED_FONT_COLOR;
        
        GuiUtils.fill(graphics, width() - 1, 2, 1, height() - 4, DragonLib.VANILLA_BUTTON_DISABLED_FONT_COLOR);
        
        final float scale = 0.75f;
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, 1);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 5, 3, TextUtils.empty().append(text.get()).withStyle(ChatFormatting.BOLD), j, ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 5, 3 + graphics.defaultFont().lineHeight + 1, TextUtils.text(value.get()).withStyle(ChatFormatting.GRAY), j, ETextAlignment.LEFT, false);
        graphics.poseStack().popPose();
    }
    
}
