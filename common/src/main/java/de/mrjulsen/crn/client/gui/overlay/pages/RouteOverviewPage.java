package de.mrjulsen.crn.client.gui.overlay.pages;

import java.util.Arrays;
import java.util.List;

import de.mrjulsen.crn.api.core.CallDirection;
import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.journey.JourneyTracker;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModServerConfig;
import de.mrjulsen.crn.core.navigator.route.RouteCall;
import de.mrjulsen.crn.core.navigator.route.RouteLeg;
import de.mrjulsen.crn.core.navigator.route.RouteTransfer;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.MutableComponent;

public class RouteOverviewPage extends AbstractRouteDetailsPage {

    public static final int ENTRY_HEIGHT = 14;
    private static final int MAX_ENTRIES = 5;

    private static final MutableComponent textTransfer = TextUtils.translate("gui.createrailwaysnavigator.route_overview.schedule_transfer");
    private static final MutableComponent textConnectionEndangered = TextUtils.translate("gui.createrailwaysnavigator.route_overview.connection_endangered");
    private static final MutableComponent textConnectionMissed = TextUtils.translate("gui.createrailwaysnavigator.route_overview.connection_missed");

    public RouteOverviewPage(JourneyTracker tracker) {
        super(tracker);
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        List<RouteLeg> legs = route().legs();
        int y = -2;
        int rendered = 0;

        for (int i = tracker.currentLegIndex(); i < legs.size() && rendered < MAX_ENTRIES; i++) {
            RouteLeg leg = legs.get(i);
            boolean reachable = route().isLegReachable(i);
            List<RouteCall> calls = leg.calls();
            boolean firstOfLeg = true;

            for (int k = 0; k < calls.size() && rendered < MAX_ENTRIES; k++) {
                RouteCall call = calls.get(k);
                if (tracker.isCallDone(leg, call)) {
                    continue;
                }

                boolean boarding = k == 0;
                RoutePathIcons icon = firstOfLeg && i > tracker.currentLegIndex() ? RoutePathIcons.TRANSFER_STOP : RoutePathIcons.STOP;
                renderStation(graphics, y, width(), font, call, icon, boarding, !reachable);
                y += RoutePathIcons.SPRITE_HEIGHT;
                rendered++;
                firstOfLeg = false;

                if (k == calls.size() - 1 && i < legs.size() - 1 && i < route().transfers().size()) {
                    renderTransfer(graphics, y, width(), font, route().transfers().get(i));
                    y += RoutePathIcons.SPRITE_HEIGHT;
                }
            }
        }
    }

    public static void renderStation(DLGuiGraphics graphics, int y, int width, Font font, RouteCall call, RoutePathIcons icon, boolean boarding, boolean isMissed) {
        final int precision = ModServerConfig.REALTIME_PRECISION_THRESHOLD.get();

        long scheduledTime = boarding ? call.scheduled().departure() : call.scheduled().arrival();
        long deviation = boarding ? call.departureDeviation() : call.arrivalDeviation();
        boolean delayed = boarding ? call.isDelayed(CallDirection.DEPARTURE) : call.isDelayed(CallDirection.ARRIVAL);

        String scheduledTimeText = clockTime(scheduledTime);
        String currentTimeText = clockTime(scheduledTime + (deviation / precision * precision));
        String platform = call.platform();

        GuiUtils.drawString(graphics, font, 7, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, TextUtils.text(scheduledTimeText).withStyle(isMissed ? ChatFormatting.STRIKETHROUGH : ChatFormatting.RESET), isMissed ? Constants.COLOR_DELAYED : DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
        if (call.hasTimes() && !isMissed) {
            GuiUtils.drawString(graphics, font, 7 + 32, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, TextUtils.text(currentTimeText), delayed ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, ETextAlignment.LEFT, false);
        }
        icon.getAsSprite().render(graphics, 10 + 64, y);
        GuiUtils.drawString(graphics, font, 17 + 64 + RoutePathIcons.SPRITE_WIDTH, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, TextUtils.truncateWithEllipsis(font, TextUtils.text(call.station().displayName()), width - (17 + 64 + RoutePathIcons.SPRITE_WIDTH) - font.width(platform) - 10), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, width - 4, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, platform, call.isDiverted() ? Constants.COLOR_DELAYED : DLColor.fromInt(0xFFDBDBDB), ETextAlignment.RIGHT, false);
    }

    public static void renderTransfer(DLGuiGraphics graphics, int y, int width, Font font, RouteTransfer transfer) {
        if (transfer.isMissed()) {
            ModGuiIcons.CROSS.getAsSprite(16, 16).render(graphics, 5, y + ENTRY_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
            GuiUtils.drawString(graphics, font, 17 + 64 + RoutePathIcons.SPRITE_WIDTH, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, textConnectionMissed.copy().withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.RED), DLColor.WHITE, ETextAlignment.LEFT, false);
        } else if (transfer.isEndangered()) {
            ModGuiIcons.WARN.getAsSprite(16, 16).render(graphics, 5, y + ENTRY_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
            GuiUtils.drawString(graphics, font, 17 + 64 + RoutePathIcons.SPRITE_WIDTH, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, textConnectionEndangered.copy().withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GOLD), DLColor.WHITE, ETextAlignment.LEFT, false);
        } else {
            String transferTimeText = ModUtils.formatDuration(transfer.duration());
            GuiUtils.drawString(graphics, font, 7, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, TextUtils.text(transferTimeText).withStyle(ChatFormatting.ITALIC), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
            GuiUtils.drawString(graphics, font, 17 + 64 + RoutePathIcons.SPRITE_WIDTH, y + ENTRY_HEIGHT - 2 - font.lineHeight / 2, textTransfer.copy().withStyle(ChatFormatting.ITALIC), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
        }
        RoutePathIcons.TRANSFER.getAsSprite().render(graphics, 10 + 64, y);
    }

    static String clockTime(long ticks) {
        return new DLTime(ticks, VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME, DLTime.defaultTimeSystem());
    }

    public static enum RoutePathIcons {
        CURRENT(0),
        STOP(1),
        TRANSFER_STOP(2),
        TRANSFER(3),
        START(4);

        private static final int V = 30;
        private static final int START_U = 21;
        static final int SPRITE_WIDTH = 7;
        static final int SPRITE_HEIGHT = 14;
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

        public DLSprite getAsSprite() {
            return new DLSprite(CRNGui.GUI, SPRITE_WIDTH, SPRITE_HEIGHT, START_U + getIndex() * SPRITE_WIDTH, V, SPRITE_WIDTH, SPRITE_HEIGHT);
        }
    }
}
