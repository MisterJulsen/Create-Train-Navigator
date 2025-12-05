package de.mrjulsen.crn.client.gui.windows;

import java.util.List;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.overlay.RouteDetailsOverlay;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.routedetails.RouteDetailsViewer;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.data.SavedRoutesManager;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.DLOverlayManager;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
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
import net.minecraft.network.chat.MutableComponent;

public class RouteDetailsWindow extends AbstractNavigatorScreen {
    
    private final MutableComponent textDeparture = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.departure");
    private final MutableComponent textArrival = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.arrival");
    private final MutableComponent timeNowText = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".time.now");
    private final MutableComponent tooltipSaveRoute = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.save_route.tooltip");
    private final MutableComponent tooltipRemoveRoute = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.remove_route.tooltip");
    private final MutableComponent tooltipShowPopup = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.show_popup.tooltip");
    private final MutableComponent tooltipShowNotifications = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overlay_settings.notifications");
    private final MutableComponent tooltipShowNotificationsDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overlay_settings.notifications.description").withStyle(ChatFormatting.GRAY);

    private final ClientRoute route;

    public RouteDetailsWindow(DLWindowManager manager, ClientRoute route) {
        super(manager, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.title"), ContainerColor.GOLD, BarColor.GOLD);
        this.route = route;

        int dy = FooterSize.DEFAULT.size() + 38;
        RouteDetailsViewer view = addComponent(new RouteDetailsViewer(3, FooterSize.DEFAULT.size() + 38, width() - 6, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1));
        view.anchor.set(EAlign.values());
        view.displayRoute(route);

        CreateButton saveRouteBtn = addComponent(new CreateButton(30, 223, SavedRoutesManager.isSaved(route) ? ModGuiIcons.BOOKMARK_FILLED.getAsCreateIcon() : ModGuiIcons.BOOKMARK.getAsCreateIcon()));
        saveRouteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            saveRouteBtn.tooltip.set(new DLTooltip(List.of(SavedRoutesManager.isSaved(route) ? tooltipRemoveRoute : tooltipSaveRoute), 200));
            if (SavedRoutesManager.isSaved(route)) {
                SavedRoutesManager.removeRoute(route);
                saveRouteBtn.icon = ModGuiIcons.BOOKMARK.getAsCreateIcon();
            } else {
                SavedRoutesManager.saveRoute(route);
                saveRouteBtn.icon = ModGuiIcons.BOOKMARK_FILLED.getAsCreateIcon();
            }
            SavedRoutesManager.push(true, null);
            boolean isSaved = SavedRoutesManager.isSaved(route);
            //notificationButton.set_visible(isSaved);
            //notificationIndicator.set_visible(isSaved);
            route.setShowNotifications(isSaved);
            return false;
        });
        saveRouteBtn.tooltip.set(new DLTooltip(List.of(SavedRoutesManager.isSaved(route) ? tooltipRemoveRoute : tooltipSaveRoute), 200));

        CreateButton popupBtn = addComponent(new CreateButton(50, 223, ModGuiIcons.PIN.getAsCreateIcon()));
        popupBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            DLOverlayManager.addOverlay(mgr -> new RouteDetailsOverlay(mgr, ModCommonEvents.getPhysicalLevel(), route, 0, 0));
            return false;
        });
        popupBtn.tooltip.set(new DLTooltip(List.of(tooltipShowPopup), 200));
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        super.renderMainLayer(graphics, mouseX, mouseY, renderBounds);
        
        int y = FooterSize.DEFAULT.size() - 1;
        CreateDynamicWidgets.renderContainer(graphics, 1, y, GUI_WIDTH - 2, 38, ContainerColor.BLUE);
        y += 38 - 1;
        CreateDynamicWidgets.renderContainer(graphics, 1, y, GUI_WIDTH - 2, GUI_HEIGHT - y - FooterSize.SMALL.size() + 1, ContainerColor.GOLD);
        
        if (!route.isAnyCancelled()) {
            if (route.getStart().isDeparted()) {
                GuiUtils.drawString(graphics, graphics.defaultFont(), GUI_WIDTH / 2, 19, textArrival, DLColor.WHITE, ETextAlignment.CENTER, false);
            } else {
                GuiUtils.drawString(graphics, graphics.defaultFont(), GUI_WIDTH / 2, 19, textDeparture, DLColor.WHITE, ETextAlignment.CENTER, false);
            }
            graphics.poseStack().pushPose();
            graphics.poseStack().scale(2, 2, 2);
            long time = 0;
            if (route.getStart().isDeparted()) {
                time = route.getEnd().getRealTimeArrivalTime() - DragonLib.getCurrentWorldTime();
                GuiUtils.drawString(graphics, graphics.defaultFont(), (GUI_WIDTH / 2) / 2, (31) / 2, time < 0 ? timeNowText : TextUtils.text(DLTime.fromTicks(time, VanillaTimeSystem.INSTANCE).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME)), DLColor.WHITE, ETextAlignment.CENTER, false);
            } else {
                time = route.getStart().getRealTimeDepartureTime() - DragonLib.getCurrentWorldTime();
                GuiUtils.drawString(graphics, graphics.defaultFont(), (GUI_WIDTH / 2) / 2, (31) / 2, time < 0 ? timeNowText : TextUtils.text(DLTime.fromTicks(time, VanillaTimeSystem.INSTANCE).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME)), DLColor.WHITE, ETextAlignment.CENTER, false);
            }
            graphics.poseStack().popPose();
        }
    }
    
}
