package de.mrjulsen.crn.client.gui.overlay.pages;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.registry.ModAccessorTypes.DeparturesData;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.ChatFormatting;

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
        
        DataAccessor.getFromServer(new DeparturesData(route.getCurrentPart().getNextStop().getClientTag().tagId(), route.getCurrentPart().getNextStop().getTrainId()), ModAccessorTypes.GET_DEPARTURES_AT, (stops) -> {
            if (stops.isEmpty()) {
                afterFirstCycle.run();
                return;
            }
            nextConnections.addAll(stops);
            connectionsSubPagesCount = nextConnections.size() / CONNECTION_ENTRIES_PER_PAGE + (nextConnections.size() % CONNECTION_ENTRIES_PER_PAGE == 0 ? 0 : 1);
        });
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
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        GuiUtils.drawString(graphics, font, 5, 4, ELanguage.translate(keyNextConnections).withStyle(ChatFormatting.BOLD), 0xFFFFFFFF, EAlignment.LEFT, false);

        int y = 16;
        final int spacing = 5;
        final int timeWidth = 30;
        final int trainNameWidth = 40;
        for (int i = connectionsSubPageIndex * CONNECTION_ENTRIES_PER_PAGE; i < (connectionsSubPageIndex + 1) * CONNECTION_ENTRIES_PER_PAGE && i < nextConnections.size(); i++) {
            TrainStop stop = nextConnections.get(i);
            String terminus = stop.getDisplayTitle();
            GuiUtils.drawString(graphics, font, 5, y, TimeUtils.parseTime(stop.getScheduledDepartureTime(), ModClientConfig.TIME_FORMAT.get()), 0xFFDBDBDB, EAlignment.LEFT, false);
            GuiUtils.drawString(graphics, font, 5 + timeWidth + spacing, y, GuiUtils.ellipsisString(font, TextUtils.text(stop.getTrainName()), trainNameWidth), 0xFFDBDBDB, EAlignment.LEFT, false);
            GuiUtils.drawString(graphics, font, width() - 5, y, stop.getRealTimeStationTag().info().platform(), 0xFFDBDBDB, EAlignment.RIGHT, false);
            int terminusWidth = width() - 10 + timeWidth + trainNameWidth + spacing * 3 - font.width(stop.getRealTimeStationTag().info().platform());
            GuiUtils.drawString(graphics, font, 5 + timeWidth + trainNameWidth + spacing * 2, y, GuiUtils.ellipsisString(font, TextUtils.text(terminus), terminusWidth), 0xFFDBDBDB, EAlignment.LEFT, false);
            y += 12;
        }

        y = 52;
        final int dotSize = 4;
        final int center = width() / 2 - dotSize / 2;
        final int startX = center - (dotSize * 2) * (connectionsSubPagesCount - 1);

        for (int i = 0; i < connectionsSubPagesCount; i++) {
            if (connectionsSubPageIndex == i) {
                GuiUtils.drawBox(graphics, new GuiAreaDefinition((startX + (dotSize * 2 * i)) - 1, y - 1, dotSize + 2, dotSize + 2), 0xFFAAAAAA, 0xFFFFFFFF);
            } else {
                GuiUtils.drawBox(graphics, new GuiAreaDefinition((startX + (dotSize * 2 * i)), y, dotSize, dotSize), 0xFF888888, 0xFFDBDBDB);
            }
        }
    }
}
