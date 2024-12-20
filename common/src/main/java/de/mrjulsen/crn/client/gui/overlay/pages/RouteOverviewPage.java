package de.mrjulsen.crn.client.gui.overlay.pages;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.ClientRoutePart;
import de.mrjulsen.crn.data.navigation.TransferConnection;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.MutableComponent;

public class RouteOverviewPage extends AbstractRouteDetailsPage {

    public static final int ENTRY_HEIGHT = 14;

    private static final MutableComponent textTransfer = TextUtils.translate("gui.createrailwaysnavigator.route_overview.schedule_transfer");
    private static final MutableComponent textConnectionEndangered = TextUtils.translate("gui.createrailwaysnavigator.route_overview.connection_endangered");
    private static final MutableComponent textConnectionMissed = TextUtils.translate("gui.createrailwaysnavigator.route_overview.connection_missed");

    public RouteOverviewPage(ClientRoute route) {
        super(route);
    }

    @Override
    public boolean isImportant() {
        return false;
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        int y = -2;
        int n = 0;
        List<ClientRoutePart> parts = route.getClientParts().stream().skip(Math.max(route.getCurrentPartIndex(), 0)).toList();
        for (int i = 0; i < parts.size(); i++) {
            ClientRoutePart part = parts.get(i);
            int toSkip = Math.max(part.getNextStopIndex(), 0);
            List<TrainStop> stops = part.getAllStops().stream().skip(toSkip).toList();
            for (int k = 0; k < stops.size() && n < 5; k++, n++) {
                TrainStop stop = stops.get(k);
                renderStation(graphics, y, width(), font, stop, toSkip <= 0 && i > 0 && k == 0 ? RoutePathIcons.TRANSFER_STOP : RoutePathIcons.STOP, stop == part.getFirstStop(), !route.isPartReachable(part));
                y += RoutePathIcons.SPRITE_HEIGHT;
                if (i < parts.size() - 1 && k >= stops.size() - 1) {
                    Optional<TransferConnection> connection = route.getConnectionWith(stop);
                    if (connection.isPresent()) {
                        renderTransfer(graphics, y, width(), font, connection.get());
                    } 
                    y += RoutePathIcons.SPRITE_HEIGHT;                   
                } 
            }
        }
    }

    public static void renderStation(Graphics graphics, int y, int width, Font font, TrainStop stop, RoutePathIcons icon, boolean isStart, boolean isMissed) {
        final int precision = ModClientConfig.REALTIME_PRECISION_THRESHOLD.get();
        GuiUtils.drawString(graphics, font, 7, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, TextUtils.text(TimeUtils.parseTime((isStart ? stop.getScheduledDepartureTime() : stop.getScheduledArrivalTime()) + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get())).withStyle(isMissed ? ChatFormatting.STRIKETHROUGH : ChatFormatting.RESET), isMissed ? Constants.COLOR_DELAYED : 0xFFDBDBDB, EAlignment.LEFT, false);
        if (stop.shouldRenderRealTime() && !isMissed) {
            GuiUtils.drawString(graphics, font, 7 + 32, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, TimeUtils.parseTime((isStart ? stop.getScheduledDepartureTime() + (stop.getDepartureTimeDeviation() / precision * precision) : stop.getScheduledArrivalTime() + (stop.getArrivalTimeDeviation() / precision * precision)) + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get()), stop.isArrivalDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        }
        icon.getAsSprite().render(graphics, 10 + 64, y);
        GuiUtils.drawString(graphics, font, 17 + 64 + RoutePathIcons.SPRITE_WIDTH, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, GuiUtils.ellipsisString(font, TextUtils.text(stop.getClientTag().tagName()), width - (17 + 64 + RoutePathIcons.SPRITE_WIDTH) - font.width(stop.getRealTimeStationTag().info().platform()) - 10), 0xFFDBDBDB, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, width - 4, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, stop.getRealTimeStationTag().info().platform(), stop.isStationInfoChanged() ? Constants.COLOR_DELAYED : 0xFFDBDBDB, EAlignment.RIGHT, false);

    }

    public static void renderTransfer(Graphics graphics, int y, int width, Font font, TransferConnection connection) {
        if (connection.isConnectionMissed()) {
            ModGuiIcons.CROSS.getAsSprite(16, 16).render(graphics, 5, y + ENTRY_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
            GuiUtils.drawString(graphics, font, 17 + 64 + RoutePathIcons.SPRITE_WIDTH, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, textConnectionMissed.withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.RED), 0xFFFFFFFF, EAlignment.LEFT, false);
        } else if (connection.isConnectionEndangered()) {
            ModGuiIcons.WARN.getAsSprite(16, 16).render(graphics, 5, y + ENTRY_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
            GuiUtils.drawString(graphics, font, 17 + 64 + RoutePathIcons.SPRITE_WIDTH, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, textConnectionEndangered.withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GOLD), 0xFFFFFFFF, EAlignment.LEFT, false);
        } else {
            GuiUtils.drawString(graphics, font, 7, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, TextUtils.text(TimeUtils.parseDurationShort((int)connection.getRealTimeTransferTime())).withStyle(ChatFormatting.ITALIC), 0xFFDBDBDB, EAlignment.LEFT, false);
            GuiUtils.drawString(graphics, font, 17 + 64 + RoutePathIcons.SPRITE_WIDTH, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, textTransfer.withStyle(ChatFormatting.ITALIC), 0xFFDBDBDB, EAlignment.LEFT, false);
        }
        RoutePathIcons.TRANSFER.getAsSprite().render(graphics, 10 + 64, y);
    }

    public static enum RoutePathIcons {
        CURRENT(0),
        STOP(1),
        TRANSFER_STOP(2),
        TRANSFER(3),
        START(4);

        private static final int V = 30;
        private static final int START_U = 21;
        private static final int SPRITE_WIDTH = 7;
        private static final int SPRITE_HEIGHT = 14;
        private int index;

        private RoutePathIcons(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public static RoutePathIcons getByIndex(int index) {
            return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(START);
        }

        public Sprite getAsSprite() {
            return new Sprite(CRNGui.GUI, CRNGui.GUI_WIDTH, CRNGui.GUI_HEIGHT, START_U + getIndex() * SPRITE_WIDTH, V, SPRITE_WIDTH, SPRITE_HEIGHT, SPRITE_WIDTH, SPRITE_HEIGHT, 0, 0);
        }
    }
}
