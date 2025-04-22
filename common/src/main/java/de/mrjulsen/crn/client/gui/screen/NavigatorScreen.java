package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.AbstractNotificationPopup;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.ModDestinationSuggestions;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.client.gui.widgets.RouteViewer;
import de.mrjulsen.crn.client.gui.widgets.SearchOptionButton;
import de.mrjulsen.crn.client.gui.widgets.flyouts.FlyoutAdvancedSearchsettingsWidget;
import de.mrjulsen.crn.client.gui.widgets.flyouts.FlyoutDepartureInWidget;
import de.mrjulsen.crn.client.gui.widgets.flyouts.FlyoutTrainGroupsWidget;
import de.mrjulsen.crn.client.gui.widgets.flyouts.FlyoutTransferTimeWidget;
import de.mrjulsen.crn.client.gui.widgets.notifications.NotificationTrainInitialization;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public class NavigatorScreen extends AbstractNavigatorScreen {

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private boolean initialized = false;

    // Params
    private final String fixedStartStation;
    private final boolean isPublic;
    
    // Controls
    private DLCreateIconButton locationButton;
    private DLCreateIconButton searchButton;   
    private DLCreateIconButton globalSettingsButton;
	private DLEditBox fromBox;
	private DLEditBox toBox;    
    private RouteViewer routeViewer;
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);   
	private ModDestinationSuggestions destinationSuggestions; 
    private AbstractNotificationPopup notificationPopup;

    private UserSettings userSettings = new UserSettings(Minecraft.getInstance().player.getUUID(), false);

    // Data
    private final List<StationTag> stationNames = new ArrayList<>();
    private String stationFrom = "";
    private String stationTo = "";
    private final NavigatorScreen instance;
    private Collection<ClientRoute> currentRoutes;


    // Tooltips
    private final MutableComponent errorTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.error_title");
    private final MutableComponent startEndEqualText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.start_end_equal");
    private final MutableComponent startEndNullText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.start_end_null");

    private final MutableComponent tooltipSearch = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.search.tooltip");
    private final MutableComponent tooltipLocation = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.location.tooltip");
    private final MutableComponent tooltipSwitch = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.switch.tooltip");
    private final MutableComponent tooltipGlobalSettings = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.global_settings.tooltip");
    //private final MutableComponent tooltipUserProfile = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.my_profile");
    private final MutableComponent tooltipSavedRoutes = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.title");
    private final MutableComponent tooltipScheduleViewer = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.title");


    public NavigatorScreen(Screen lastScreen, String fixedStartStation, boolean isPublic) {
        super(lastScreen, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.title"), BarColor.GRAY);
        this.instance = this;
        this.fixedStartStation = fixedStartStation;
        this.isPublic = isPublic;

        if (fixedStartStation != null) {
            stationFrom = fixedStartStation;
        }
    }

    private void generateRouteEntries() {
        
    }   

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
    }

    private void switchButtonClick() {
        String fromInput = fromBox.getValue();
        String toInput = toBox.getValue();

        fromBox.setValue(toInput);
        toBox.setValue(fromInput);
    }

    @Override
    protected void init() {
        super.init();
        setAllowedLayer(0);
        initialized = false;

        DataAccessor.getFromServer(true, ModAccessorTypes.GET_ALL_STATIONS_AS_TAGS, (names) -> {
            this.stationNames.clear();
            this.stationNames.addAll(names);
        });

        if (fixedStartStation == null) {
            locationButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 195, guiTop + 20, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.POSITION.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    DataAccessor.getFromServer(minecraft.player.blockPosition(), ModAccessorTypes.GET_NEAREST_STATION, (result) -> {
                        if (result.tagName.isPresent()) {
                            fromBox.setValue(result.tagName.get().get());
                        }
                    });
                }
            });
            addTooltip(DLTooltip.of(tooltipLocation).assignedTo(locationButton));        
    
            fromBox = addEditBox(guiLeft + 32 + 5, guiTop + 25, 157, 12, stationFrom, TextUtils.empty(), false, (v) -> {
                if (!initialized) {
                    return;
                }
                stationFrom = v;
                updateEditorSubwidgets(fromBox);
            }, NO_EDIT_BOX_FOCUS_CHANGE_ACTION, null);
            fromBox.setMaxLength(StationTag.MAX_NAME_LENGTH);
        }

        toBox = addEditBox(guiLeft + 32 + 5, guiTop + 47, 157, 12, stationTo, TextUtils.empty(), false, (v) -> {
            if (!initialized) {
                return;
            }
            stationTo = v;
            updateEditorSubwidgets(toBox);
        }, NO_EDIT_BOX_FOCUS_CHANGE_ACTION, null);
		toBox.setMaxLength(StationTag.MAX_NAME_LENGTH);


        // Global Options Button
        globalSettingsButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 30, guiTop + 223, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.SETTINGS.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                minecraft.setScreen(new GlobalSettingsScreen(instance));
            }
        });
        addTooltip(DLTooltip.of(tooltipGlobalSettings).assignedTo(globalSettingsButton));

        if (!isPublic) {
            /*
            DLCreateIconButton userProfileBtn = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - DEFAULT_ICON_BUTTON_WIDTH - 49, guiTop + 223, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.USER.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                }
            });
            addTooltip(DLTooltip.of(tooltipUserProfile).assignedTo(userProfileBtn));
            */
            
            DLCreateIconButton savedRoutes = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - DEFAULT_ICON_BUTTON_WIDTH - 37, guiTop + 223, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.MAP_PATH.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    minecraft.setScreen(new SavedRoutesScreen(instance));
                }
            });
            addTooltip(DLTooltip.of(tooltipSavedRoutes).assignedTo(savedRoutes));
        }
        
        DLCreateIconButton scheduleBoardBtn = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - DEFAULT_ICON_BUTTON_WIDTH - 8, guiTop + 223, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.VERY_DETAILED.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                minecraft.setScreen(new ScheduleBoardScreen(instance, null));
            }
        });
        addTooltip(DLTooltip.of(tooltipScheduleViewer).assignedTo(scheduleBoardBtn));

        if (fixedStartStation == null) {
            DLIconButton btn = addRenderableWidget((new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, new Sprite(CRNGui.GUI, CRNGui.GUI_WIDTH, CRNGui.GUI_HEIGHT, 55, 0, 9, 12), guiLeft + 176, guiTop + 33, 13, 14, TextUtils.empty(), (b) -> switchButtonClick())));
            addTooltip(DLTooltip.of(tooltipSwitch).assignedTo(btn));
            btn.setBackColor(0x00000000);
        }
        
        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, guiLeft + GUI_WIDTH - 8, guiTop + 88, 128, GuiAreaDefinition.empty());
        routeViewer = addRenderableWidget(new RouteViewer(this, guiLeft + 3, guiTop + 88, GUI_WIDTH - 6, 128, scrollBar, routes -> {
            currentRoutes = routes;
        }));
        routeViewer.enableRecentlySearchedList(userSettings);
        routeViewer.setRoutes(currentRoutes);
        addRenderableWidget(scrollBar);

        searchButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 195, guiTop + 42, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_MTD_SCAN) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                
                if (stationFrom == null || stationTo == null || stationFrom.isBlank() || stationTo.isBlank()) {
                    Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, errorTitle, startEndNullText));
                    return;
                }

                if (stationFrom.equals(stationTo)) {
                    Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, errorTitle, startEndEqualText));
                    return;
                }

                routeViewer.search(stationFrom, stationTo, userSettings, (viewer) -> {
                    DataAccessor.getFromServer(null, ModAccessorTypes.ALL_TRAINS_INITIALIZED, (result) -> {
                        DLUtils.doIfNotNull(notificationPopup, x -> x.close());
                        if (!result) notificationPopup = addRenderableWidget(new NotificationTrainInitialization(instance, guiLeft + 10, guiTop + GUI_HEIGHT - FooterSize.DEFAULT.size() - 20, GUI_WIDTH - 20, instance::removeWidget));
                    });
                });
            }
        });
        addTooltip(DLTooltip.of(tooltipSearch).assignedTo(searchButton));

        // Search Options
        final int btnCount = 3;
        int btnWidth = (GUI_WIDTH - 6 - 16) / btnCount;
        addRenderableWidget(new SearchOptionButton(guiLeft + 3, guiTop + 54 + FooterSize.DEFAULT.size() - 2, btnWidth, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.departure_in"), () -> userSettings.navigationDepartureInTicks.toString(), (b) -> {
            new FlyoutDepartureInWidget<>(this, FlyoutPointer.UP, ColorShade.DARK, this::addRenderableWidget, userSettings, () -> {
                return userSettings.navigationDepartureInTicks;
            }, (w) -> {
                removeWidget(w);
                reloadUserSettings();
            }).open(b);
        }));
        addRenderableWidget(new SearchOptionButton(guiLeft + 3 + btnWidth, guiTop + 54 + FooterSize.DEFAULT.size() - 2, btnWidth, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.transfer_time"), () -> userSettings.navigationTransferTime.toString(), (b) -> {
            new FlyoutTransferTimeWidget<>(this, FlyoutPointer.UP, ColorShade.DARK, this::addRenderableWidget, userSettings, () -> {
                return userSettings.navigationTransferTime;
            }, (w) -> {
                removeWidget(w);
                reloadUserSettings();
            }).open(b);
        }));
        addRenderableWidget(new SearchOptionButton(guiLeft + 3 + btnWidth * 2, guiTop + 54 + FooterSize.DEFAULT.size() - 2, btnWidth, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_groups"), () -> userSettings.navigationExcludedTrainGroups.toString(), (b) -> {
            new FlyoutTrainGroupsWidget<>(this, FlyoutPointer.UP, ColorShade.DARK, this::addRenderableWidget, userSettings, () -> {
                return userSettings.navigationExcludedTrainGroups;
            }, (w) -> {
                removeWidget(w);
                reloadUserSettings();
            }).open(b);
        }));
        DLIconButton moreSearchOptionsBtn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, GuiIcons.ARROW_RIGHT.getAsSprite(16, 16), guiLeft + GUI_WIDTH - 3 - (GUI_WIDTH - btnWidth * 3 - 6), guiTop + 54 + FooterSize.DEFAULT.size() - 2, (GUI_WIDTH - btnWidth * 3 - 6), 18, TextUtils.empty(),
        (b) -> {            
            new FlyoutAdvancedSearchsettingsWidget<>(this, FlyoutPointer.UP, ColorShade.DARK, this::addRenderableWidget, (w) -> {
                removeWidget(w);
            }).open(b);
        }));
        moreSearchOptionsBtn.setBackColor(0x00000000);

        generateRouteEntries();
        reloadUserSettings();
        initialized = true;
    }

    private void reloadUserSettings() {
        DataAccessor.getFromServer(Minecraft.getInstance().player.getUUID(), ModAccessorTypes.GET_USER_SETTINGS, settings -> {
            this.userSettings = settings;
            this.routeViewer.refresh(settings);
        });
    }

    protected void updateEditorSubwidgets(DLEditBox field) {        
        updateEditorSubwidgetsInternal(field, getViableStations(stationNames));
    }

    protected void updateEditorSubwidgetsInternal(DLEditBox field, List<StationTag> list) {
        clearSuggestions();
		destinationSuggestions = new ModDestinationSuggestions(this.minecraft, this, field, this.font, list, field.getHeight() + 2 + field.y());
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
	}

    private List<StationTag> getViableStations(Collection<StationTag> src) {
        return src.stream()
            .distinct()
            .sorted((a, b) -> a.getTagName().get().compareToIgnoreCase(b.getTagName().get()))
            .toList();
	}

    private void clearSuggestions() {
        if (destinationSuggestions != null) {
            destinationSuggestions.getEditBox().setSuggestion("");
        }
        destinationSuggestions = null;
    }

    @Override
    public void tick() {
        
		scroll.tickChaser();
        
        DLUtils.doIfNotNull(destinationSuggestions, x -> {
            x.tick();

            if (!toBox.canConsumeInput() && (fromBox == null || !fromBox.canConsumeInput())) {
                clearSuggestions();
            }
        });

        DLUtils.doIfNotNull(searchButton, x -> x.set_active(!routeViewer.isLoading()));

        super.tick();
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {          
        renderNavigatorBackground(graphics, pMouseX, pMouseY, pPartialTick);

        int y = FooterSize.DEFAULT.size() - 1;
        CreateDynamicWidgets.renderContainer(graphics, guiLeft + 1, guiTop + y, GUI_WIDTH - 2, 52, ContainerColor.BLUE);
        CreateDynamicWidgets.renderContainer(graphics, guiLeft + 1, guiTop + y + 52 - 1, GUI_WIDTH - 2, 22, ContainerColor.GOLD);
        y += 52 + 22 - 2;
        CreateDynamicWidgets.renderContainer(graphics, guiLeft + 1, guiTop + y, GUI_WIDTH - 2, GUI_HEIGHT - y - FooterSize.SMALL.size() + 1, ContainerColor.GRAY);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, guiLeft + GUI_WIDTH - DEFAULT_ICON_BUTTON_WIDTH - 14, guiTop + 218, 27, BarColor.GRAY);

        if (fixedStartStation == null) {            
            CreateDynamicWidgets.renderTextBox(graphics, guiLeft + 32, guiTop + 20, 159);
        } else {
            GuiUtils.drawString(graphics, font, guiLeft + 32, guiTop + 25, TextUtils.text(fixedStartStation).withStyle(ChatFormatting.BOLD), 0xFFFFFFFF, EAlignment.LEFT, true);
        }
        CreateDynamicWidgets.renderTextBox(graphics, guiLeft + 32, guiTop + 42, 159);
        GuiUtils.drawTexture(CRNGui.GUI, graphics, guiLeft + 16, guiTop + 16, 7, 24, 0, 30, 7, 24, CRNGui.GUI_WIDTH, CRNGui.GUI_HEIGHT);
        GuiUtils.drawTexture(CRNGui.GUI, graphics, guiLeft + 16, guiTop + 16 + 24, 7, 24, 7, 30, 7, 24, CRNGui.GUI_WIDTH, CRNGui.GUI_HEIGHT);

        

        
        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
	public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
		if (destinationSuggestions != null) {
			graphics.poseStack().pushPose();
			graphics.poseStack().translate(0, 0, 500);
			destinationSuggestions.render(graphics.poseStack(), mouseX, mouseY);
			graphics.poseStack().popPose();
		}
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (destinationSuggestions != null && destinationSuggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
			return true;

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (destinationSuggestions != null && destinationSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers))
			return true;

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(pMouseX, pMouseY, Mth.clamp(pDelta, -1.0D, 1.0D)))
			return true;

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }
    
}
