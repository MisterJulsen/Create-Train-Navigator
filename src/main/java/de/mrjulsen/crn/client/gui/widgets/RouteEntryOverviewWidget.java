package de.mrjulsen.crn.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.DynamicWidgets;
import de.mrjulsen.crn.client.gui.DynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.screen.NavigatorScreen;
import de.mrjulsen.crn.client.gui.screen.RouteDetailsScreen;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleRoute.SimpleRoutePart;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.event.listeners.JourneyListener.State;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;

public class RouteEntryOverviewWidget extends Button {

    public static final int WIDTH = 200;
    public static final int HEIGHT = 54;

    private static final int DISPLAY_WIDTH = 190;
    
    private final SimpleRoute route;
    private final NavigatorScreen parent;
    private final Level level;
    private final int lastRefreshedTime;

    private static final String transferText = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.route_entry.transfer").getString();
    private static final MutableComponent connectionInPast = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.route_entry.connection_in_past");
    private static final MutableComponent trainCancelled = Utils.translate("gui.createrailwaysnavigator.route_overview.stop_cancelled");

    public RouteEntryOverviewWidget(NavigatorScreen parent, Level level, int lastRefreshedTime, int pX, int pY, SimpleRoute route, OnPress pOnPress) {
        super(pX, pY, WIDTH, HEIGHT, new TextComponent(route.getName()), pOnPress); // 48
        this.route = route;
        this.parent = parent;
        this.level = level;
        this.lastRefreshedTime = lastRefreshedTime;
    }


    @Override
    public void onClick(double pMouseX, double pMouseY) {
        super.onClick(pMouseX, pMouseY);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new RouteDetailsScreen(parent, level, route, null));
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        
        final float scale = 0.75f;
        float l = isMouseOver(pMouseX, pMouseY) ? 0.1f : 0;
        boolean beforeJourney = JourneyListenerManager.get(route.getListenerId(), null) != null && JourneyListenerManager.get(route.getListenerId(), null).getCurrentState() == State.BEFORE_JOURNEY;
        
        int color = ModGuiUtils.lightenColor(ColorShade.DARK.getColor(), l);
        if (!beforeJourney) {
            color = ModGuiUtils.applyTint(color, 0x663300);
        }
        DynamicWidgets.renderSingleShadeWidget(pPoseStack, x, y, WIDTH, HEIGHT, color);
        DynamicWidgets.renderHorizontalSeparator(pPoseStack, x + 6, y + 22, 188);

        Minecraft minecraft = Minecraft.getInstance();
        SimpleRoutePart[] parts = route.getParts().toArray(SimpleRoutePart[]::new);
        Font shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        String timeStart = TimeUtils.parseTime(lastRefreshedTime + Constants.TIME_SHIFT + route.getStartStation().getTicks(), ModClientConfig.TIME_FORMAT.get());
        String timeEnd = TimeUtils.parseTime(lastRefreshedTime + Constants.TIME_SHIFT + route.getEndStation().getTicks(), ModClientConfig.TIME_FORMAT.get());
        String dash = " - ";
        MutableComponent line = Utils.text(String.format("%s%s%s | %s %s | %s",
            timeStart,
            dash,
            timeEnd,
            route.getTransferCount(),
            transferText,
            TimeUtils.parseDurationShort(route.getTotalDuration())
        ));

        if (!route.isValid()) {
            line = line.withStyle(ChatFormatting.RED).withStyle(ChatFormatting.STRIKETHROUGH);
        }

        float localScale = shadowlessFont.width(line) > WIDTH - 12 ? scale : 1;
        pPoseStack.pushPose();
        pPoseStack.scale(localScale, 1, 1);
        drawString(pPoseStack, minecraft.font, line, (int)((x + 6) / localScale), y + 5, 0xFFFFFF);
        pPoseStack.popPose();

        int routePartWidth = DISPLAY_WIDTH / parts.length;
        String end = route.getEndStation().getStationName();
        int textW = shadowlessFont.width(end);
        
        for (int i = 0; i < parts.length; i++) {
            fill(pPoseStack, x + 5 + (i * routePartWidth) + 1, y + 27, x + 5 + ((i + 1) * routePartWidth + 1) - 2, y + 38, 0xFF393939);
        }

        pPoseStack.pushPose();
        pPoseStack.scale(scale, scale, scale);
        
        if (route.getStartStation().shouldRenderRealtime()) {
            drawString(pPoseStack, shadowlessFont, TimeUtils.parseTime((int)(route.getStartStation().getEstimatedTimeWithThreshold() % 24000 + Constants.TIME_SHIFT), ModClientConfig.TIME_FORMAT.get()), (int)((x + 6 + shadowlessFont.width(timeStart) * localScale / 2.0f) / scale) - shadowlessFont.width(timeStart) / 2, (int)((y + 15) / scale), route.getStartStation().isDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME);
        }
        if (route.getEndStation().shouldRenderRealtime()) {
            drawString(pPoseStack, shadowlessFont, TimeUtils.parseTime((int)(route.getEndStation().getEstimatedTimeWithThreshold() % 24000 + Constants.TIME_SHIFT), ModClientConfig.TIME_FORMAT.get()), (int)((x + 6 + shadowlessFont.width(timeEnd) * localScale * 1.5f + (shadowlessFont.width(dash)) * localScale) / scale) - shadowlessFont.width(timeEnd) / 2, (int)((y + 15) / scale), route.getEndStation().isDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME);
        }

        if (!route.isValid()) {
            drawString(pPoseStack, shadowlessFont, trainCancelled, (int)((x + WIDTH - 5) / scale) - shadowlessFont.width(trainCancelled), (int)((y + 15) / scale), Constants.COLOR_DELAYED);        
        } else if (!beforeJourney) {
            drawString(pPoseStack, shadowlessFont, connectionInPast, (int)((x + WIDTH - 5) / scale) - shadowlessFont.width(connectionInPast), (int)((y + 15) / scale), Constants.COLOR_DELAYED);        
        }


        for (int i = 0; i < parts.length; i++) {
            drawCenteredString(pPoseStack, shadowlessFont, parts[i].getTrainName(), (int)((x + 5 + (i * routePartWidth) + (routePartWidth / 2)) / 0.75f), (int)((y + 30) / 0.75f), 0xFFFFFF);
        }

        drawString(pPoseStack, shadowlessFont, route.getStartStation().getStationName(), (int)((x + 6) / scale), (int)((y + 43) / scale), 0xDBDBDB);
        drawString(pPoseStack, shadowlessFont, end, (int)((x + WIDTH - 6) / scale) - textW, (int)((y + 43) / scale), 0xDBDBDB);
        
        pPoseStack.popPose();
    }
    
}
