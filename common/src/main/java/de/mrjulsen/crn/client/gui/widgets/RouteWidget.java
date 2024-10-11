package de.mrjulsen.crn.client.gui.widgets;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ModGuiUtils;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.screen.RouteDetailsScreen;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.SavedRoutesManager;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.RoutePart;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem.ContextMenuItemData;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class RouteWidget extends DLButton {    

    public static final int WIDTH = 214;
    public static final int HEIGHT = 54;

    private static final int DISPLAY_WIDTH = WIDTH - 10;
    
    private final ClientRoute route;

    private final MutableComponent transferText = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.transfer");
    private final MutableComponent connectionInPast = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.connection_in_past");
    private final MutableComponent trainCanceled = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.stop_cancelled");
    private final MutableComponent textShowDetails = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.show_details");
    private final MutableComponent textSave = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.save");
    private final MutableComponent textShare = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.share");
    private final MutableComponent textRemove = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_widget.remove");

    public RouteWidget(RouteViewer parent, ClientRoute route, int x, int y) {
        super(x, y, WIDTH, HEIGHT, TextUtils.empty(), (b) -> Minecraft.getInstance().setScreen(new RouteDetailsScreen(parent.getParent(), route)));
        this.route = route;

        setRenderStyle(AreaStyle.FLAT);
        setMenu(new DLContextMenu(() -> GuiAreaDefinition.of(this), () -> new DLContextMenuItem.Builder()
            .add(new ContextMenuItemData(textShowDetails, Sprite.empty(), true, (b) -> onPress.onPress(b), null))
            .addSeparator()
            .add(new ContextMenuItemData(SavedRoutesManager.isSaved(route) ? textRemove : textSave, Sprite.empty(), true, (b) -> {
                if (SavedRoutesManager.isSaved(route)) {
                    SavedRoutesManager.removeRoute(route);
                } else {
                    SavedRoutesManager.saveRoute(route);
                }
            }, null))
            //.add(new ContextMenuItemData(textShare, Sprite.empty(), true, (b) -> {}, null))
        ));
    }
    

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {        
        final int precision = ModClientConfig.REALTIME_PRECISION_THRESHOLD.get();
        
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, x, y, WIDTH, HEIGHT, ColorShade.DARK.getColor());
        CreateDynamicWidgets.renderHorizontalSeparator(graphics, x + 6, y + 22, WIDTH - 12);

        if (isMouseSelected()) {
            GuiUtils.fill(graphics, x(), y(), width(), height(), 0x22FFFFFF);
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        ImmutableList<RoutePart> parts = route.getParts();
        Font shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        String timeStart = TimeUtils.parseTime((int)((route.getStart().getScheduledDepartureTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        String timeEnd = TimeUtils.parseTime((int)((route.getEnd().getScheduledArrivalTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        String dash = " - ";
        MutableComponent summary = TextUtils.text(String.format("%s%s%s | %s %s | %s",
            timeStart,
            dash,
            timeEnd,
            route.getTransferCount(),
            transferText.getString(),
            TimeUtils.parseDurationShort((int)route.travelTime())
        ));

        final float scale = 0.75f;

        float localScale = shadowlessFont.width(summary) > WIDTH - 12 ? scale : 1;
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(localScale, 1, 1);
        GuiUtils.drawString(graphics, minecraft.font, (int)((x + 6) / localScale), y + 5, summary, 0xFFFFFF, EAlignment.LEFT, false);
        graphics.poseStack().popPose();

        int routePartWidth = DISPLAY_WIDTH / parts.size();
        String endStationName = route.getEnd().getClientTag().tagName();
        int textW = shadowlessFont.width(endStationName);
        
        for (int i = 0; i < parts.size(); i++) {
            int color = parts.get(i).getFirstStop().getTrainDisplayColor();
            GuiUtils.fill(graphics, x + 6 + (i * routePartWidth) + 1, y + 27, routePartWidth - 4, 1, color); 
            GuiUtils.fill(graphics, x + 5 + (i * routePartWidth) + 1, y + 28, routePartWidth - 2, 9, color);
            GuiUtils.fill(graphics, x + 6 + (i * routePartWidth) + 1, y + 37, routePartWidth - 4, 1, color);
        }

        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, scale);
        
        for (int i = 0; i < parts.size(); i++) {
            int color = parts.get(i).getFirstStop().getTrainDisplayColor();
            int fontColor = ModGuiUtils.useWhiteOrBlackForeColor(color) ? 0xFFFFFFFF: 0xFF000000;
            Component trainName = GuiUtils.ellipsisString(font, TextUtils.text(parts.get(i).getFirstStop().getTrainDisplayName()), (int)((routePartWidth - 10) / 0.75f));
            GuiUtils.drawString(graphics, font, (int)((x + 5 + (i * routePartWidth) + (routePartWidth / 2)) / 0.75f), (int)((y + 30) / 0.75f), trainName, fontColor, EAlignment.CENTER, false);
        }

        GuiUtils.drawString(graphics, font, (int)((x + 6) / scale), (int)((y + 43) / scale), TextUtils.text(route.getStart().getClientTag().tagName()), 0xDBDBDB, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, (int)((x + WIDTH - 6) / scale) - textW, (int)((y + 43) / scale), TextUtils.text(endStationName), 0xDBDBDB, EAlignment.LEFT, false);
        if (route.getStart().shouldRenderRealTime()) {
            GuiUtils.drawString(graphics, font, (int)((x + 6 + font.width(timeStart) * localScale / 2.0f) / scale) - font.width(timeStart) / 2, (int)((y + 15) / scale), TextUtils.text(TimeUtils.parseTime((int)((route.getStart().getScheduledDepartureTime() + (route.getStart().getDepartureTimeDeviation() / precision * precision)) % 24000 + DragonLib.DAYTIME_SHIFT), ModClientConfig.TIME_FORMAT.get())), route.getStart().isDepartureDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        }
        if (route.getEnd().shouldRenderRealTime()) {
            GuiUtils.drawString(graphics, font, (int)((x + 6 + font.width(timeEnd) * localScale * 1.5f + (font.width(dash)) * localScale) / scale) - font.width(timeEnd) / 2, (int)((y + 15) / scale), TextUtils.text(TimeUtils.parseTime((int)((route.getEnd().getScheduledArrivalTime() + (route.getEnd().getArrivalTimeDeviation() / precision * precision)) % 24000 + DragonLib.DAYTIME_SHIFT), ModClientConfig.TIME_FORMAT.get())), route.getEnd().isArrivalDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        }

        if (route.isAnyCancelled()) {
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + WIDTH - 5) / scale), (int)((y + 15) / scale), trainCanceled, Constants.COLOR_DELAYED, EAlignment.RIGHT, false);
        } else if (route.getStart().isDeparted()) {
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + WIDTH - 5) / scale), (int)((y + 15) / scale), connectionInPast, Constants.COLOR_DELAYED, EAlignment.RIGHT, false);
        }

        graphics.poseStack().popPose();
    }
    
}
