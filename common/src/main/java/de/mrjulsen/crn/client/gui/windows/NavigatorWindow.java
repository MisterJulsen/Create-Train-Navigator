package de.mrjulsen.crn.client.gui.windows;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.ModGuiIcons;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.flyout.*;
import de.mrjulsen.crn.client.gui.flyout.content.FlyoutContent;
import de.mrjulsen.crn.client.gui.flyout.content.SettingEntry;
import de.mrjulsen.crn.client.gui.flyout.content.SettingsMenuContent;
import de.mrjulsen.crn.client.gui.flyout.content.TimeSettingContent;
import de.mrjulsen.crn.client.gui.flyout.content.TrainCategoriesContent;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.RouteViewer;
import de.mrjulsen.crn.client.gui.widgets.SearchOptionButton;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.widgets.autocomplete.StationTagsAutocomplete;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.crn.data.settings.UserSettings;
import de.mrjulsen.crn.network.packets.GetNearestStationPacketData;
import de.mrjulsen.crn.network.packets.GetUserSettingsPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.TableLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.RenderLayer;
import de.mrjulsen.mcdragonlib.client.render.DLTextureSheet;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class NavigatorWindow extends AbstractNavigatorScreen {

    private static final DLTextureSheet GUI_SHEET = new DLTextureSheet(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/gui.png"));

    private UserSettings userSettings = new UserSettings(Minecraft.getInstance().player.getUUID(), false);

    private RouteViewer routeViewer;

    private final Component txtGlobalSettings = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.global_settings.tooltip");
    private final Component txtNearestStation = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.location.tooltip");
    private final Component txtSwapInputs = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.swap.tooltip");
    private final Component txtDepartureBoard = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.title");
    private final Component txtSavedRoutes = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.title");

    public NavigatorWindow(DLWindowManager manager) {
        super(manager, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.title"), ContainerColor.GRAY, BarColor.GRAY);
        manager.setPauseScreen(false);

        CreateTextBox fromBox = addComponent(new CreateTextBox(40, 20, 150));
        fromBox.autocompleteManager.set(new StationTagsAutocomplete());

        CreateTextBox toBox = addComponent(new CreateTextBox(40, fromBox.y() + fromBox.height() + 4, 150));
        toBox.autocompleteManager.set(new StationTagsAutocomplete());

        FlatIconButton swapInputs = addComponent(new FlatIconButton(fromBox.x() + fromBox.width() - 20, fromBox.y() + ((toBox.y() + toBox.height()) - fromBox.y()) / 2 - 8, ModGuiIcons.EMPTY.getAsSprite(16, 16)) {
            @Override
            public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
                super.renderMainLayer(graphics, mouseX, mouseY, renderBounds);
                CRNGui.GUI_SPRITES.getSprite("swap_arrows").render(graphics, 4, 2, 9, 12);
            }
        });
        swapInputs.setSize(16, 16);
        swapInputs.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            String fromTxt = fromBox.text.get().getPlainText();
            String toTxt = toBox.text.get().getPlainText();
            fromBox.text.get().set(toTxt);
            toBox.text.get().set(fromTxt);
            return false;
        });
        swapInputs.tooltip.set(new DLTooltip(List.of(txtSwapInputs), 256));

        routeViewer = addComponent(new RouteViewer(3, 88, width() - 6, 128));
        routeViewer.displayRecentSearchQueries.set(true);
        routeViewer.addEventListener(RouteViewer.NavigateEvent.class, (s, e) -> {
            fromBox.text.get().set(e.from());
            toBox.text.get().set(e.to());
            return false;
        });

        CreateButton positionBtn = addComponent(new CreateButton(fromBox.x() + fromBox.width() + 4, fromBox.y(), ModGuiIcons.POSITION.getAsCreateIcon()));
        positionBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            ModNetworkManager.GET_NEAREST_STATION.send(NetworkDirection.toServer(), new GetNearestStationPacketData.Request(Minecraft.getInstance().player.blockPosition()), (response) -> {
                fromBox.text.get().set(response.getResult().tagName.get().get());
            }, () -> {});
            return false;
        });
        positionBtn.tooltip.set(new DLTooltip(List.of(txtNearestStation), 256));

        CreateButton searchBtn = addComponent(new CreateButton(toBox.x() + toBox.width() + 4, toBox.y(), AllIcons.I_MTD_SCAN));
        searchBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            searchBtn.enabled.set(false);
            routeViewer.search(fromBox.text.get().getPlainText(), toBox.text.get().getPlainText(), () -> searchBtn.enabled.set(true));
            return false;
        });
        searchBtn.tooltip.set(new DLTooltip(List.of(Constants.TEXT_SEARCH), 256));


        CreateButton globalSettingsButton = addComponent(new CreateButton(30, 223, ModGuiIcons.SETTINGS.getAsCreateIcon()));
        globalSettingsButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new GlobalSettingsWindow(mgr));
            return false;
        });
        globalSettingsButton.tooltip.set(new DLTooltip(List.of(txtGlobalSettings), 256));

        CreateButton departureBoardBtn = addComponent(new CreateButton(width() - CreateButton.WIDTH - 8, 223, ModGuiIcons.VERY_DETAILED.getAsCreateIcon()));
        departureBoardBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new ScheduleBoardWindow(mgr, null));
            return false;
        });
        departureBoardBtn.tooltip.set(new DLTooltip(List.of(txtDepartureBoard), 256));

        CreateButton savedRoutesBtn = addComponent(new CreateButton(width() - CreateButton.WIDTH - 37, 223, ModGuiIcons.MAP_PATH.getAsCreateIcon()));
        savedRoutesBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new SavedRoutesWindow(mgr));
            return false;
        });
        savedRoutesBtn.tooltip.set(new DLTooltip(List.of(txtSavedRoutes), 256));





        DLPanel optionsPanel = new DLPanel(3, 52 + FooterSize.DEFAULT.size(), width() - 6, 18);
        TableLayout layout = new TableLayout();
        layout.columnGap.set(1);
        layout.addColumn("column1", 0.3333333f, TableLayout.ColumnSizeMode.PERCENTAGE);
        layout.addColumn("column2", 0.3333333f, TableLayout.ColumnSizeMode.PERCENTAGE);
        layout.addColumn("column3", 0.3333333f, TableLayout.ColumnSizeMode.PERCENTAGE);
        layout.addColumn("more_options", 18, TableLayout.ColumnSizeMode.FIXED);
        optionsPanel.layout.set(layout);
        addComponent(optionsPanel);
        optionsPanel.addEventListener(DLGuiStandardEvents.RenderEvent.class, (s, e) -> {
            if (e.layer() == RenderLayer.MAIN) {
                for (int i = 0; i < optionsPanel.componentsCount() - 1; i++) {
                    try {
                        DLGuiComponent c = optionsPanel.getComponents().get(i);
                        if (c != null) {
                            GuiUtils.fill(e.graphics(), c.x() + c.width(), 2, 1, optionsPanel.height() - 4, DragonLib.VANILLA_BUTTON_DISABLED_FONT_COLOR);
                        }
                    } catch (Exception ignored) {}
                }
            }
            return false;
        });

        // Highlighted Options
        SearchOptionButton departureInBtn = new SearchOptionButton(0, 0, 100, getOptionTitle("departure_in"), () -> userSettings.navigationDepartureInTicks.toString(), (b) -> {
            createOptionSetting(b, new TimeSettingContent(getOptionTitle("departure_in").withStyle(ChatFormatting.BOLD), () -> userSettings.navigationDepartureInTicks));
        });
        departureInBtn.layoutContraint.set("column1");
        optionsPanel.addComponent(departureInBtn);

        SearchOptionButton transferTimeBtn = new SearchOptionButton(0, 0, 100, getOptionTitle("transfer_time"), () -> userSettings.navigationTransferTime.toString(), (b) -> {
            createOptionSetting(b, new TimeSettingContent(getOptionTitle("transfer_time").withStyle(ChatFormatting.BOLD), () -> userSettings.navigationTransferTime));
        });
        transferTimeBtn.layoutContraint.set("column2");
        optionsPanel.addComponent(transferTimeBtn);

        SearchOptionButton trainCategoriesBtn = new SearchOptionButton(0, 0, 100, getOptionTitle("train_categories"), () -> userSettings.navigationExcludedTrainCategories.toString(), (b) -> {
            createOptionSetting(b, new TrainCategoriesContent(getOptionTitle("train_categories").withStyle(ChatFormatting.BOLD), () -> userSettings.navigationExcludedTrainCategories));
        });
        trainCategoriesBtn.layoutContraint.set("column3");
        optionsPanel.addComponent(trainCategoriesBtn);

        // More Options
        FlatIconButton moreOptionsBtn = new FlatIconButton(0, 0, GuiIcons.ARROW_DOWN.getAsSprite(16, 16));
        moreOptionsBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            List<SettingEntry> moreOptions = List.of(
                new SettingEntry(
                        getOptionTitle("departure_in"),
                        () -> userSettings.navigationDepartureInTicks.toString(),
                        () -> new TimeSettingContent(getOptionTitle("departure_in").withStyle(ChatFormatting.BOLD),
                        () -> userSettings.navigationDepartureInTicks
                )),
                new SettingEntry(
                        getOptionTitle("transfer_time"),
                        () -> userSettings.navigationTransferTime.toString(),
                        () -> new TimeSettingContent(getOptionTitle("transfer_time").withStyle(ChatFormatting.BOLD),
                        () -> userSettings.navigationTransferTime
                )),
                new SettingEntry(
                        getOptionTitle("train_categories"),
                        () -> userSettings.navigationExcludedTrainCategories.toString(),
                        () -> new TrainCategoriesContent(getOptionTitle("train_categories").withStyle(ChatFormatting.BOLD),
                        () -> userSettings.navigationExcludedTrainCategories
                ))
            );
            getWindowManager().createModal((mgr) -> new SettingFlyout(mgr, s, FlyoutPointer.UP, ColorShade.DARK, userSettings).open(new SettingsMenuContent(getOptionTitle("more_options").withStyle(ChatFormatting.BOLD), moreOptions)));
            return false;
        });
        moreOptionsBtn.layoutContraint.set("more_options");
        moreOptionsBtn.tooltip.set(new DLTooltip(List.of(), 200));
        optionsPanel.addComponent(moreOptionsBtn);

        reloadUserSettings();
    }

    private MutableComponent getOptionTitle(String key) {
        return TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options." + key);
    }

    private void createOptionSetting(DLGuiComponent anchor, FlyoutContent content) {
        getWindowManager().createModal((mgr) -> new SettingFlyout(mgr, anchor, FlyoutPointer.UP, ColorShade.DARK, userSettings).open(content));
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
