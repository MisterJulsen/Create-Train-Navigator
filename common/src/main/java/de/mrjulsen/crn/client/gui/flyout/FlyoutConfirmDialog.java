package de.mrjulsen.crn.client.gui.flyout;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

public class FlyoutConfirmDialog extends AbstractFlyoutWidget {

    private final MutableComponent txtAreYouSure = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.are_you_sure").withStyle(ChatFormatting.BOLD);

    public FlyoutConfirmDialog(DLWindowManager manager, DLGuiComponent parentComponent, FlyoutPointer pointer, ColorShade pointerShade, Runnable confirm) {
        super(manager, parentComponent, 1, FlatIconButton.HEIGHT + FlyoutPointer.HEIGHT * 2, pointer, pointerShade);
        setWidth(FlatIconButton.WIDTH + 15 + Minecraft.getInstance().font.width(txtAreYouSure) + FlyoutPointer.HEIGHT * 2);
        FlatIconButton acceptBtn = addComponent(new FlatIconButton(width() - 2 - FlatIconButton.WIDTH - FlyoutPointer.HEIGHT, FlyoutPointer.HEIGHT, ModGuiIcons.CHECK.getAsSprite(16, 16)));
        acceptBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            closeImmediately();
            DLUtils.doIfNotNull(confirm, Runnable::run);
            return false;
        });
    }

    @Override
    protected void onOpen() {
    }
    
    @Override
    protected void onClose() {
    }


    @Override
    public void renderFlyoutContent(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle contentArea) {
        super.renderFlyoutContent(graphics, mouseX, mouseY, contentArea);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 10, height() / 2 - graphics.defaultFont().lineHeight / 2, txtAreYouSure, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
    }
    
}
