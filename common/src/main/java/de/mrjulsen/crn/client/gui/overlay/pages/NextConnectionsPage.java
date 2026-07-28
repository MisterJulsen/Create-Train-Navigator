package de.mrjulsen.crn.client.gui.overlay.pages;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.api.core.BoardEntry;
import de.mrjulsen.crn.client.journey.JourneyTracker;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.core.navigator.route.RouteCall;
import de.mrjulsen.crn.network.packets.GetStationBoardPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class NextConnectionsPage extends AbstractRouteDetailsPage {

    private static final String keyNextConnections = "gui.createrailwaysnavigator.route_overview.next_connections";

    private static final int CONNECTION_ENTRIES_PER_PAGE = 3;
    private static final int TIME_PER_CONNECTIONS_SUBPAGE = 200;
    private static final int MAX_CONNECTIONS = 12;

    private final List<BoardEntry> nextConnections = new ArrayList<>();
    private final Runnable afterFirstCycle;

    private int connectionsSubPageTime = 0;
    private int connectionsSubPageIndex = 0;
    private int connectionsSubPagesCount = 0;
    private int cycles;

    public NextConnectionsPage(JourneyTracker tracker, Runnable afterFirstCycle) {
        super(tracker);
        this.afterFirstCycle = afterFirstCycle;

        RouteCall call = tracker.nextCall();
        if (call.station().tagId() == null) {
            DLUtils.doIfNotNull(afterFirstCycle, Runnable::run);
            return;
        }

        ModNetworkManager.GET_STATION_BOARD.send(NetworkDirection.toServer(), new GetStationBoardPacketData.Request(call.station().tagId(), tracker.currentLeg().trainId(), MAX_CONNECTIONS), (response) -> {
            if (response.getEntries().isEmpty()) {
                DLUtils.doIfNotNull(afterFirstCycle, Runnable::run);
                return;
            }
            nextConnections.addAll(response.getEntries());
            connectionsSubPagesCount = (nextConnections.size() + CONNECTION_ENTRIES_PER_PAGE - 1) / CONNECTION_ENTRIES_PER_PAGE;
        }, () -> {});
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    public boolean hasConnections() {
        return !nextConnections.isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (nextConnections.isEmpty() || connectionsSubPagesCount <= 0) {
            return;
        }
        connectionsSubPageTime++;
        if ((connectionsSubPageTime %= TIME_PER_CONNECTIONS_SUBPAGE) == 0) {
            connectionsSubPageIndex++;
            if ((connectionsSubPageIndex %= connectionsSubPagesCount) == 0) {
                cycles++;
                if (cycles == 1) {
                    DLUtils.doIfNotNull(afterFirstCycle, Runnable::run);
                }
            }
        }
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        GuiUtils.drawString(graphics, font, 5, 4, CustomLanguage.translate(keyNextConnections).withStyle(ChatFormatting.BOLD), DLColor.WHITE, ETextAlignment.LEFT, false);

        int y = 16;
        final int spacing = 5;
        final int timeWidth = 30;
        final int trainNameWidth = 40;
        for (int i = connectionsSubPageIndex * CONNECTION_ENTRIES_PER_PAGE; i < (connectionsSubPageIndex + 1) * CONNECTION_ENTRIES_PER_PAGE && i < nextConnections.size(); i++) {
            BoardEntry entry = nextConnections.get(i);
            String platform = entry.station().platform();
            Component departureTimeText = TextUtils.text(RouteOverviewPage.clockTime(entry.scheduled().departure()));

            GuiUtils.drawString(graphics, font, 5, y, departureTimeText, DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
            GuiUtils.drawString(graphics, font, 5 + timeWidth + spacing, y, TextUtils.truncateWithEllipsis(font, TextUtils.text(entry.displayName()), trainNameWidth), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
            GuiUtils.drawString(graphics, font, width() - 5, y, platform, DLColor.fromInt(0xFFDBDBDB), ETextAlignment.RIGHT, false);
            int terminusWidth = width() - 10 - timeWidth - trainNameWidth - spacing * 3 - font.width(platform);
            GuiUtils.drawString(graphics, font, 5 + timeWidth + trainNameWidth + spacing * 2, y, TextUtils.truncateWithEllipsis(font, TextUtils.text(entry.destinationText()), terminusWidth), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
            y += 12;
        }

        y = 52;
        final int dotSize = 4;
        final int center = width() / 2 - dotSize / 2;
        final int startX = center - (dotSize * 2) * (connectionsSubPagesCount - 1) / 2;

        for (int i = 0; i < connectionsSubPagesCount; i++) {
            if (connectionsSubPageIndex == i) {
                GuiUtils.drawBox(graphics, Rectangle.withSize((startX + (dotSize * 2 * i)) - 1, y - 1, dotSize + 2, dotSize + 2), DLColor.fromInt(0xFFAAAAAA), DLColor.fromInt(0xFFFFFFFF));
            } else {
                GuiUtils.drawBox(graphics, Rectangle.withSize((startX + (dotSize * 2 * i)), y, dotSize, dotSize), DLColor.fromInt(0xFF888888), DLColor.fromInt(0xFFDBDBDB));
            }
        }
    }
}
