package de.mrjulsen.crn.client.gui.overlay.pages;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.overlay.pages.RouteOverviewPage.RoutePathIcons;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.TransferConnection;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineLabel;

public class TransferPage extends AbstractRouteDetailsPage {

    private static final String keyScheduleTransfer = "gui.createrailwaysnavigator.route_overview.schedule_transfer";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyTransferWithPlatform = "gui.createrailwaysnavigator.route_overview.transfer_with_platform";
    private static final String keyTimeNow = "gui.createrailwaysnavigator.time.now";

    private final TransferConnection connection;
    private MultiLineLabel messageLabel;

    public TransferPage(ClientRoute route, TransferConnection connection) {
        super(route);
        this.connection = connection;

        String terminus = connection.getDepartureStation().getDisplayTitle();
        StationInfo info = connection.getDepartureStation().getRealTimeStationTag().info();
        this.messageLabel = MultiLineLabel.create(font,
        info.platform() == null || info.platform().isBlank() ?
        ELanguage.translate(keyTransfer,
            connection.getDepartureStation().getTrainName(),
            terminus
        ) : 
        ELanguage.translate(keyTransferWithPlatform,
        connection.getDepartureStation().getTrainName(),
            terminus,
            info.platform()
        ), width - (15 + ModGuiIcons.ICON_SIZE));
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        int y = 0;
        RouteOverviewPage.renderStation(graphics, -4, width(), font, connection.getDepartureStation(), RoutePathIcons.START, true, connection.isConnectionMissed());
        y += 16;
        GuiUtils.fill(graphics, 0, y, width(), 1, 0xFFDBDBDB);
        
        // Title
        ModGuiIcons.WALK.render(graphics, 5, y + 3);        
        long transferTime = connection.getDepartureStation().getRealTimeDepartureTime() - DragonLib.getCurrentWorldTime();
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - font.lineHeight / 2, ELanguage.translate(keyScheduleTransfer).append(" ").append(transferTime > 0 ? TextUtils.text(TimeUtils.parseDurationShort((int)transferTime)) : ELanguage.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), 0xFFFFFFFF, EAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(graphics.graphics(), 10 + ModGuiIcons.ICON_SIZE, y, font.lineHeight, 0xFFDBDBDB);
    }
    
}
