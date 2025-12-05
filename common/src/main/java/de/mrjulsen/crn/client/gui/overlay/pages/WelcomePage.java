package de.mrjulsen.crn.client.gui.overlay.pages;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.overlay.pages.RouteOverviewPage.RoutePathIcons;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
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
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        int y = 16;
        RouteOverviewPage.renderStation(graphics, -4, width(), font, route.getStart(), RoutePathIcons.START, true, false);
        GuiUtils.fill(graphics, 0, y, width(), 1, DLColor.fromInt(0xFFDBDBDB));
        
        // Title
        ModGuiIcons.TIME.render(graphics, 5, y + 3);
        long departureTicks = route.getCurrentPart().departureIn();
        Component time = TextUtils.text(DLTime.fromTicks(departureTicks, new ConfiguredTimeSystem()).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME));
        
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - font.lineHeight / 2, CustomLanguage.translate(keyDepartureIn).append(" ").append(departureTicks > 0 ? time : CustomLanguage.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), DLColor.WHITE, ETextAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        final int detailsLineHeight = 12;
        //StationEntry station = taggedRoute[0];
        ClientTrainStop endStation = route.getLastClientPart().getLastClientStop();

        Component platformText = TextUtils.text(endStation.getRealTimeStationTag().info().platform());
        int platformTextWidth = font.width(platformText);
        final int maxStationNameWidth = width() - platformTextWidth - 10 - 5;
        String timeText = DLTime.fromTicks(endStation.getRoundedRealTimeArrivalTime(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME);
        MutableComponent stationText = TextUtils.text(timeText).append(TextUtils.text(" " + endStation.getRealTimeStationTag().tagName()));
        if (font.width(stationText) > maxStationNameWidth) {
            stationText = TextUtils.text(font.substrByWidth(stationText, maxStationNameWidth).getString()).append(TextUtils.text("...")).withStyle(stationText.getStyle());
        }

        ModGuiIcons.TARGET.render(graphics, 5, y + font.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y, stationText, DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, width() - 5, y, platformText, endStation.isStationInfoChanged() ? Constants.COLOR_DELAYED : DLColor.fromInt(0xFFDBDBDB), ETextAlignment.RIGHT, false);
        ModGuiIcons.INFO.render(graphics, 5, y + detailsLineHeight + font.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y + detailsLineHeight, TextUtils.text(String.format("%s %s | %s",
            route.getTransferCount(),
            CustomLanguage.translate(keyTransferCount).getString(),
            DLTime.fromTicks(route.travelTime(), new ConfiguredTimeSystem()).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME)
        )), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
    }
}
