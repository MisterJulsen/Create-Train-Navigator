package de.mrjulsen.crn.client.gui.widgets;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.screen.AbstractEntryListSettingsScreen;
import de.mrjulsen.crn.data.AliasName;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.data.TrainStationAlias.StationInfo;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class AliasEntryWidget extends AbstractEntryListOptionWidget {

    private static final ResourceLocation GUI_WIDGETS = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/settings_widgets.png");
    public static final int WIDTH = 200;
    public static final int HEIGHT = 48;
    private static final int STATION_ENTRY_HEIGHT = 20;
        
    private final AbstractEntryListSettingsScreen<TrainStationAlias, AliasEntryWidget> parent;
    private final Font shadowlessFont;
    private final Minecraft minecraft;

    private final Runnable onUpdate;

    // Data
    private TrainStationAlias alias;
    private boolean expanded = false;

    // Controls
    private final DLEditBox titleBox;
    private final DLEditBox newEntryBox;
    private final DLEditBox newEntryPlatformBox;
    //private final WidgetsCollection controls = new WidgetsCollection();
    private final Map<String, GuiAreaDefinition> removeStationButtons = new HashMap<>();
    private final Map<String, GuiAreaDefinition> stationInfoAreas = new HashMap<>();
    private final Map<String, GuiAreaDefinition> stationNameAreas = new HashMap<>();

    private GuiAreaDefinition titleBarArea;

    private GuiAreaDefinition deleteButton;
    private GuiAreaDefinition expandButton;
    private GuiAreaDefinition addButton;

	private ModStationSuggestions destinationSuggestions;

    // Edit station info
    private String selectedStationName = null;
    private final DLEditBox editAliasPlatform;

    // Tooltips
    private final MutableComponent tooltipDeleteAlias = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".alias_settings.delete_alias.tooltip");
    private final MutableComponent tooltipDeleteStation = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".alias_settings.delete_station.tooltip");
    private final MutableComponent tooltipAddStation = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".alias_settings.add_station.tooltip");
    private final MutableComponent tooltipStationName = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".alias_settings.hint.station_name");
    private final MutableComponent tooltipPlatform = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".alias_settings.hint.platform");
    

    public AliasEntryWidget(AbstractEntryListSettingsScreen<TrainStationAlias, AliasEntryWidget> parent, int pX, int pY, TrainStationAlias alias, Runnable onUpdate, boolean expanded) {
        super(pX, pY, 200, 48);
        
        Minecraft minecraft = Minecraft.getInstance();
        shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        this.minecraft = Minecraft.getInstance();
        this.parent = parent;
        this.alias = alias;
        this.expanded = expanded;
        this.onUpdate = onUpdate;

        titleBox = new DLEditBox(minecraft.font, pX + 30, pY + 10, 129, 12, TextUtils.empty());
		titleBox.setBordered(false);
		titleBox.setMaxLength(32);
		titleBox.setTextColor(0xFFFFFF);
        titleBox.setValue(alias.getAliasName().get());
        titleBox.withOnFocusChanged((box, focused) -> {
            if (!focused) {
                if (!setAliasName(box.getValue())) {
                    titleBox.setValue(alias.getAliasName().get());
                }
            }
        });
        titleBox.visible = expanded;
        addRenderableWidget(titleBox);

        
        newEntryBox = new DLEditBox(minecraft.font, pX + 30, pY + 30, 95, 12, TextUtils.empty());
		newEntryBox.setBordered(false);
		newEntryBox.setMaxLength(32);
		newEntryBox.setTextColor(0xFFFFFF);
        newEntryBox.visible = expanded;
        newEntryBox.setResponder(x -> {
            updateEditorSubwidgets(newEntryBox);
        });
        addRenderableWidget(newEntryBox);

        newEntryPlatformBox = new DLEditBox(minecraft.font, pX + 134, pY + 30, 25, 12, TextUtils.empty());
		newEntryPlatformBox.setBordered(false);
		newEntryPlatformBox.setMaxLength(10);
		newEntryPlatformBox.setTextColor(0xFFFFFF);
        newEntryPlatformBox.visible = expanded;
        addRenderableWidget(newEntryPlatformBox);

        editAliasPlatform = new DLEditBox(minecraft.font, pX + 134, 0, 33, 14, TextUtils.empty());
		editAliasPlatform.setBordered(true);
		editAliasPlatform.setMaxLength(10);
		editAliasPlatform.setTextColor(0xFFFFFF);
        editAliasPlatform.visible = false;
        editAliasPlatform.withOnFocusChanged((box, focus) -> {
            if (!focus) {
                if (selectedStationName != null && !selectedStationName.isBlank()) {
                    alias.updateInfoForStation(selectedStationName, new StationInfo(box.getValue()));
                    alias.updateLastEdited(minecraft.player.getName().getString());
                    GlobalSettingsManager.getInstance().getSettingsData().updateAlias(alias.getAliasName(), alias, () -> {
                        onUpdate.run();
                        initStationDeleteButtons();
                    });
                }
                box.visible = false;
                selectedStationName = null;
                box.setValue("");
            }
        });
        addRenderableWidget(editAliasPlatform);

        setYPos(pY);
    }

    public TrainStationAlias getAlias() {
        return alias;
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void setYPos(int y) {
        this.y = y;
        deleteButton = new GuiAreaDefinition(x + 165, y + 6, 16, 16);
        expandButton = new GuiAreaDefinition(x + 182, y + 6, 16, 16);
        addButton = new GuiAreaDefinition(x + 165, y + 26 + (alias.getAllStationNames().size() * STATION_ENTRY_HEIGHT) + 2, 16, 16);
        titleBox.y = y + 10;
        initStationDeleteButtons();
    }

    private void initStationDeleteButtons() {
        removeStationButtons.clear();
        stationInfoAreas.clear();
        stationNameAreas.clear();
        
        String[] names = alias.getAllStationNames().toArray(String[]::new);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            removeStationButtons.put(name, new GuiAreaDefinition(x + 165, y + 26 + (i * STATION_ENTRY_HEIGHT) + 2, 16, 16));
            stationNameAreas.put(name, new GuiAreaDefinition(x + 25, y + 26 + (i * STATION_ENTRY_HEIGHT) + 2, 104, 16));
            stationInfoAreas.put(name, new GuiAreaDefinition(x + 129, y + 26 + (i * STATION_ENTRY_HEIGHT) + 2, 35, 16));
        }
        
        titleBarArea = new GuiAreaDefinition(x + 25, y + 6, 129, 16);
    }

    @Override
    public void tick() {
        super.tick();

        if (destinationSuggestions != null) {
            destinationSuggestions.tick();

            if (!newEntryBox.canConsumeInput()) {
                clearSuggestions();
            }
        }
    }

    private void toggleExpanded() {
        this.expanded = !expanded;
        titleBox.visible = expanded;
        newEntryBox.visible = expanded;
        newEntryPlatformBox.visible = expanded;
    }

    private void deleteAlias() {
        GlobalSettingsManager.getInstance().getSettingsData().unregisterAlias(alias, onUpdate);
    }

    private void addStation(String name, StationInfo info) {
        AliasName prevName = alias.getAliasName();
        if (ClientTrainStationSnapshot.getInstance().getAllTrainStations().stream().noneMatch(x -> x.equals(name)) || newEntryPlatformBox.getValue().isBlank()) {
            return;
        }

        alias.add(name, info);
        alias.updateLastEdited(minecraft.player.getName().getString());
        GlobalSettingsManager.getInstance().getSettingsData().updateAlias(prevName, alias, () -> {
            onUpdate.run();
            initStationDeleteButtons();
        });
        
        newEntryBox.setValue("");
        newEntryBox.setFocus(false);
        newEntryPlatformBox.setValue("");
        newEntryPlatformBox.setFocus(false);
    }

    private boolean setAliasName(String name) {
        AliasName prevName = alias.getAliasName();

        if (name == null || name.isBlank()) {
            return false;
        }

        if (GlobalSettingsManager.getInstance().getSettingsData().getAliasList().stream().anyMatch(x -> x.getAliasName().get().toLowerCase().equals(name.toLowerCase()))) {
            return false;
        }

        alias.setName(AliasName.of(name));
        alias.updateLastEdited(minecraft.player.getName().getString());
        GlobalSettingsManager.getInstance().getSettingsData().updateAlias(prevName, alias, onUpdate);
        return true;
    }

    
    @Override
    public int getHeight() {
        return height;
    }

    private void removeStation(String name) {
        AliasName prevName = alias.getAliasName();
        alias.remove(name);
        alias.updateLastEdited(minecraft.player.getName().getString());
        GlobalSettingsManager.getInstance().getSettingsData().updateAlias(prevName, alias, () -> {
            onUpdate.run();
            initStationDeleteButtons();
        });
        initStationDeleteButtons();
        onUpdate.run();
    }

    @Override
    public int calcHeight() {
        if (expanded) {            
            height = STATION_ENTRY_HEIGHT *  alias.getAllStationNames().size() + 50;
        } else {
            height = HEIGHT;
        }
        return height;
    }

    private void editStationInfo(String stationName, GuiAreaDefinition buttonArea) {
        //parent.unfocusAllWidgets();
        //parent.unfocusAllEntries();

        selectedStationName = stationName;
        editAliasPlatform.setValue(alias.getInfoForStation(selectedStationName).platform());
        editAliasPlatform.x = buttonArea.getLeft() + 1;
        editAliasPlatform.y = buttonArea.getTop() + 1;
        editAliasPlatform.visible = true;
        editAliasPlatform.setFocus(true);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) { 
        GuiUtils.drawTexture(GUI_WIDGETS, graphics, x, y, 0, 0, WIDTH, HEIGHT);

        GuiUtils.drawTexture(GUI_WIDGETS, graphics, deleteButton.getX(), deleteButton.getY(), 232, 0, 16, 16); // delete button
        GuiUtils.drawTexture(GUI_WIDGETS, graphics, expandButton.getX(), expandButton.getY(), expanded ? 216 : 200, 0, 16, 16); // expand button  

        if (expanded) {
            Map<String, StationInfo> names = alias.getAllStations();
            GuiUtils.drawTexture(GUI_WIDGETS, graphics, x + 25, y + 5, 0, 92, 139, 18); // textbox
            newEntryBox.y = y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 6;
            newEntryPlatformBox.y = y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 6;

            for (int i = 0; i < names.size(); i++) {
                GuiUtils.drawTexture(GUI_WIDGETS, graphics, x, y + 26 + (i * STATION_ENTRY_HEIGHT), 0, 48, 200, STATION_ENTRY_HEIGHT);
            }
            
            GuiUtils.drawTexture(GUI_WIDGETS, graphics, x, y + 26 + (names.size() * STATION_ENTRY_HEIGHT), 0, 68, 200, 24);
            GuiUtils.drawTexture(GUI_WIDGETS, graphics, x + 25, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 0, 92, 103, 18); // textbox
            GuiUtils.drawTexture(GUI_WIDGETS, graphics, x + 25 + 102, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 138, 92, 1, 18); // textbox

            GuiUtils.drawTexture(GUI_WIDGETS, graphics, x + 129, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 0, 92, 35, 18); // textbox
            GuiUtils.drawTexture(GUI_WIDGETS, graphics, x + 129 + 34, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 138, 92, 1, 18); // textbox
            GuiUtils.drawTexture(GUI_WIDGETS, graphics, addButton.getX(), addButton.getY(), 200, 16, 16, 16); // add button 

            for (GuiAreaDefinition def : removeStationButtons.values()) { 
                GuiUtils.drawTexture(GUI_WIDGETS, graphics, def.getX(), def.getY(), 232, 0, 16, 16); // delete button
            }

            int i = 0;
            for (Entry<String, StationInfo> entry : names.entrySet()) {
                MutableComponent name = TextUtils.text(entry.getKey());
                int maxTextWidth = 104 - 12;  
                if (shadowlessFont.width(name) > maxTextWidth) {
                    name = TextUtils.text(shadowlessFont.substrByWidth(name, maxTextWidth).getString()).append(Constants.ELLIPSIS_STRING);
                }
                GuiUtils.drawString(graphics, shadowlessFont, x + 30, y + 26 + (i * STATION_ENTRY_HEIGHT) + 6, name, 0xFFFFFF, EAlignment.LEFT, false);

                StationInfo info = entry.getValue();
                MutableComponent platform = TextUtils.text(info.platform());
                int maxPlatformWidth = 35 - 7;  
                if (shadowlessFont.width(platform) > maxPlatformWidth) {
                    platform = TextUtils.text(shadowlessFont.substrByWidth(platform, maxPlatformWidth - 3).getString()).append(Constants.ELLIPSIS_STRING);
                }                
                int platformTextWidth = shadowlessFont.width(platform);
                GuiUtils.drawString(graphics, shadowlessFont, x + 30 + 130 - platformTextWidth, y + 26 + (i * STATION_ENTRY_HEIGHT) + 6, platform, 0xFFFFFF, EAlignment.LEFT, false);
                i++;
            }
            
        } else {
            MutableComponent name = TextUtils.text(alias.getAliasName().get());
            int maxTextWidth = 129;  
            if (shadowlessFont.width(name) > maxTextWidth) {
                name = TextUtils.text(shadowlessFont.substrByWidth(name, maxTextWidth).getString()).append(Constants.ELLIPSIS_STRING);
            }
            GuiUtils.drawString(graphics, shadowlessFont, x + 30, y + 10, name, 0xFFFFFF, EAlignment.LEFT, false);

            graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 5) / 0.75f), (int)((y + 30) / 0.75f), TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".alias_settings.summary", alias.getAllStationNames().size()), 0xDBDBDB, EAlignment.LEFT, false);
            if (alias.getLastEditorName() != null && !alias.getLastEditorName().isBlank()) {
                GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 5) / 0.75f), (int)((y + 38) / 0.75f), TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".alias_settings.editor", alias.getLastEditorName(), alias.getLastEditedTimeFormatted()), 0xDBDBDB, EAlignment.LEFT, false);
            }
            float s = 1 / 0.75f;
            graphics.poseStack().scale(s, s, s);
        }

        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick); 

        // Button highlight
        if (deleteButton.isInBounds(pMouseX, pMouseY)) {
            GuiUtils.fill(graphics, deleteButton.getX(), deleteButton.getY(), deleteButton.getWidth(), deleteButton.getHeight(), 0x1AFFFFFF);
        } else if (expandButton.isInBounds(pMouseX, pMouseY)) {
            GuiUtils.fill(graphics, expandButton.getX(), expandButton.getY(), expandButton.getWidth(), expandButton.getHeight(), 0x1AFFFFFF);
        } else if (expanded && addButton.isInBounds(pMouseX, pMouseY)) {
            GuiUtils.fill(graphics, addButton.getX(), addButton.getY(), addButton.getWidth(), addButton.getHeight(), 0x1AFFFFFF);
        } else if (expanded && removeStationButtons.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY))) {
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY)) {
                    GuiUtils.fill(graphics, entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getWidth(), entry.getValue().getHeight(), 0x1AFFFFFF);
                }
            }
        } else if (expanded && stationInfoAreas.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY))) {
            for (Entry<String, GuiAreaDefinition> entry : stationInfoAreas.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY)) {
                    GuiUtils.fill(graphics, entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getWidth(), entry.getValue().getHeight(), 0x1AFFFFFF);
                }
            }
        }
    }
    
    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        GuiUtils.renderTooltipWithOffset(parent, newEntryBox, List.of(tooltipStationName), width / 2, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        GuiUtils.renderTooltipWithOffset(parent, newEntryPlatformBox, List.of(tooltipPlatform), width / 2, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        GuiUtils.renderTooltipWithOffset(parent, deleteButton, List.of(tooltipDeleteAlias), width / 2, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        GuiUtils.renderTooltipWithOffset(parent, expandButton, List.of(expanded ? Constants.TOOLTIP_COLLAPSE : Constants.TOOLTIP_EXPAND), width / 2, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));

        if (expanded) {
            GuiUtils.renderTooltipWithOffset(parent, addButton, List.of(tooltipAddStation), width / 2, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));        
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (GuiUtils.renderTooltipWithOffset(parent, entry.getValue(), List.of(tooltipDeleteStation), width / 2, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks))) {
                    break;
                }
            }

            for (Entry<String, GuiAreaDefinition> entry : stationNameAreas.entrySet()) {
                if (shadowlessFont.width(entry.getKey()) > 104 - 12 && GuiUtils.renderTooltipAt(parent, entry.getValue(), List.of(TextUtils.text(entry.getKey())), width, graphics, entry.getValue().getLeft() + 1, entry.getValue().getTop() - parent.getScrollOffset(partialTicks), mouseX, mouseY, 0, parent.getScrollOffset(partialTicks))) {
                    break;
                }
            }

            for (Entry<String, GuiAreaDefinition> entry : stationInfoAreas.entrySet()) {
                MutableComponent text = TextUtils.text(alias.getInfoForStation(entry.getKey()).platform());
                if ((selectedStationName == null || !entry.getKey().equals(selectedStationName)) && shadowlessFont.width(text) > 35 - 7 && GuiUtils.renderTooltipAt(parent, entry.getValue(), List.of(text), width, graphics, entry.getValue().getLeft(), entry.getValue().getTop() - parent.getScrollOffset(partialTicks), mouseX, mouseY, 0, parent.getScrollOffset(partialTicks))) {
                    break;
                }
            }
        } else {
            if (titleBarArea.isInBounds(mouseX, mouseY + parent.getScrollOffset(partialTicks)) && shadowlessFont.width(alias.getAliasName().get()) > 129) {
                GuiUtils.renderTooltipAt(parent, titleBarArea, List.of(TextUtils.text(alias.getAliasName().get())), width, graphics, titleBarArea.getLeft() + 1, titleBarArea.getTop() - parent.getScrollOffset(partialTicks), mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
            }
        }
    }

    @Override
    public void renderSuggestions(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (destinationSuggestions != null) {
			matrixStack.pushPose();
			matrixStack.translate(0, -parent.getScrollOffset(partialTicks), 500);
			destinationSuggestions.render(matrixStack, mouseX, mouseY + parent.getScrollOffset(partialTicks));
			matrixStack.popPose();
		}
    }

    @Override
    public boolean mouseClickedLoop(double pMouseX, double pMouseY, int pButton) {
        //parent.unfocusAllEntries();

        if (destinationSuggestions != null && destinationSuggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
            return super.mouseClicked(pMouseX, pMouseY, pButton);

        return false;
    }
    
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {

        if (expanded && stationInfoAreas.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY))) {
            for (Entry<String, GuiAreaDefinition> entry : stationInfoAreas.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY)) {   
                    editStationInfo(entry.getKey(), entry.getValue());
                    return super.mouseClicked(pMouseX, pMouseY, pButton);
                }
            }
        }

        editAliasPlatform.setFocus(false);

        if (deleteButton.isInBounds(pMouseX, pMouseY)) {
            deleteAlias();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expandButton.isInBounds(pMouseX, pMouseY)) {
            toggleExpanded();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expanded && addButton.isInBounds(pMouseX, pMouseY)) {
            addStation(newEntryBox.getValue(), new StationInfo(newEntryPlatformBox.getValue()));
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expanded && removeStationButtons.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY))) {
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY)) {
                    removeStation(entry.getKey());
                    return super.mouseClicked(pMouseX, pMouseY, pButton);
                }
            }
        }
        
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseScrolledLoop(double pMouseX, double pMouseY, double pDelta) {
        if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(pMouseX, pMouseY, MathUtils.clamp(pDelta, -1.0D, 1.0D)))
			return true;

        return false;
    }

    private void clearSuggestions() {
        if (destinationSuggestions != null) {
            destinationSuggestions.getEditBox().setSuggestion("");
        }
        destinationSuggestions = null;
    }


    protected void updateEditorSubwidgets(DLEditBox field) {
        clearSuggestions();

		destinationSuggestions = new ModStationSuggestions(minecraft, parent, field, minecraft.font, getViableStations(field), field.getHeight() + 2 + field.y);
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
	}

    private List<String> getViableStations(DLEditBox field) {
        return ClientTrainStationSnapshot.getInstance().getAllTrainStations().stream()
            .distinct()
            .filter(x -> !GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(x) && !alias.contains(x))
            .sorted((a, b) -> a.compareTo(b))
            .toList();
	}

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
    }
}
