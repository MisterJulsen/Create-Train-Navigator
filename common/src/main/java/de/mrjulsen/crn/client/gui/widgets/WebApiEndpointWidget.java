package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.MutableComponent;

public class WebApiEndpointWidget extends DLButton {

    public static final int HEIGHT = 28;

    private final String url;
    private final MutableComponent description;
    private final MutableComponent copiedText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".web_api.copied");

    public WebApiEndpointWidget(int x, int y, int width, String url, String description) {
        super(x, y, width, HEIGHT);
        this.url = url;
        this.description = TextUtils.text(description).withStyle(ChatFormatting.GRAY);

        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(url);
            Minecraft.getInstance().getToasts().addToast(new SystemToast(
                SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                copiedText,
                TextUtils.text(url)
            ));
            return false;
        });
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, width(), height(), ColorShade.DARK.getColor());
        if (isSelected() || isMouseOver(mouseX, mouseY)) {
            GuiUtils.fill(graphics, 0, 0, width(), height(), DLColor.fromInt(0x33FFFFFF));
        }

        int padding = 5;
        GuiUtils.drawString(
            graphics,
            graphics.defaultFont(),
            padding,
            4,
            TextUtils.truncateWithEllipsis(graphics.defaultFont(), url, width() - padding * 2),
            DLColor.fromInt(0xFF55FFFF),
            ETextAlignment.LEFT,
            false
        );
        GuiUtils.drawString(
            graphics,
            graphics.defaultFont(),
            padding,
            4 + graphics.defaultFont().lineHeight + 1,
            TextUtils.truncateWithEllipsis(graphics.defaultFont(), description, width() - padding * 2),
            DLColor.fromInt(ChatFormatting.GRAY.getColor().intValue()),
            ETextAlignment.LEFT,
            false
        );
    }
}
