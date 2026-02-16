package de.mrjulsen.crn.client.gui.overlay.pages;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineLabel;

public class TrainCancelledInfo extends AbstractRouteDetailsPage {

    private static final String keyTrainCancelled = "gui.createrailwaysnavigator.route_overview.train_cancelled_title";
    private static final String keyTrainCancelledInfoText = "gui.createrailwaysnavigator.route_overview.train_cancelled_info";
    
    private MultiLineLabel messageLabel;

    public TrainCancelledInfo(ClientRoute route, String trainName) {
        super(route);
        this.messageLabel = MultiLineLabel.create(font, TextUtils.translate(keyTrainCancelledInfoText, trainName), width() - 10);
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        int y = 3;
        // Title
        ModGuiIcons.CROSS.render(graphics, 5, y);
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y + ModGuiIcons.ICON_SIZE / 2 - font.lineHeight / 2, TextUtils.translate(keyTrainCancelled).withStyle(ChatFormatting.BOLD), Constants.COLOR_DELAYED, ETextAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(graphics.graphics(), 10, y, font.lineHeight, 0xFFDBDBDB);
    }
    
}
