package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.Consumer;

import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.screen.NavigatorScreen;
import de.mrjulsen.crn.client.gui.screen.RouteDetailsScreen;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleRoute.SimpleRoutePart;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.event.listeners.JourneyListener.State;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.ColorUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;

public class RouteEntryOverviewWidget extends DLButton {

    public static final int WIDTH = 200;
    public static final int HEIGHT = 54;

    private static final int DISPLAY_WIDTH = 190;
    
    private final SimpleRoute route;
    private final NavigatorScreen parent;
    private final Level level;
    private final int lastRefreshedTime;

    private static final MutableComponent transferText = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".navigator.route_entry.transfer");
    private static final MutableComponent connectionInPast = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".navigator.route_entry.connection_in_past");
    private static final MutableComponent trainCanceled = TextUtils.translate("gui.createrailwaysnavigator.route_overview.stop_canceled");

    public RouteEntryOverviewWidget(NavigatorScreen parent, Level level, int lastRefreshedTime, int pX, int pY, SimpleRoute route, Consumer<RouteEntryOverviewWidget> onClick) {
        super(pX, pY, WIDTH, HEIGHT, TextUtils.text(route.getName()), onClick); // 48
        this.route = route;
        this.parent = parent;
        this.level = level;
        this.lastRefreshedTime = lastRefreshedTime;
    }


    @Override
    public void onClick(double pMouseX, double pMouseY) {
        super.onClick(pMouseX, pMouseY);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new RouteDetailsScreen(parent, level, route, route.getListenerId()));
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        
        final float scale = 0.75f;
        float l = isMouseOver(pMouseX, pMouseY) ? 0.1f : 0;
        boolean isActive = JourneyListenerManager.getInstance().get(route.getListenerId(), null) != null;
        boolean beforeJourney = isActive && JourneyListenerManager.getInstance().get(route.getListenerId(), null).getCurrentState() == State.BEFORE_JOURNEY;
        
        int color = ColorUtils.lightenColor(ColorShade.DARK.getColor(), l);
        if (!beforeJourney) {
            color = ColorUtils.applyTint(color, 0x663300);
        }
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, x, y, WIDTH, HEIGHT, color);
        CreateDynamicWidgets.renderHorizontalSeparator(graphics, x + 6, y + 22, 188);

        Minecraft minecraft = Minecraft.getInstance();
        SimpleRoutePart[] parts = route.getParts().toArray(SimpleRoutePart[]::new);
        Font shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        String timeStart = TimeUtils.parseTime(lastRefreshedTime + DragonLib.DAYTIME_SHIFT + route.getStartStation().getTicks(), ModClientConfig.TIME_FORMAT.get());
        String timeEnd = TimeUtils.parseTime(lastRefreshedTime + DragonLib.DAYTIME_SHIFT + route.getEndStation().getTicks(), ModClientConfig.TIME_FORMAT.get());
        String dash = " - ";
        MutableComponent line = TextUtils.text(String.format("%s%s%s | %s %s | %s",
            timeStart,
            dash,
            timeEnd,
            route.getTransferCount(),
            transferText.getString(),
            TimeUtils.parseDurationShort(route.getTotalDuration())
        ));

        if (!route.isValid()) {
            line = line.withStyle(ChatFormatting.RED).withStyle(ChatFormatting.STRIKETHROUGH);
        }

        float localScale = shadowlessFont.width(line) > WIDTH - 12 ? scale : 1;
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(localScale, 1, 1);
        GuiUtils.drawString(graphics, minecraft.font, (int)((x + 6) / localScale), y + 5, line, 0xFFFFFF, EAlignment.LEFT, false);
        graphics.poseStack().popPose();

        int routePartWidth = DISPLAY_WIDTH / parts.length;
        String end = route.getEndStation().getStationName();
        int textW = shadowlessFont.width(end);
        
        for (int i = 0; i < parts.length; i++) {
            GuiUtils.fill(graphics, x + 5 + (i * routePartWidth) + 1, y + 27, routePartWidth - 2, 11, 0xFF393939);
        }

        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, scale);
        
        if (route.getStartStation().shouldRenderRealtime()) {
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 6 + shadowlessFont.width(timeStart) * localScale / 2.0f) / scale) - shadowlessFont.width(timeStart) / 2, (int)((y + 15) / scale), TextUtils.text(TimeUtils.parseTime((int)(route.getStartStation().getEstimatedTimeWithThreshold() % 24000 + DragonLib.DAYTIME_SHIFT), ModClientConfig.TIME_FORMAT.get())), route.getStartStation().isDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        }
        if (route.getEndStation().shouldRenderRealtime()) {
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 6 + shadowlessFont.width(timeEnd) * localScale * 1.5f + (shadowlessFont.width(dash)) * localScale) / scale) - shadowlessFont.width(timeEnd) / 2, (int)((y + 15) / scale), TextUtils.text(TimeUtils.parseTime((int)(route.getEndStation().getEstimatedTimeWithThreshold() % 24000 + DragonLib.DAYTIME_SHIFT), ModClientConfig.TIME_FORMAT.get())), route.getEndStation().isDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        }

        if (!route.isValid()) {
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + WIDTH - 5) / scale) - shadowlessFont.width(trainCanceled), (int)((y + 15) / scale), trainCanceled, Constants.COLOR_DELAYED, EAlignment.LEFT, false);
        } else if (!beforeJourney) {
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + WIDTH - 5) / scale) - shadowlessFont.width(connectionInPast), (int)((y + 15) / scale), connectionInPast, Constants.COLOR_DELAYED, EAlignment.LEFT, false);
        }


        for (int i = 0; i < parts.length; i++) {
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 5 + (i * routePartWidth) + (routePartWidth / 2)) / 0.75f), (int)((y + 30) / 0.75f), TextUtils.text(parts[i].getTrainName()), 0xFFFFFF, EAlignment.CENTER, false);
        }

        GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 6) / scale), (int)((y + 43) / scale), TextUtils.text(route.getStartStation().getStationName()), 0xDBDBDB, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, shadowlessFont, (int)((x + WIDTH - 6) / scale) - textW, (int)((y + 43) / scale), TextUtils.text(end), 0xDBDBDB, EAlignment.LEFT, false);
        
        graphics.poseStack().popPose();
    }
    
}
