package de.mrjulsen.crn.client.gui.overlay.pages;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.overlay.pages.RouteOverviewPage.RoutePathIcons;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class WelcomePage extends AbstractRouteDetailsPage {

    private static final String keyDepartureIn = "gui.createrailwaysnavigator.route_details.departure";
    private static final String keyTimeNow = "gui.createrailwaysnavigator.time.now";
    private static final String keyTransferCount = "gui.createrailwaysnavigator.navigator.route_entry.transfer";

    public WelcomePage(ClientRoute route) {
        super(route);
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        int y = 16;
        RouteOverviewPage.renderStation(graphics, -4, width(), font, route.getStart(), RoutePathIcons.START, true, false);
        GuiUtils.fill(graphics, 0, y, width(), 1, 0xFFDBDBDB);
        
        // Title
        ModGuiIcons.TIME.render(graphics, 5, y + 3);
        long time = route.getCurrentPart().departureIn();
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - font.lineHeight / 2, ELanguage.translate(keyDepartureIn).append(" ").append(time > 0 ? TextUtils.text(TimeUtils.parseDurationShort((int)time)) : ELanguage.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), 0xFFFFFFFF, EAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        final int detailsLineHeight = 12;
        //StationEntry station = taggedRoute[0];
        ClientTrainStop endStation = route.getLastClientPart().getLastClientStop();

        Component platformText = TextUtils.text(endStation.getRealTimeStationTag().info().platform());
        int platformTextWidth = font.width(platformText);
        final int maxStationNameWidth = width() - platformTextWidth - 10 - 5;
        MutableComponent stationText = TextUtils.text(ModUtils.formatTime(endStation.getRoundedRealTimeArrivalTime(), false)).append(TextUtils.text(" " + endStation.getClientTag().tagName()));
        if (font.width(stationText) > maxStationNameWidth) {
            stationText = TextUtils.text(font.substrByWidth(stationText, maxStationNameWidth).getString()).append(TextUtils.text("...")).withStyle(stationText.getStyle());
        }

        ModGuiIcons.TARGET.render(graphics, 5, y + font.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y, stationText, 0xFFDBDBDB,  EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, width() - 5, y, platformText, endStation.isStationInfoChanged() ? Constants.COLOR_DELAYED : 0xFFDBDBDB, EAlignment.RIGHT, false);
        ModGuiIcons.INFO.render(graphics, 5, y + detailsLineHeight + font.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y + detailsLineHeight, TextUtils.text(String.format("%s %s | %s",
            route.getTransferCount(),
            ELanguage.translate(keyTransferCount).getString(),
            TimeUtils.parseDurationShort((int)route.travelTime())
        )), 0xFFDBDBDB, EAlignment.LEFT, false);
    }
}
