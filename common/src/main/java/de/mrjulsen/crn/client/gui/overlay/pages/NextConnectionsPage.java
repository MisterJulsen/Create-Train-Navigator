package de.mrjulsen.crn.client.gui.overlay.pages;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.network.packets.pain.GetDeparturesAtPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class NextConnectionsPage extends AbstractRouteDetailsPage {

    private final List<TrainStop> nextConnections = new ArrayList<>();
    private static final String keyNextConnections = "gui.createrailwaysnavigator.route_overview.next_connections";
    
    private static final int CONNECTION_ENTRIES_PER_PAGE = 3;
    private static final int TIME_PER_CONNECTIONS_SUBPAGE = 200;
    private int connectionsSubPageTime = 0;
    private int connectionsSubPageIndex = 0;
    private int connectionsSubPagesCount = 0;
    private final Runnable afterFirstCycle;
    private int cycles;

    public NextConnectionsPage(ClientRoute route, Runnable afterFirstCycle) {
        super(route);
        this.afterFirstCycle = afterFirstCycle;

        ModNetworkManager.GET_DEPARTURES_AT.send(NetworkDirection.toServer(), new GetDeparturesAtPacketData.Request(route.getCurrentPart().getNextStop().getRealTimeStationTag().tagId(), route.getCurrentPart().getNextStop().getTrainId(), true, false /* TODO CUSTOM SETTINGS */), (response) -> {
            List<ClientTrainStop> stops = response.getData();
            
            if (stops.isEmpty()) {
                afterFirstCycle.run();
                return;
            }
            nextConnections.addAll(stops);
            connectionsSubPagesCount = nextConnections.size() / CONNECTION_ENTRIES_PER_PAGE + (nextConnections.size() % CONNECTION_ENTRIES_PER_PAGE == 0 ? 0 : 1);
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
        if (nextConnections.isEmpty()) {
            return;
        }
        connectionsSubPageTime++;
        if ((connectionsSubPageTime %= TIME_PER_CONNECTIONS_SUBPAGE) == 0) {
            connectionsSubPageIndex++;
            if ((connectionsSubPageIndex %= connectionsSubPagesCount) == 0) {
                cycles++;
                if (cycles == 1) {
                    DLUtils.doIfNotNull(afterFirstCycle, x -> x.run());
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
            TrainStop stop = nextConnections.get(i);
            String terminus = stop.getDisplayTitle();
            Component departureTimeText = TextUtils.text(DLTime.fromTicks(stop.getScheduledDepartureTime(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME));

            GuiUtils.drawString(graphics, font, 5, y, departureTimeText, DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
            GuiUtils.drawString(graphics, font, 5 + timeWidth + spacing, y, TextUtils.truncateWithEllipsis(font, TextUtils.text(stop.getTrainName()), trainNameWidth), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
            GuiUtils.drawString(graphics, font, width() - 5, y, stop.getRealTimeStationTag().info().platform(), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.RIGHT, false);
            int terminusWidth = width() - 10 + timeWidth + trainNameWidth + spacing * 3 - font.width(stop.getRealTimeStationTag().info().platform());
            GuiUtils.drawString(graphics, font, 5 + timeWidth + trainNameWidth + spacing * 2, y, TextUtils.truncateWithEllipsis(font, TextUtils.text(terminus), terminusWidth), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
            y += 12;
        }

        y = 52;
        final int dotSize = 4;
        final int center = width() / 2 - dotSize / 2;
        final int startX = center - (dotSize * 2) * (connectionsSubPagesCount - 1);

        for (int i = 0; i < connectionsSubPagesCount; i++) {
            if (connectionsSubPageIndex == i) {
                GuiUtils.drawBox(graphics, Rectangle.withSize((startX + (dotSize * 2 * i)) - 1, y - 1, dotSize + 2, dotSize + 2), DLColor.fromInt(0xFFAAAAAA), DLColor.fromInt(0xFFFFFFFF));
            } else {
                GuiUtils.drawBox(graphics, Rectangle.withSize((startX + (dotSize * 2 * i)), y, dotSize, dotSize), DLColor.fromInt(0xFF888888), DLColor.fromInt(0xFFDBDBDB));
            }
        }
    }
}
