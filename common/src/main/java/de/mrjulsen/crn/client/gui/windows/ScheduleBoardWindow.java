package de.mrjulsen.crn.client.gui.windows;

import java.util.List;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.flyout.FlyoutDepartureInWidget;
import de.mrjulsen.crn.client.gui.flyout.FlyoutTrainCategoriesWidget;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.widgets.autocomplete.StationTagsAutocomplete;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.SearchOptionButton;
import de.mrjulsen.crn.client.gui.widgets.StationDeparturesViewer;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.StationTag.ClientStationTag;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.network.packets.pain.GetNearestStationPacketData;
import de.mrjulsen.crn.network.packets.pain.GetUserSettingsPacketData;
import de.mrjulsen.crn.network.packets.pain.StationTagRequestByTagPacketData;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.TableLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.TableLayout.ColumnSizeMode;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

public class ScheduleBoardWindow extends AbstractNavigatorScreen {

    private final StationDeparturesViewer viewer;

    private UserSettings userSettings = new UserSettings(Minecraft.getInstance().player.getUUID(), false);
    private CreateTextBox stationBox;

    private String stationTagName;
    private final boolean fixedStation;

    private final MutableComponent tooltipSearch = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.search.tooltip");
    private final MutableComponent tooltipLocation = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.location.tooltip");
    private final MutableComponent tooltipRefresh = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.refresh.tooltip");

    public ScheduleBoardWindow(DLWindowManager manager, ClientStationTag tag) {
        super(manager, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.title"), ContainerColor.GOLD, BarColor.GOLD);
        this.fixedStation = tag != null;
        if (fixedStation) {
            this.stationTagName = tag.tagName();
        }

        int wY = FooterSize.DEFAULT.size() - 1;
        int wH = GUI_HEIGHT - wY - FooterSize.SMALL.size();
        
        this.viewer = addComponent(new StationDeparturesViewer(3, wY + 52, width() - 6, wH - 53));

        if (!fixedStation) {
            stationBox = addComponent(new CreateTextBox(32, 20, 154));
            stationBox.maxCharacters.set(StationTag.MAX_NAME_LENGTH);
            stationBox.autocompleteManager.set(new StationTagsAutocomplete());

            CreateButton searchButton = addComponent(new CreateButton(190, 20, AllIcons.I_MTD_SCAN));
            searchButton.tooltip.set(new DLTooltip(List.of(tooltipSearch), 200));
            searchButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                String stationFrom = stationBox.text.get().getPlainText();
                if (stationFrom == null || stationFrom.isBlank()) {
                    viewer.displayDepartures(stationFrom, userSettings);
                    return false;
                }

                ModNetworkManager.GET_STATION_TAG_BY_TAG.send(NetworkDirection.toServer(), new StationTagRequestByTagPacketData.Request(TagName.of(stationFrom)), (response) -> {
                    stationTagName = response.getTag().getTagName().get();
                    viewer.displayDepartures(stationTagName, userSettings);
                }, () -> {});
                return false;
            });

            CreateButton locationBtn = addComponent(new CreateButton(212, 20, ModGuiIcons.POSITION.getAsCreateIcon()));
            locationBtn.tooltip.set(new DLTooltip(List.of(tooltipLocation), 200));
            locationBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                ModNetworkManager.GET_NEAREST_STATION.send(NetworkDirection.toServer(), new GetNearestStationPacketData.Request(Minecraft.getInstance().player.blockPosition()), (response) -> {
                    if (response.getResult().tagName.isPresent()) {
                        stationBox.text.get().set(response.getResult().tagName.get().get());
                    }
                }, () -> {});
                return false;
            });
        }

        DLPanel optionsPanel = new DLPanel(3, 30 + FooterSize.DEFAULT.size(), width() - 6, 18);
        TableLayout layout = new TableLayout();
        layout.addColumn("departure", 0.3333333f, ColumnSizeMode.PERCENTAGE);
        layout.addColumn("categories", 0.3333333f, ColumnSizeMode.PERCENTAGE);
        layout.addColumn("filter", 0.3333333f, ColumnSizeMode.PERCENTAGE);
        layout.addColumn("refresh", 18, ColumnSizeMode.FIXED);
        optionsPanel.layout.set(layout);
        addComponent(optionsPanel);

        SearchOptionButton departureInBtn = new SearchOptionButton(0, 0, 100, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.departure_in"), () -> userSettings.searchDepartureInTicks.toString(), (b) -> {
            getWindowManager().createModal((mgr) -> new FlyoutDepartureInWidget(mgr, b, FlyoutPointer.UP, ColorShade.DARK, userSettings, () -> userSettings.searchDepartureInTicks));
        });
        departureInBtn.layoutContraint.set("departure");
        optionsPanel.addComponent(departureInBtn);
        
        SearchOptionButton trainCategoriesBtn = new SearchOptionButton(0, 0, 100, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_categories"), () -> userSettings.searchExcludedTrainCaegories.toString(), (b) -> {
            getWindowManager().createModal((mgr) -> new FlyoutTrainCategoriesWidget(mgr, b, FlyoutPointer.UP, ColorShade.DARK, userSettings, () -> userSettings.searchExcludedTrainCaegories));
        });
        trainCategoriesBtn.layoutContraint.set("categories");
        optionsPanel.addComponent(trainCategoriesBtn);
                
        SearchOptionButton trainFilterBtn = new SearchOptionButton(0, 0, 100, 18, userSettings.searchTrainFilter.getValue().getEnumTranslation(), () -> userSettings.searchTrainFilter.toString(), (b) -> {
            this.userSettings.searchTrainFilter.setValue(this.userSettings.searchTrainFilter.getValue().next());
            this.userSettings.clientSave(() -> {
                reloadUserSettings(() -> this.viewer.displayDepartures(stationTagName, userSettings));
            });
        });
        trainFilterBtn.layoutContraint.set("filter");
        optionsPanel.addComponent(trainFilterBtn);

        FlatIconButton refreshBtn = new FlatIconButton(0, 0, ModGuiIcons.REFRESH.getAsSprite(16, 16));
        refreshBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            reloadUserSettings(() -> this.viewer.displayDepartures(stationTagName, userSettings));
            return false;
        });
        refreshBtn.layoutContraint.set("refresh");
        refreshBtn.tooltip.set(new DLTooltip(List.of(tooltipRefresh), 200));
        optionsPanel.addComponent(refreshBtn);

        reloadUserSettings(() -> this.viewer.displayDepartures(stationTagName, userSettings));
    }

    private void reloadUserSettings(Runnable andThen) {
        ModNetworkManager.GET_USER_SETTINGS.send(NetworkDirection.toServer(), new GetUserSettingsPacketData.Request(Minecraft.getInstance().player.getUUID()), (response) -> {
            response.getData().ifPresent(s -> this.userSettings = s);
            DLUtils.doIfNotNull(andThen, Runnable::run);
        }, () -> {});
    }


    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        super.renderMainLayer(graphics, mouseX, mouseY, renderBounds);
        int dy = FooterSize.DEFAULT.size() - 1;
        CreateDynamicWidgets.renderContainer(graphics, 1, dy, width() - 2, 30, ContainerColor.BLUE);
        dy += 29;
        CreateDynamicWidgets.renderContainer(graphics, 1, dy, width() - 2, 22, ContainerColor.GOLD);
        dy += 21;
        CreateDynamicWidgets.renderContainer(graphics, 1, dy, width() - 2, height() - dy - FooterSize.SMALL.size() + 1, ContainerColor.PURPLE);
        
        if (fixedStation) {
            graphics.poseStack().pushPose();
            graphics.poseStack().scale(2, 2, 2);
            GuiUtils.drawString(graphics, graphics.defaultFont(), (GUI_WIDTH / 2) / 2, 22 / 2, TextUtils.truncateWithEllipsis(graphics.defaultFont(), TextUtils.text(stationTagName), GUI_WIDTH / 2 - 20), DLColor.WHITE, ETextAlignment.CENTER, false);
            graphics.poseStack().popPose();
        } else {            
            ModGuiIcons.POSITION.render(graphics, 8, FooterSize.DEFAULT.size() + 6);
            CreateDynamicWidgets.renderTextBox(graphics, 32, 20, 154);
        }
    }
}
