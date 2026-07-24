package de.mrjulsen.crn.client.gui.overlay.pages;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.overlay.pages.RouteOverviewPage.RoutePathIcons;
import de.mrjulsen.crn.client.journey.JourneyTracker;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.core.navigator.route.RouteCall;
import de.mrjulsen.crn.core.navigator.route.RouteLeg;
import de.mrjulsen.crn.core.navigator.route.RouteTransfer;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;

public class TransferPage extends AbstractRouteDetailsPage {

    private static final String keyScheduleTransfer = "gui.createrailwaysnavigator.route_overview.schedule_transfer";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyTransferWithPlatform = "gui.createrailwaysnavigator.route_overview.transfer_with_platform";
    private static final String keyTimeNow = "gui.createrailwaysnavigator.time.now";

    private final RouteLeg connectingLeg;
    private final RouteTransfer transfer;
    private final MultiLineLabel messageLabel;

    public TransferPage(JourneyTracker tracker, RouteTransfer transfer, RouteLeg connectingLeg) {
        super(tracker);
        this.transfer = transfer;
        this.connectingLeg = connectingLeg;

        this.messageLabel = MultiLineLabel.create(font, transferMessage(connectingLeg), width() - (15 + ModGuiIcons.ICON_SIZE));
    }

    static Component transferMessage(RouteLeg connectingLeg) {
        String platform = connectingLeg.boarding().realtimePlatform();
        return platform == null || platform.isBlank()
            ? CustomLanguage.translate(keyTransfer, connectingLeg.displayName(), connectingLeg.destinationText())
            : CustomLanguage.translate(keyTransferWithPlatform, connectingLeg.displayName(), connectingLeg.destinationText(), platform);
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        RouteCall boarding = connectingLeg.boarding();
        int y = 0;
        RouteOverviewPage.renderStation(graphics, -4, width(), font, boarding, RoutePathIcons.START, true, !transfer.state().isReachable());
        y += 16;
        GuiUtils.fill(graphics, 0, y, width(), 1, DLColor.WHITE);

        ModGuiIcons.WALK.render(graphics, 5, y + 3);
        long transferTime = boarding.realtime().departure() - ModUtils.getTransformedWorldTime();
        Component transferTimeText = TextUtils.text(new DLTime(transferTime, VanillaTimeSystem.INSTANCE).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME, DLTime.defaultTimeSystem()));
        GuiUtils.drawString(graphics, font, 10 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - font.lineHeight / 2, CustomLanguage.translate(keyScheduleTransfer).append(" ").append(transferTime > 0 ? transferTimeText : CustomLanguage.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), DLColor.WHITE, ETextAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;

        this.messageLabel.renderLeftAligned(graphics.graphics(), 10 + ModGuiIcons.ICON_SIZE, y, font.lineHeight, 0xFFDBDBDB);
    }
}
