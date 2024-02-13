package de.mrjulsen.crn.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.DynamicWidgets;
import de.mrjulsen.crn.client.gui.DynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.screen.NavigatorScreen;
import de.mrjulsen.crn.client.gui.screen.RouteDetailsScreen;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleRoute.SimpleRoutePart;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.event.listeners.JourneyListener.State;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils.TimeFormat;
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
        
        float l = isMouseOver(pMouseX, pMouseY) ? 0.1f : 0;
        boolean beforeJourney = JourneyListenerManager.get(route.getListenerId(), null) != null && JourneyListenerManager.get(route.getListenerId(), null).getCurrentState() == State.BEFORE_JOURNEY;
        //RenderSystem.setShaderColor(1 + l, (beforeJourney ? 1 : 0.5f) + l, (beforeJourney ? 1 : 0.5f) + l, 1);
        //RenderSystem.setShaderTexture(0, Constants.GUI_WIDGETS);
        //blit(pPoseStack, x, y, 0, 0, WIDTH, HEIGHT);
        int color = ModGuiUtils.lightenColor(ColorShade.DARK.getColor(), l);
        if (!beforeJourney) {
            color = ModGuiUtils.applyTint(color, 0x663300);
        }
        DynamicWidgets.renderSingleShadeWidget(pPoseStack, x, y, WIDTH, HEIGHT, color);
        DynamicWidgets.renderHorizontalSeparator(pPoseStack, x + 6, y + 22, 188);

        Minecraft minecraft = Minecraft.getInstance();
        SimpleRoutePart[] parts = route.getParts().toArray(SimpleRoutePart[]::new);
        Font shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        String timeStart = TimeUtils.parseTime(lastRefreshedTime + Constants.TIME_SHIFT + route.getStartStation().getTicks(), TimeFormat.HOURS_24);
        String timeEnd = TimeUtils.parseTime(lastRefreshedTime + Constants.TIME_SHIFT + route.getEndStation().getTicks(), TimeFormat.HOURS_24);
        String dash = " - ";

        drawString(pPoseStack, minecraft.font, String.format("%s%s%s | %s %s | %s",
            timeStart,
            dash,
            timeEnd,
            route.getTransferCount(),
            transferText,
            TimeUtils.parseDurationShort(route.getTotalDuration())
        ), x + 6, y + 5, 0xFFFFFF);

        int routePartWidth = DISPLAY_WIDTH / parts.length;
        String end = route.getEndStation().getStationName();
        int textW = shadowlessFont.width(end);
        
        for (int i = 0; i < parts.length; i++) {
            fill(pPoseStack, x + 5 + (i * routePartWidth) + 1, y + 27, x + 5 + ((i + 1) * routePartWidth + 1) - 2, y + 38, 0xFF393939);
        }

        pPoseStack.pushPose();
        final float scale = 0.75f;
        pPoseStack.scale(scale, scale, scale);
        
        if (route.getStartStation().shouldRenderRealtime()) {
            drawString(pPoseStack, shadowlessFont, TimeUtils.parseTime((int)(route.getStartStation().getEstimatedTimeWithThreshold() % 24000 + Constants.TIME_SHIFT), TimeFormat.HOURS_24), (int)((x + 6 + shadowlessFont.width(timeStart) / 2.0f) / scale) - shadowlessFont.width(timeStart) / 2, (int)((y + 15) / scale), route.getStartStation().isDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME);
        }
        if (route.getEndStation().shouldRenderRealtime()) {
            drawString(pPoseStack, shadowlessFont, TimeUtils.parseTime((int)(route.getEndStation().getEstimatedTimeWithThreshold() % 24000 + Constants.TIME_SHIFT), TimeFormat.HOURS_24), (int)((x + 6 + shadowlessFont.width(timeEnd) * 1.5f + shadowlessFont.width(dash)) / scale) - shadowlessFont.width(timeEnd) / 2, (int)((y + 15) / scale), route.getEndStation().isDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME);
        }
        if (!beforeJourney) {
            drawString(pPoseStack, shadowlessFont, connectionInPast, (int)((x + WIDTH - 5) / scale) - shadowlessFont.width(connectionInPast), (int)((y + 15) / scale), Constants.COLOR_DELAYED);        
        }


        for (int i = 0; i < parts.length; i++) {
            drawCenteredString(pPoseStack, shadowlessFont, parts[i].getTrainName(), (int)((x + 5 + (i * routePartWidth) + (routePartWidth / 2)) / 0.75f), (int)((y + 30) / 0.75f), 0xFFFFFF);
        }

        drawString(pPoseStack, shadowlessFont, route.getStartStation().getStationName(), (int)((x + 6) / scale), (int)((y + 43) / scale), 0xDBDBDB);
        drawString(pPoseStack, shadowlessFont, end, (int)((x + WIDTH - 5) / scale) - textW, (int)((y + 43) / scale), 0xDBDBDB);
        
        pPoseStack.popPose();
    }
    
}
