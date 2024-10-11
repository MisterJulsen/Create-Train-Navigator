package de.mrjulsen.crn.client.gui.screen;

import java.util.LinkedHashMap;
import java.util.Map;

import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;
import com.simibubi.create.foundation.utility.Components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.overlay.RouteDetailsOverlay;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIndicator;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.client.gui.widgets.RouteDetailsViewer;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.data.SavedRoutesManager;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.OverlayManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.WidgetsCollection;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class RouteDetailsScreen extends AbstractNavigatorScreen {

    private final MutableComponent textDeparture = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.departure");
    private final MutableComponent timeNowText = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".time.now");
    private final MutableComponent tooltipSaveRoute = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.save_route.tooltip");
    private final MutableComponent tooltipRemoveRoute = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.remove_route.tooltip");
    private final MutableComponent tooltipShowPopup = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.show_popup.tooltip");
    private final MutableComponent tooltipShowNotifications = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overlay_settings.notifications");
    private final MutableComponent tooltipShowNotificationsDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overlay_settings.notifications.description").withStyle(ChatFormatting.GRAY);

    private final ClientRoute route;

    private RouteDetailsViewer viewer;    
    private DLCreateIconButton notificationButton;
    private DLCreateIndicator notificationIndicator;
    private DLCreateIconButton saveRouteBtn;
    private DLTooltip saveBtnTooltip;
    private DLCreateIconButton popupBtn;

    private final Map<IconButton, Pair<Component, Component>> buttonTooltips = new LinkedHashMap<>();
    private final WidgetsCollection buttons = new WidgetsCollection();

    public RouteDetailsScreen(Screen lastScreen, ClientRoute route) {
        super(lastScreen, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.title"), BarColor.GOLD);
        this.route = route;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        buttonTooltips.clear();
        final int fWidth = width;
        final int fHeight = height;
        int dy = FooterSize.DEFAULT.size() + 38;
        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, guiLeft + GUI_WIDTH - 8, guiTop + dy, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1, null);
        viewer = new RouteDetailsViewer(this, guiLeft + 3, guiTop + dy, GUI_WIDTH - 6, GUI_HEIGHT - dy - FooterSize.SMALL.size() - 1, scrollBar);
        addRenderableWidget(viewer);
        addRenderableWidget(scrollBar);
        viewer.displayRoute(route);


        saveRouteBtn = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 30, guiTop + 223, DLIconButton.DEFAULT_BUTTON_WIDTH, DLIconButton.DEFAULT_BUTTON_HEIGHT, SavedRoutesManager.isSaved(route) ? ModGuiIcons.BOOKMARK_FILLED.getAsCreateIcon() : ModGuiIcons.BOOKMARK.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                removeTooltips(x -> x == saveBtnTooltip);
                addTooltip(DLTooltip.of(SavedRoutesManager.isSaved(route) ? tooltipRemoveRoute : tooltipSaveRoute).assignedTo(saveRouteBtn));
                if (SavedRoutesManager.isSaved(route)) {
                    SavedRoutesManager.removeRoute(route);
                    this.setIcon(ModGuiIcons.BOOKMARK.getAsCreateIcon());
                } else {                    
                    SavedRoutesManager.saveRoute(route);
                    this.setIcon(ModGuiIcons.BOOKMARK_FILLED.getAsCreateIcon());
                }
                SavedRoutesManager.push(true, null);
                boolean isSaved = SavedRoutesManager.isSaved(route);
                notificationButton.set_visible(isSaved);
                notificationIndicator.set_visible(isSaved);
                route.setShowNotifications(isSaved);
                
            }
        });
        addTooltip(DLTooltip.of(SavedRoutesManager.isSaved(route) ? tooltipRemoveRoute : tooltipSaveRoute).assignedTo(saveRouteBtn));
        
        popupBtn = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 52, guiTop + 223, DLIconButton.DEFAULT_BUTTON_WIDTH, DLIconButton.DEFAULT_BUTTON_HEIGHT, ModGuiIcons.POPUP.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                InstanceManager.setRouteOverlay(OverlayManager.add(new RouteDetailsOverlay(ModCommonEvents.getPhysicalLevel(), route, fWidth, fHeight)));
            }
        });
        addTooltip(DLTooltip.of(tooltipShowPopup).assignedTo(popupBtn));

        
        notificationIndicator = this.addRenderableWidget(new DLCreateIndicator(guiLeft + GUI_WIDTH - DLIconButton.DEFAULT_BUTTON_WIDTH - 8, guiTop + 220, Components.immutableEmpty()));
        notificationButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - DLIconButton.DEFAULT_BUTTON_WIDTH - 8, guiTop + 225, DLIconButton.DEFAULT_BUTTON_WIDTH, DLIconButton.DEFAULT_BUTTON_HEIGHT, ModGuiIcons.INFO.getAsCreateIcon()));
        notificationButton.withCallback(() -> {
            route.setShowNotifications(!route.shouldShowNotifications());
        });
        buttonTooltips.put(notificationButton, Pair.of(tooltipShowNotifications, tooltipShowNotificationsDescription));
        boolean isSaved = SavedRoutesManager.isSaved(route);
        notificationButton.set_visible(isSaved);
        notificationIndicator.set_visible(isSaved);
    }

    @Override
    public void tick() {
        super.tick();        
        notificationIndicator.state = route.shouldShowNotifications() ? State.ON : State.OFF;
        saveRouteBtn.set_visible(!route.getEnd().isDeparted() && !route.isClosed());
        popupBtn.set_visible(!route.getEnd().isDeparted() && !route.isClosed());
        
        buttons.performForEachOfType(IconButton.class, x -> {
            if (!buttonTooltips.containsKey(x)) {
                return;
            }

            x.setToolTip(buttonTooltips.get(x).getFirst());
            x.getToolTip().add(TooltipHelper.holdShift(Palette.YELLOW, hasShiftDown()));

            if (hasShiftDown()) {
                x.getToolTip().add(buttonTooltips.get(x).getSecond());
            }
        });
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderNavigatorBackground(graphics, mouseX, mouseY, partialTicks);        
        
        int y = FooterSize.DEFAULT.size() - 1;
        CreateDynamicWidgets.renderContainer(graphics, guiLeft + 1, guiTop + y, GUI_WIDTH - 2, 38, ContainerColor.BLUE);
        y += 38 - 1;
        CreateDynamicWidgets.renderContainer(graphics, guiLeft + 1, guiTop + y, GUI_WIDTH - 2, GUI_HEIGHT - y - FooterSize.SMALL.size() + 1, ContainerColor.GOLD);

        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);

        if (!route.isAnyCancelled()) {
            if (route.getStart().isDeparted()) {
                GuiUtils.drawString(graphics, font, guiLeft + GUI_WIDTH / 2, guiTop + 19, "Ankunft in", 0xFFFFFF, EAlignment.CENTER, false);
            } else {
                GuiUtils.drawString(graphics, font, guiLeft + GUI_WIDTH / 2, guiTop + 19, textDeparture, 0xFFFFFF, EAlignment.CENTER, false);
            }
            graphics.poseStack().pushPose();
            graphics.poseStack().scale(2, 2, 2);
            long time = 0;
            if (route.getStart().isDeparted()) {
                time = route.getEnd().getRealTimeArrivalTime() - DragonLib.getCurrentWorldTime();
                GuiUtils.drawString(graphics, font, (guiLeft + GUI_WIDTH / 2) / 2, (guiTop + 31) / 2, time < 0 ? timeNowText : TextUtils.text(TimeUtils.parseDurationShort(time)), 0xFFFFFF, EAlignment.CENTER, false);
            } else {
                time = route.getStart().getRealTimeDepartureTime() - DragonLib.getCurrentWorldTime();
                GuiUtils.drawString(graphics, font, (guiLeft + GUI_WIDTH / 2) / 2, (guiTop + 31) / 2, time < 0 ? timeNowText : TextUtils.text(TimeUtils.parseDurationShort(time)), 0xFFFFFF, EAlignment.CENTER, false);
            }
            graphics.poseStack().popPose();
        }

        //GuiUtils.drawString(graphics, font, 5, 5, "State: " + route.getState(), 0xFFFF0000, EAlignment.LEFT, false);
    }
}
