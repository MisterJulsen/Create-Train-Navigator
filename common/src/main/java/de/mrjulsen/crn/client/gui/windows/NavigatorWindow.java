package de.mrjulsen.crn.client.gui.windows;

import de.mrjulsen.crn.client.gui.ModGuiIcons;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.flyout.FlyoutDepartureInWidget;
import de.mrjulsen.crn.client.gui.flyout.FlyoutTrainCategoriesWidget;
import de.mrjulsen.crn.client.gui.flyout.FlyoutTransferTimeWidget;
import de.mrjulsen.crn.client.gui.widgets.RouteViewer;
import de.mrjulsen.crn.client.gui.widgets.SearchOptionButton;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.widgets.autocomplete.StationTagsAutocomplete;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.network.packets.pain.GetNearestStationPacketData;
import de.mrjulsen.crn.network.packets.pain.GetUserSettingsPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;

public class NavigatorWindow extends AbstractNavigatorScreen {

    
    private UserSettings userSettings = new UserSettings(Minecraft.getInstance().player.getUUID(), false);

    private RouteViewer routeViewer;

    public NavigatorWindow(DLWindowManager manager) {
        super(manager, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.title"), ContainerColor.GRAY, BarColor.GRAY);
        manager.setPauseScreen(false);

        CreateTextBox fromBox = addComponent(new CreateTextBox(40, 20, 150));
        fromBox.autocompleteManager.set(new StationTagsAutocomplete());
        
        CreateTextBox toBox = addComponent(new CreateTextBox(40, fromBox.y() + fromBox.height() + 4, 150));
        toBox.autocompleteManager.set(new StationTagsAutocomplete());

        routeViewer = addComponent(new RouteViewer(3, 88, width() - 6, 128));
        routeViewer.displayRecentSearchQueries.set(true);

        CreateButton positionBtn = addComponent(new CreateButton(fromBox.x() + fromBox.width() + 4, fromBox.y(), ModGuiIcons.POSITION.getAsCreateIcon()));
        positionBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            ModNetworkManager.GET_NEAREST_STATION.send(NetworkDirection.toServer(), new GetNearestStationPacketData.Request(Minecraft.getInstance().player.blockPosition()), (response) -> {
                fromBox.text.get().set(response.getResult().tagName.get().get());
            }, () -> {});
            return false;
        });

        CreateButton searchBtn = addComponent(new CreateButton(toBox.x() + toBox.width() + 4, toBox.y(), AllIcons.I_MTD_SCAN));
        searchBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            searchBtn.enabled.set(false);
            routeViewer.search(fromBox.text.get().getPlainText(), toBox.text.get().getPlainText(), () -> searchBtn.enabled.set(true));
            return false;
        });

        
        CreateButton globalSettingsButton = addComponent(new CreateButton(30, 223, ModGuiIcons.SETTINGS.getAsCreateIcon()));
        globalSettingsButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new GlobalSettingsWindow(mgr));
            return false;
        });
        CreateButton departureBoardBtn = addComponent(new CreateButton(width() - CreateButton.WIDTH - 8, 223, ModGuiIcons.VERY_DETAILED.getAsCreateIcon()));
        departureBoardBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new ScheduleBoardWindow(mgr, null));
            return false;
        });
        CreateButton savedRoutesBtn = addComponent(new CreateButton(width() - CreateButton.WIDTH - 37, 223, ModGuiIcons.MAP_PATH.getAsCreateIcon()));
        savedRoutesBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new SavedRoutesWindow(mgr));
            return false;
        });



        // Filter Options
        final int btnCount = 3;
        int btnWidth = (GUI_WIDTH - 6 - 16) / btnCount;
        addComponent(new SearchOptionButton(3, 54 + FooterSize.DEFAULT.size() - 2, btnWidth, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.departure_in"), () -> userSettings.navigationDepartureInTicks.toString(), (b) -> {
            getWindowManager().createModal((mgr) -> new FlyoutDepartureInWidget(mgr, b, FlyoutPointer.UP, ColorShade.DARK, userSettings, () -> userSettings.navigationDepartureInTicks));
        }));
        addComponent(new SearchOptionButton(3 + btnWidth, 54 + FooterSize.DEFAULT.size() - 2, btnWidth, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.transfer_time"), () -> userSettings.navigationTransferTime.toString(), (b) -> {
            getWindowManager().createModal((mgr) -> new FlyoutTransferTimeWidget(mgr, b, FlyoutPointer.UP, ColorShade.DARK, userSettings, () -> userSettings.navigationTransferTime));
        }));
        addComponent(new SearchOptionButton(3 + btnWidth * 2, 54 + FooterSize.DEFAULT.size() - 2, btnWidth, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_categories"), () -> userSettings.navigationExcludedTrainCategories.toString(), (b) -> {
            getWindowManager().createModal((mgr) -> new FlyoutTrainCategoriesWidget(mgr, b, FlyoutPointer.UP, ColorShade.DARK, userSettings, () -> userSettings.navigationExcludedTrainCategories));
        }));

        reloadUserSettings();


    }    

    private void reloadUserSettings() {
        ModNetworkManager.GET_USER_SETTINGS.send(NetworkDirection.toServer(), new GetUserSettingsPacketData.Request(Minecraft.getInstance().player.getUUID()), (response) -> {
            response.getData().ifPresent(a -> {
                this.userSettings = a;
                this.routeViewer.updateSettings(a);
            });
        }, () -> {});
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        super.renderMainLayer(graphics, mouseX, mouseY, renderBounds);

        int y = FooterSize.DEFAULT.size() - 1;
        CreateDynamicWidgets.renderContainer(graphics, 1, y, GUI_WIDTH - 2, 52, ContainerColor.BLUE);
        CreateDynamicWidgets.renderContainer(graphics, 1, y + 52 - 1, GUI_WIDTH - 2, 22, ContainerColor.GOLD);
        y += 52 + 22 - 2;
        CreateDynamicWidgets.renderContainer(graphics, 1, y, GUI_WIDTH - 2, GUI_HEIGHT - y - FooterSize.SMALL.size() + 1, ContainerColor.GRAY);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, GUI_WIDTH - 18 - 14, 218, 27, BarColor.GRAY);
        
        CRNGui.GUI_SPRITES.getSprite("route_start").render(graphics, 16, 16, 7, 24);
        CRNGui.GUI_SPRITES.getSprite("route_end").render(graphics, 16, 16 + 24, 7, 24);


    }    
}
