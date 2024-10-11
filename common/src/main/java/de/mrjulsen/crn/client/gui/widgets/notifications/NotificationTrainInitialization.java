package de.mrjulsen.crn.client.gui.widgets.notifications;

import java.util.function.Consumer;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractNotificationPopup;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.Util;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.MutableComponent;

public class NotificationTrainInitialization extends AbstractNotificationPopup {

    private final float scale = 0.75f;
    private final int maxTextWidth;
    private final MutableComponent text = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.train_initialization_warning");

    public NotificationTrainInitialization(DLScreen screen, int x, int y, int width, Consumer<GuiEventListener> removeWidgetFunc) {
        super(screen, x, y, width, 1, ColorShade.LIGHT, removeWidgetFunc);
        this.maxTextWidth = (int)(width / scale) - 10 - ModGuiIcons.ICON_SIZE - 12 - DLIconButton.DEFAULT_BUTTON_WIDTH * 2;
        set_height((int)((ClientWrapper.getTextBlockHeight(font, text, maxTextWidth)) * scale) + 10);
        set_y(y() - height());

        DLIconButton closeBtn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.X.getAsSprite(16, 16), x() + width() - DLIconButton.DEFAULT_BUTTON_WIDTH - 2, y() + 2, DLIconButton.DEFAULT_BUTTON_WIDTH, height() - 4, TextUtils.empty(), (b) -> {
            close();
        }));
        DLIconButton helpBtn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.HELP.getAsSprite(16, 16), x() + width() - DLIconButton.DEFAULT_BUTTON_WIDTH * 2 - 2, y() + 2, DLIconButton.DEFAULT_BUTTON_WIDTH, height() - 4, TextUtils.empty(), (b) -> {
            Util.getPlatform().openUri(Constants.HELP_PAGE_NAVIGATION_WARNING);
        }));
        closeBtn.setBackColor(0);
        helpBtn.setBackColor(0);
    }

    @Override
    public void renderFlyoutContent(Graphics graphics, int mouseX, int mouseY, float partialTicks, GuiAreaDefinition contentArea) {
        super.renderFlyoutContent(graphics, mouseX, mouseY, partialTicks, contentArea);
        ModGuiIcons.WARN.render(graphics, x() + 4, y() + 4);
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(x() + 5 + 2 + ModGuiIcons.ICON_SIZE, y() + 5, 0);
        graphics.poseStack().scale(scale, scale, scale);
        ClientWrapper.renderMultilineLabelSafe(graphics, 0, 0, font, text, maxTextWidth, 0xFFFF9999);
        graphics.poseStack().popPose();
    }    
}
