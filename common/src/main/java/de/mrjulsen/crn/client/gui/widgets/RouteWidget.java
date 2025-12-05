package de.mrjulsen.crn.client.gui.widgets;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.windows.RouteDetailsWindow;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.RoutePart;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class RouteWidget extends DLButton {
    
    public static final int WIDTH = 214;
    public static final int HEIGHT = 54;

    private static final int DISPLAY_WIDTH = WIDTH - 10;
    
    
    private final ClientRoute route;

    private final MutableComponent transferText = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.transfer");
    private final MutableComponent connectionInPast = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.connection_in_past");
    private final MutableComponent trainCanceled = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.stop_cancelled");
    private final MutableComponent textShowDetails = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.show_details");
    private final MutableComponent textSave = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.save");
    private final MutableComponent textShare = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.share");
    private final MutableComponent textRemove = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.remove");

    public RouteWidget(int x, int y, ClientRoute route) {
        super(x, y, WIDTH, HEIGHT);
        this.cursor.set(CursorType.HAND);
        this.route = route;

        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new RouteDetailsWindow(mgr, route));
            return false;
        });
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        final int precision = ModClientConfig.REALTIME_PRECISION_THRESHOLD.get();
        
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, WIDTH, HEIGHT, ColorShade.DARK.getColor());
        CreateDynamicWidgets.renderHorizontalSeparator(graphics, 6, 22, WIDTH - 12);

        if (isSelected()) {
            GuiUtils.fill(graphics, 0, 0, width(), height(), DLColor.fromInt(0x22FFFFFF));
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        ImmutableList<RoutePart> parts = route.getParts();
        Font shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        String timeStart = DLTime.fromTicks(route.getStart().getScheduledDepartureTime(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME);
        String timeEnd = DLTime.fromTicks(route.getEnd().getScheduledArrivalTime(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME);
        String dash = " - ";
        MutableComponent summary = TextUtils.text(String.format("%s%s%s | %s %s | %s",
            timeStart,
            dash,
            timeEnd,
            route.getTransferCount(),
            transferText.getString(),
            DLTime.fromTicks((int)route.travelTime(), new ConfiguredTimeSystem()).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME)
        ));

        final float scale = 0.75f;

        float localScale = shadowlessFont.width(summary) > WIDTH - 12 ? scale : 1;
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(localScale, 1, 1);
        GuiUtils.drawString(graphics, minecraft.font, (int)(6 / localScale), 5, summary, DLColor.WHITE, ETextAlignment.LEFT, false);
        graphics.poseStack().popPose();

        int routePartWidth = DISPLAY_WIDTH / parts.size();
        String endStationName = route.getEnd().getRealTimeStationTag().tagName();
        int textW = shadowlessFont.width(endStationName);
        
        for (int i = 0; i < parts.size(); i++) {
            DLColor color = parts.get(i).getFirstStop().getTrainDisplayColor();
            GuiUtils.fill(graphics, 6 + (i * routePartWidth) + 1, 27, routePartWidth - 4, 1, color); 
            GuiUtils.fill(graphics, 5 + (i * routePartWidth) + 1, 28, routePartWidth - 2, 9, color);
            GuiUtils.fill(graphics, 6 + (i * routePartWidth) + 1, 37, routePartWidth - 4, 1, color);
        }

        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, scale);
        
        for (int i = 0; i < parts.size(); i++) {
            DLColor color = parts.get(i).getFirstStop().getTrainDisplayColor();
            DLColor fontColor = DLColor.pickBasedOnBrightness(color, DLColor.WHITE, DLColor.BLACK, 0.5f);
            Component trainName = TextUtils.truncateWithEllipsis(graphics.defaultFont(), TextUtils.text(parts.get(i).getFirstStop().getTrainDisplayName()), (int)((routePartWidth - 10) / 0.75f));
            GuiUtils.drawString(graphics, graphics.defaultFont(), (int)((5 + (i * routePartWidth) + (routePartWidth / 2)) / 0.75f), (int)(30 / 0.75f), trainName, fontColor, ETextAlignment.CENTER, false);
        }

        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)(6 / scale), (int)(43 / scale), TextUtils.text(route.getStart().getRealTimeStationTag().tagName()), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)((WIDTH - 6) / scale) - textW, (int)(43 / scale), TextUtils.text(endStationName), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
        if (route.getStart().shouldRenderRealTime()) {
            GuiUtils.drawString(graphics, graphics.defaultFont(), (int)((6 + graphics.defaultFont().width(timeStart) * localScale / 2.0f) / scale) - graphics.defaultFont().width(timeStart) / 2, (int)(15 / scale), TextUtils.text(DLTime.fromTicks(route.getStart().getScheduledDepartureTime() + (route.getStart().getDepartureTimeDeviation() / precision * precision), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)), route.getStart().isDepartureDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, ETextAlignment.LEFT, false);
        }
        if (route.getEnd().shouldRenderRealTime()) {
            GuiUtils.drawString(graphics, graphics.defaultFont(), (int)((6 + graphics.defaultFont().width(timeEnd) * localScale * 1.5f + (graphics.defaultFont().width(dash)) * localScale) / scale) - graphics.defaultFont().width(timeEnd) / 2, (int)(15 / scale), TextUtils.text(DLTime.fromTicks(route.getEnd().getScheduledArrivalTime() + (route.getEnd().getArrivalTimeDeviation() / precision * precision), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)), route.getEnd().isArrivalDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, ETextAlignment.LEFT, false);
        }

        if (route.isAnyCancelled()) {
            GuiUtils.drawString(graphics, shadowlessFont, (int)((WIDTH - 5) / scale), (int)(15 / scale), trainCanceled, Constants.COLOR_DELAYED, ETextAlignment.RIGHT, false);
        } else if (route.getStart().isDeparted()) {
            GuiUtils.drawString(graphics, shadowlessFont, (int)((WIDTH - 5) / scale), (int)(15 / scale), connectionInPast, Constants.COLOR_DELAYED, ETextAlignment.RIGHT, false);
        }

        graphics.poseStack().popPose();
    }
    
}
