package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.ModDestinationSuggestions;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.client.gui.widgets.SearchOptionButton;
import de.mrjulsen.crn.client.gui.widgets.StationDeparturesViewer;
import de.mrjulsen.crn.client.gui.widgets.flyouts.FlyoutDepartureInWidget;
import de.mrjulsen.crn.client.gui.widgets.flyouts.FlyoutTrainGroupsWidget;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.StationTag.ClientStationTag;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

public class ScheduleBoardScreen extends AbstractNavigatorScreen {

    private StationDeparturesViewer viewer;

    @SuppressWarnings("resource")
    private UserSettings userSettings = new UserSettings(Minecraft.getInstance().player.getUUID(), false);

    private DLEditBox stationBox;
    private String stationFrom;
	private ModDestinationSuggestions destinationSuggestions;

    private GuiAreaDefinition workingArea;
    private String stationTagName;
    private final boolean fixedStation;

    private final List<StationTag> stationNames = new ArrayList<>();

    public ScheduleBoardScreen(Screen lastScreen, ClientStationTag tag) {
        super(lastScreen, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.title"), BarColor.GOLD);
        this.fixedStation = tag != null;
        if (fixedStation) {            
            this.stationTagName = tag.tagName();
        }
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
    public void tick() {
        
        DLUtils.doIfNotNull(destinationSuggestions, x -> {
            x.tick();

            if (!stationBox.canConsumeInput()) {
                clearSuggestions();
            }
        });
        super.tick();
    }

    @Override
    protected void init() {
        super.init();
        
        DataAccessor.getFromServer(true, ModAccessorTypes.GET_ALL_STATIONS_AS_TAGS, (names) -> {
            this.stationNames.clear();
            this.stationNames.addAll(names);
        });

        setAllowedLayer(0);
        int wY = FooterSize.DEFAULT.size() - 1;
        int wH = GUI_HEIGHT - wY - FooterSize.SMALL.size();
        workingArea = new GuiAreaDefinition(guiLeft + 3, guiTop + wY + 2, GUI_WIDTH - 6, wH - 3);

        if (!fixedStation) {            
            stationBox = addEditBox(guiLeft + 32 + 5, guiTop + 25, 152, 12, stationFrom, TextUtils.empty(), false, (v) -> {
                stationFrom = v;
                updateEditorSubwidgets(stationBox);
            }, NO_EDIT_BOX_FOCUS_CHANGE_ACTION, null);
            stationBox.setMaxLength(StationTag.MAX_NAME_LENGTH);

            this.addRenderableWidget(new DLCreateIconButton(guiLeft + 190, guiTop + 20, 18, 18, AllIcons.I_MTD_SCAN) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    if (stationFrom == null || stationFrom.isBlank()) {
                        viewer.displayRoutes(null, userSettings);
                        return;
                    }
                    DataAccessor.getFromServer(TagName.of(stationFrom), ModAccessorTypes.GET_STATION_TAG_BY_TAG_NAME, (result) -> {
                        stationTagName = result.getTagName().get();
                        viewer.displayRoutes(stationTagName, userSettings);
                    });
                }
            });

            this.addRenderableWidget(new DLCreateIconButton(guiLeft + 212, guiTop + 20, 18, 18, ModGuiIcons.POSITION.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    DataAccessor.getFromServer(minecraft.player.blockPosition(), ModAccessorTypes.GET_NEAREST_STATION, (result) -> {
                        if (result.tagName.isPresent()) {
                            stationBox.setValue(result.tagName.get().get());
                        }
                    });
                }
            });
        }
        
        // Search Options
        final int btnCount = 2;
        int btnWidth = (workingArea.getWidth() - 16) / btnCount;
        addRenderableWidget(new SearchOptionButton(workingArea.getLeft(), workingArea.getTop() + 16 + FooterSize.DEFAULT.size() - 2, btnWidth, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.departure_in"), () -> userSettings.searchDepartureInTicks.toString(), (b) -> {
            new FlyoutDepartureInWidget<>(this, FlyoutPointer.UP, ColorShade.DARK, this::addRenderableWidget, userSettings, () -> {
                return userSettings.searchDepartureInTicks;
            }, (w) -> {
                removeWidget(w);
                reloadUserSettings(() -> this.viewer.displayRoutes(stationTagName, userSettings));
            }).open(b);
        }));
        addRenderableWidget(new SearchOptionButton(workingArea.getLeft() + btnWidth, workingArea.getTop() + 16 + FooterSize.DEFAULT.size() - 2, btnWidth, 18, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_groups"), () -> userSettings.searchExcludedTrainGroups.toString(), (b) -> {
            new FlyoutTrainGroupsWidget<>(this, FlyoutPointer.UP, ColorShade.DARK, this::addRenderableWidget, userSettings, () -> {
                return userSettings.searchExcludedTrainGroups;
            }, (w) -> {
                removeWidget(w);
                reloadUserSettings(() -> this.viewer.displayRoutes(stationTagName, userSettings));
            }).open(b);
        }));
        DLIconButton moreSearchOptionsBtn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.REFRESH.getAsSprite(16, 16), workingArea.getRight() - (workingArea.getWidth() - btnWidth * btnCount), workingArea.getTop() + 16 + FooterSize.DEFAULT.size() - 2, (workingArea.getWidth() - btnWidth * btnCount), 18, TextUtils.empty(),
        (b) -> {            
            reloadUserSettings(() -> this.viewer.displayRoutes(stationTagName, userSettings));
        }));
        moreSearchOptionsBtn.setBackColor(0x00000000);

        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, workingArea.getRight() - 5, workingArea.getY() + 50, workingArea.getHeight() - 50, GuiAreaDefinition.of(lastScreen));
        this.viewer = new StationDeparturesViewer(this, workingArea.getX(), workingArea.getY() + 50, workingArea.getWidth(), workingArea.getHeight() - 50, scrollBar);
        
        addRenderableWidget(viewer);
        addRenderableWidget(scrollBar);
        reloadUserSettings(() -> this.viewer.displayRoutes(stationTagName, userSettings));
    }

    @SuppressWarnings("resource")
    private void reloadUserSettings(Runnable andThen) {
        DataAccessor.getFromServer(Minecraft.getInstance().player.getUUID(), ModAccessorTypes.GET_USER_SETTINGS, settings -> {
            this.userSettings = settings;
            DLUtils.doIfNotNull(andThen, Runnable::run);
        });
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderNavigatorBackground(graphics, mouseX, mouseY, partialTicks);
        CreateDynamicWidgets.renderContainer(graphics, workingArea.getX() - 2, workingArea.getY() - 2, workingArea.getWidth() + 4, 30, ContainerColor.BLUE);
        CreateDynamicWidgets.renderContainer(graphics, workingArea.getX() - 2, workingArea.getY() - 2 + 29, workingArea.getWidth() + 4, 22, ContainerColor.GOLD);
        CreateDynamicWidgets.renderContainer(graphics, workingArea.getX() - 2, workingArea.getY() - 2 + 50, workingArea.getWidth() + 4, workingArea.getHeight() + 4 - 50, ContainerColor.PURPLE);
        
        if (fixedStation) {
            graphics.poseStack().pushPose();
            graphics.poseStack().scale(2, 2, 2);
            GuiUtils.drawString(graphics, font, (guiLeft + GUI_WIDTH / 2) / 2, (guiTop + 22) / 2, GuiUtils.ellipsisString(font, TextUtils.text(stationTagName), GUI_WIDTH / 2), 0xFFFFFF, EAlignment.CENTER, false);
            graphics.poseStack().popPose();
        } else {            
            ModGuiIcons.POSITION.render(graphics, workingArea.getX() + 5, workingArea.getY() + 4);
            CreateDynamicWidgets.renderTextBox(graphics, guiLeft + 32, guiTop + 20, 154);
        }

        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
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
    }    @Override
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
