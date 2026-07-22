package de.mrjulsen.crn.client.gui.overlay.pages;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.journey.JourneyTracker;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineLabel;

public class ConnectionMissedPage extends AbstractRouteDetailsPage {

    private static final String keyConnectionMissed = "gui.createrailwaysnavigator.route_overview.connection_missed";
    private static final String keyConnectionMissedPageText = "gui.createrailwaysnavigator.route_overview.journey_interrupted_info";

    private final MultiLineLabel messageLabel;

    public ConnectionMissedPage(JourneyTracker tracker) {
        super(tracker);
        this.messageLabel = MultiLineLabel.create(font, TextUtils.translate(keyConnectionMissedPageText, route().destination().displayName()), width() - 10);
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        int y = 3;
        ModGuiIcons.CROSS.render(graphics, 5, y);
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y + ModGuiIcons.ICON_SIZE / 2 - font.lineHeight / 2, TextUtils.translate(keyConnectionMissed).withStyle(ChatFormatting.BOLD), Constants.COLOR_DELAYED, ETextAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;

        this.messageLabel.renderLeftAligned(graphics.graphics(), 10, y, font.lineHeight, 0xFFDBDBDB);
    }
}
