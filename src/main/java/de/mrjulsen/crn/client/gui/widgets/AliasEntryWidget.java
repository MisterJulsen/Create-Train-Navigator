package de.mrjulsen.crn.client.gui.widgets;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import de.mrjulsen.mcdragonlib.client.gui.WidgetsCollection;
import de.mrjulsen.mcdragonlib.client.gui.widgets.ModEditBox;
import de.mrjulsen.mcdragonlib.utils.Utils;
import de.mrjulsen.crn.client.gui.screen.AbstractEntryListSettingsScreen;
import de.mrjulsen.crn.data.AliasName;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.data.TrainStationAlias.StationInfo;
import de.mrjulsen.crn.util.ModGuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AliasEntryWidget extends AbstractEntryListOptionWidget {

    private static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings_widgets.png");
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
    private final ModEditBox titleBox;
    private final ModEditBox newEntryBox;
    private final ModEditBox newEntryPlatformBox;
    private final WidgetsCollection controls = new WidgetsCollection();
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
    private final ModEditBox editAliasPlatform;

    // Tooltips
    private final MutableComponent tooltipDeleteAlias = Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.delete_alias.tooltip");
    private final MutableComponent tooltipDeleteStation = Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.delete_station.tooltip");
    private final MutableComponent tooltipAddStation = Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.add_station.tooltip");
    private final MutableComponent tooltipStationName = Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.hint.station_name");
    private final MutableComponent tooltipPlatform = Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.hint.platform");
    

    public AliasEntryWidget(AbstractEntryListSettingsScreen<TrainStationAlias, AliasEntryWidget> parent, int pX, int pY, TrainStationAlias alias, Runnable onUpdate, boolean expanded) {
        super(pX, pY, 200, 48, Utils.text(alias.getAliasName().get()), (btn) -> {});
        
        Minecraft minecraft = Minecraft.getInstance();
        shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        this.minecraft = Minecraft.getInstance();
        this.parent = parent;
        this.alias = alias;
        this.expanded = expanded;
        this.onUpdate = onUpdate;

        titleBox = new ModEditBox(minecraft.font, pX + 30, pY + 10, 129, 12, Utils.emptyText());
		titleBox.setBordered(false);
		titleBox.setMaxLength(32);
		titleBox.setTextColor(0xFFFFFF);
        titleBox.setValue(alias.getAliasName().get());
        titleBox.setOnFocusChanged((box, focused) -> {
            if (!focused) {
                if (!setAliasName(box.getValue())) {
                    titleBox.setValue(alias.getAliasName().get());
                }
            }
        });
        titleBox.visible = expanded;
        controls.components.add(titleBox);

        
        newEntryBox = new ModEditBox(minecraft.font, pX + 30, pY + 30, 95, 12, Utils.emptyText());
		newEntryBox.setBordered(false);
		newEntryBox.setMaxLength(32);
		newEntryBox.setTextColor(0xFFFFFF);
        newEntryBox.visible = expanded;
        newEntryBox.setResponder(x -> {
            updateEditorSubwidgets(newEntryBox);
        });
        controls.components.add(newEntryBox);

        newEntryPlatformBox = new ModEditBox(minecraft.font, pX + 134, pY + 30, 25, 12, Utils.emptyText());
		newEntryPlatformBox.setBordered(false);
		newEntryPlatformBox.setMaxLength(10);
		newEntryPlatformBox.setTextColor(0xFFFFFF);
        newEntryPlatformBox.visible = expanded;
        controls.components.add(newEntryPlatformBox);

        editAliasPlatform = new ModEditBox(minecraft.font, pX + 134, 0, 33, 14, Utils.emptyText());
		editAliasPlatform.setBordered(true);
		editAliasPlatform.setMaxLength(10);
		editAliasPlatform.setTextColor(0xFFFFFF);
        editAliasPlatform.visible = false;
        editAliasPlatform.setOnFocusChanged((box, focus) -> {
            if (!focus) {
                if (selectedStationName != null && !selectedStationName.isBlank()) {
                    alias.updateInfoForStation(selectedStationName, new StationInfo(box.getValue()));
                }
                box.visible = false;
                selectedStationName = null;
                box.setValue("");
            }
        });
        controls.components.add(editAliasPlatform);

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
        controls.performForEach(x -> x.visible && x instanceof EditBox, x -> ((EditBox)x).tick());

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
    public void unfocusAll() {
        controls.performForEachOfType(ModEditBox.class, x -> x.setFocus(false));
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
        parent.unfocusAllWidgets();
        parent.unfocusAllEntries();

        selectedStationName = stationName;
        editAliasPlatform.setValue(alias.getInfoForStation(selectedStationName).platform());
        editAliasPlatform.x = buttonArea.getLeft() + 1;
        editAliasPlatform.y = buttonArea.getTop() + 1;
        editAliasPlatform.visible = true;
        editAliasPlatform.setFocus(true);
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) { 
        GuiUtils.blit(GUI_WIDGETS, pPoseStack, x, y, 0, 0, WIDTH, HEIGHT);

        GuiUtils.blit(GUI_WIDGETS, pPoseStack, deleteButton.getX(), deleteButton.getY(), 232, 0, 16, 16); // delete button
        GuiUtils.blit(GUI_WIDGETS, pPoseStack, expandButton.getX(), expandButton.getY(), expanded ? 216 : 200, 0, 16, 16); // expand button  

        if (expanded) {
            Map<String, StationInfo> names = alias.getAllStations();
            GuiUtils.blit(GUI_WIDGETS, pPoseStack, x + 25, y + 5, 0, 92, 139, 18); // textbox
            newEntryBox.y = y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 6;
            newEntryPlatformBox.y = y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 6;

            for (int i = 0; i < names.size(); i++) {
                GuiUtils.blit(GUI_WIDGETS, pPoseStack, x, y + 26 + (i * STATION_ENTRY_HEIGHT), 0, 48, 200, STATION_ENTRY_HEIGHT);
            }
            
            GuiUtils.blit(GUI_WIDGETS, pPoseStack, x, y + 26 + (names.size() * STATION_ENTRY_HEIGHT), 0, 68, 200, 24);
            GuiUtils.blit(GUI_WIDGETS, pPoseStack, x + 25, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 0, 92, 103, 18); // textbox
            GuiUtils.blit(GUI_WIDGETS, pPoseStack, x + 25 + 102, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 138, 92, 1, 18); // textbox

            GuiUtils.blit(GUI_WIDGETS, pPoseStack, x + 129, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 0, 92, 35, 18); // textbox
            GuiUtils.blit(GUI_WIDGETS, pPoseStack, x + 129 + 34, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 138, 92, 1, 18); // textbox
            GuiUtils.blit(GUI_WIDGETS, pPoseStack, addButton.getX(), addButton.getY(), 200, 16, 16, 16); // add button 

            for (GuiAreaDefinition def : removeStationButtons.values()) { 
                GuiUtils.blit(GUI_WIDGETS, pPoseStack, def.getX(), def.getY(), 232, 0, 16, 16); // delete button
            }

            int i = 0;
            for (Entry<String, StationInfo> entry : names.entrySet()) {
                MutableComponent name = Utils.text(entry.getKey());
                int maxTextWidth = 104 - 12;  
                if (shadowlessFont.width(name) > maxTextWidth) {
                    name = Utils.text(shadowlessFont.substrByWidth(name, maxTextWidth).getString()).append(Constants.ELLIPSIS_STRING);
                }
                drawString(pPoseStack, shadowlessFont, name, x + 30, y + 26 + (i * STATION_ENTRY_HEIGHT) + 6, 0xFFFFFF);

                StationInfo info = entry.getValue();
                MutableComponent platform = Utils.text(info.platform());
                int maxPlatformWidth = 35 - 7;  
                if (shadowlessFont.width(platform) > maxPlatformWidth) {
                    platform = Utils.text(shadowlessFont.substrByWidth(platform, maxPlatformWidth - 3).getString()).append(Constants.ELLIPSIS_STRING);
                }                
                int platformTextWidth = shadowlessFont.width(platform);
                drawString(pPoseStack, shadowlessFont, platform, x + 30 + 130 - platformTextWidth, y + 26 + (i * STATION_ENTRY_HEIGHT) + 6, 0xFFFFFF);
                i++;
            }
            
        } else {
            MutableComponent name = Utils.text(alias.getAliasName().get());
            int maxTextWidth = 129;  
            if (shadowlessFont.width(name) > maxTextWidth) {
                name = Utils.text(shadowlessFont.substrByWidth(name, maxTextWidth).getString()).append(Constants.ELLIPSIS_STRING);
            }
            drawString(pPoseStack, shadowlessFont, name, x + 30, y + 10, 0xFFFFFF);

            pPoseStack.scale(0.75f, 0.75f, 0.75f);
            drawString(pPoseStack, shadowlessFont, Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.summary", alias.getAllStationNames().size()), (int)((x + 5) / 0.75f), (int)((y + 30) / 0.75f), 0xDBDBDB);
            if (alias.getLastEditorName() != null && !alias.getLastEditorName().isBlank()) {
                drawString(pPoseStack, shadowlessFont, Utils.translate("gui." + ModMain.MOD_ID + ".alias_settings.editor", alias.getLastEditorName(), alias.getLastEditedTimeFormatted()), (int)((x + 5) / 0.75f), (int)((y + 38) / 0.75f), 0xDBDBDB);
            }
            float s = 1 / 0.75f;
            pPoseStack.scale(s, s, s);
        }

        controls.performForEach(x -> x.visible, x -> x.render(pPoseStack, pMouseX, pMouseY, pPartialTick));  

        // Button highlight
        if (deleteButton.isInBounds(pMouseX, pMouseY)) {
            fill(pPoseStack, deleteButton.getX(), deleteButton.getY(), deleteButton.getRight(), deleteButton.getBottom(), 0x1AFFFFFF);
        } else if (expandButton.isInBounds(pMouseX, pMouseY)) {
            fill(pPoseStack, expandButton.getX(), expandButton.getY(), expandButton.getRight(), expandButton.getBottom(), 0x1AFFFFFF);
        } else if (expanded && addButton.isInBounds(pMouseX, pMouseY)) {
            fill(pPoseStack, addButton.getX(), addButton.getY(), addButton.getRight(), addButton.getBottom(), 0x1AFFFFFF);
        } else if (expanded && removeStationButtons.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY))) {
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY)) {
                    fill(pPoseStack, entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getRight(), entry.getValue().getBottom(), 0x1AFFFFFF);
                }
            }
        } else if (expanded && stationInfoAreas.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY))) {
            for (Entry<String, GuiAreaDefinition> entry : stationInfoAreas.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY)) {
                    fill(pPoseStack, entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getRight(), entry.getValue().getBottom(), 0x1AFFFFFF);
                }
            }
        }
    }
    
    @Override
    public void renderForeground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {        
        GuiUtils.renderTooltipWithScrollOffset(parent, newEntryBox, List.of(tooltipStationName), width / 2, matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        GuiUtils.renderTooltipWithScrollOffset(parent, newEntryPlatformBox, List.of(tooltipPlatform), width / 2, matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        GuiUtils.renderTooltipWithScrollOffset(parent, deleteButton, List.of(tooltipDeleteAlias), width / 2, matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        GuiUtils.renderTooltipWithScrollOffset(parent, expandButton, List.of(expanded ? Constants.TOOLTIP_COLLAPSE : Constants.TOOLTIP_EXPAND), width / 2, matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        if (expanded) {
            GuiUtils.renderTooltipWithScrollOffset(parent, addButton, List.of(tooltipAddStation), width / 2, matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));        
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (GuiUtils.renderTooltipWithScrollOffset(parent, entry.getValue(), List.of(tooltipDeleteStation), width / 2, matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks))) {
                    break;
                }
            }

            for (Entry<String, GuiAreaDefinition> entry : stationNameAreas.entrySet()) {
                if (shadowlessFont.width(entry.getKey()) > 104 - 12 && ModGuiUtils.renderTooltipAtFixedPos(parent, entry.getValue(), List.of(Utils.text(entry.getKey())), width, matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks), entry.getValue().getLeft() + 1, entry.getValue().getTop() - parent.getScrollOffset(partialTicks))) {
                    break;
                }
            }

            for (Entry<String, GuiAreaDefinition> entry : stationInfoAreas.entrySet()) {
                MutableComponent text = Utils.text(alias.getInfoForStation(entry.getKey()).platform());
                if ((selectedStationName == null || !entry.getKey().equals(selectedStationName)) && shadowlessFont.width(text) > 35 - 7 && ModGuiUtils.renderTooltipAtFixedPos(parent, entry.getValue(), List.of(text), width, matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks), entry.getValue().getLeft(), entry.getValue().getTop() - parent.getScrollOffset(partialTicks))) {
                    break;
                }
            }
        } else {
            if (titleBarArea.isInBounds(mouseX, mouseY + parent.getScrollOffset(partialTicks)) && shadowlessFont.width(alias.getAliasName().get()) > 129) {
                ModGuiUtils.renderTooltipAtFixedPos(parent, titleBarArea, List.of(Utils.text(alias.getAliasName().get())), width, matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks), titleBarArea.getLeft() + 1, titleBarArea.getTop() - parent.getScrollOffset(partialTicks));
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
        parent.unfocusAllEntries();

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

        if (controls.components.stream().filter(x -> x.visible).anyMatch(x -> x.mouseClicked(pMouseX, pMouseY, pButton))) {
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (destinationSuggestions != null && destinationSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers))
			return true;

            for (AbstractWidget w : controls.components) {
                if (w.keyPressed(pKeyCode, pScanCode, pModifiers)) {
                    return true;
                }
            }
        
        return false;
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        for (AbstractWidget w : controls.components) {
            if (w.charTyped(pCodePoint, pModifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolledLoop(double pMouseX, double pMouseY, double pDelta) {
        if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(pMouseX, pMouseY, Mth.clamp(pDelta, -1.0D, 1.0D)))
			return true;

        return false;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    private void clearSuggestions() {
        if (destinationSuggestions != null) {
            destinationSuggestions.getEditBox().setSuggestion("");
        }
        destinationSuggestions = null;
    }


    protected void updateEditorSubwidgets(EditBox field) {
        clearSuggestions();

		destinationSuggestions = new ModStationSuggestions(minecraft, parent, field, minecraft.font, getViableStations(field), field.getHeight() + 2 + field.y);
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
	}

    private List<String> getViableStations(EditBox field) {
        return ClientTrainStationSnapshot.getInstance().getAllTrainStations().stream()
            .distinct()
            .filter(x -> !GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(x) && !alias.contains(x))
            .sorted((a, b) -> a.compareTo(b))
            .toList();
	}
}
