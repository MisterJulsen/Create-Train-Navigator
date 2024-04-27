package de.mrjulsen.crn.client.gui.widgets;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Map.Entry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.screen.AbstractEntryListSettingsScreen;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainGroup;
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

public class TrainGroupEntryWidget extends AbstractEntryListOptionWidget {

    private static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ExampleMod.MOD_ID, "textures/gui/settings_widgets.png");
    public static final int WIDTH = 200;
    public static final int HEIGHT = 48;
    private static final int STATION_ENTRY_HEIGHT = 20;
        
    private final AbstractEntryListSettingsScreen<TrainGroup, TrainGroupEntryWidget> parent;
    private final Font shadowlessFont;
    private final Minecraft minecraft;

    private final Runnable onUpdate;

    // Data
    private TrainGroup trainGroup;
    private boolean expanded = false;

    // Controls
    private final DLEditBox titleBox;
    private final DLEditBox newEntryBox;
    private final Map<String, GuiAreaDefinition> removeStationButtons = new HashMap<>();
    private final Map<String, GuiAreaDefinition> trainGroupNames = new HashMap<>();

    private GuiAreaDefinition titleBarArea;

    private GuiAreaDefinition deleteButton;
    private GuiAreaDefinition expandButton;
    private GuiAreaDefinition addButton;

	private ModStationSuggestions suggestions;

    // Tooltips
    private final MutableComponent tooltipDeleteAlias = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".train_group_settings.delete_alias.tooltip");
    private final MutableComponent tooltipDeleteStation = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".train_group_settings.delete_station.tooltip");
    private final MutableComponent tooltipAddStation = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".train_group_settings.add_station.tooltip");
    

    public TrainGroupEntryWidget(AbstractEntryListSettingsScreen<TrainGroup, TrainGroupEntryWidget> parent, int pX, int pY, TrainGroup trainGroup, Runnable onUpdate, boolean expanded) {
        super(pX, pY, 200, 48);
        
        Minecraft minecraft = Minecraft.getInstance();
        shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        this.minecraft = Minecraft.getInstance();
        this.parent = parent;
        this.trainGroup = trainGroup;
        this.expanded = expanded;
        this.onUpdate = onUpdate;

        titleBox = new DLEditBox(minecraft.font, pX + 30, pY + 10, 129, 12, TextUtils.empty());
		titleBox.setBordered(false);
		titleBox.setMaxLength(32);
		titleBox.setTextColor(0xFFFFFF);
        titleBox.setValue(trainGroup.getGroupName());
        titleBox.withOnFocusChanged((box, focused) -> {
            if (!focused) {
                if (!setGroupName(box.getValue())) {
                    titleBox.setValue(trainGroup.getGroupName());
                }
            }
        });
        titleBox.visible = expanded;
        addRenderableWidget(titleBox);

        
        newEntryBox = new DLEditBox(minecraft.font, pX + 30, pY + 30, 129, 12, TextUtils.empty());
		newEntryBox.setBordered(false);
		newEntryBox.setMaxLength(32);
		newEntryBox.setTextColor(0xFFFFFF);
        newEntryBox.visible = expanded;
        newEntryBox.setResponder(x -> {
            updateEditorSubwidgets(newEntryBox);
        });
        addRenderableWidget(newEntryBox);

        setYPos(pY);
    }    

    public TrainGroup getTrainGroup() {
        return trainGroup;
    }

    public boolean isExpanded() {
        return expanded;
    }

    private void initStationDeleteButtons() {
        removeStationButtons.clear();
        trainGroupNames.clear();
        
        String[] names = trainGroup.getTrainNames().toArray(String[]::new);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            removeStationButtons.put(name, new GuiAreaDefinition(x + 165, y + 26 + (i * STATION_ENTRY_HEIGHT) + 2, 16, 16));
            trainGroupNames.put(name, new GuiAreaDefinition(x + 25, y + 26 + (i * STATION_ENTRY_HEIGHT) + 2, 129, 16));
        }
        
        titleBarArea = new GuiAreaDefinition(x + 25, y + 6, 129, 16);
    }

    @Override
    public void tick() {
        super.tick();

        if (suggestions != null) {
            suggestions.tick();

            if (!newEntryBox.canConsumeInput()) {
                clearSuggestions();
            }
        }
    }

    private void toggleExpanded() {
        this.expanded = !expanded;
        titleBox.visible = expanded;
        newEntryBox.visible = expanded;
    }

    private void deleteTrainGroup() {
        GlobalSettingsManager.getInstance().getSettingsData().unregisterTrainGroup(trainGroup, onUpdate);
    }

    private void addTrain(String name) {
        String prevName = trainGroup.getGroupName();
        if (ClientTrainStationSnapshot.getInstance().getAllTrainNames().stream().noneMatch(x -> x.equals(name)) || newEntryBox.getValue().isBlank()) {
            return;
        }

        trainGroup.add(name);
        trainGroup.updateLastEdited(minecraft.player.getName().getString());
        GlobalSettingsManager.getInstance().getSettingsData().updateTrainGroup(prevName, trainGroup, () -> {
            onUpdate.run();
            initStationDeleteButtons();
        });        
        
        newEntryBox.setValue("");
        newEntryBox.setFocus(false);
    }

    private boolean setGroupName(String name) {
        String prevName = trainGroup.getGroupName();

        if (name == null || name.isBlank()) {
            return false;
        }

        if (GlobalSettingsManager.getInstance().getSettingsData().getTrainGroupsList().stream().anyMatch(x -> x.getGroupName().equals(name))) {
            return false;
        }

        trainGroup.setGroupName(name);
        trainGroup.updateLastEdited(minecraft.player.getName().getString());
        GlobalSettingsManager.getInstance().getSettingsData().updateTrainGroup(prevName, trainGroup, onUpdate);
        return true;
    }

    private void removeTrain(String name) {
        String prevName = trainGroup.getGroupName();
        trainGroup.remove(name);
        trainGroup.updateLastEdited(minecraft.player.getName().getString());
        GlobalSettingsManager.getInstance().getSettingsData().updateTrainGroup(prevName, trainGroup, () -> {
            onUpdate.run();
            initStationDeleteButtons();
        });
        initStationDeleteButtons();
        onUpdate.run();
    }

    @Override
    public int getHeight() {
        return height;
    }

    public int calcHeight() {
        if (expanded) {            
            height = STATION_ENTRY_HEIGHT * trainGroup.getTrainNames().size() + 50;
        } else {
            height = HEIGHT;
        }
        return height;
    }

    @Override
    public void setYPos(int y) {
        this.y = y;
        deleteButton = new GuiAreaDefinition(x + 165, y + 6, 16, 16);
        expandButton = new GuiAreaDefinition(x + 182, y + 6, 16, 16);
        addButton = new GuiAreaDefinition(x + 165, y + 26 + (trainGroup.getTrainNames().size() * STATION_ENTRY_HEIGHT) + 2, 16, 16);
        titleBox.y = y + 10;
        initStationDeleteButtons();
    }

    @Override
    public void renderSuggestions(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if (suggestions != null) {
			poseStack.pushPose();
			poseStack.translate(0, -parent.getScrollOffset(partialTicks), 500);
			suggestions.render(poseStack, mouseX, mouseY + parent.getScrollOffset(partialTicks));
			poseStack.popPose();
		}
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) { 
        RenderSystem.setShaderTexture(0, GUI_WIDGETS);
        GuiUtils.drawTexture(GUI_WIDGETS, graphics, x, y, 0, 0, WIDTH, HEIGHT);

        GuiUtils.drawTexture(GUI_WIDGETS, graphics, deleteButton.getX(), deleteButton.getY(), 232, 0, 16, 16); // delete button
        GuiUtils.drawTexture(GUI_WIDGETS, graphics, expandButton.getX(), expandButton.getY(), expanded ? 216 : 200, 0, 16, 16); // expand button  

        if (expanded) {
            Set<String> names = trainGroup.getTrainNames();
            GuiUtils.drawTexture(GUI_WIDGETS, graphics, x + 25, y + 5, 0, 92, 139, 18); // textbox
            newEntryBox.y = y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 6;
            
            for (int i = 0; i < names.size(); i++) {
                GuiUtils.drawTexture(GUI_WIDGETS, graphics, x, y + 26 + (i * STATION_ENTRY_HEIGHT), 0, 48, 200, STATION_ENTRY_HEIGHT);
            }
            
            GuiUtils.drawTexture(GUI_WIDGETS, graphics, x, y + 26 + (names.size() * STATION_ENTRY_HEIGHT), 0, 68, 200, 24);
            CreateDynamicWidgets.renderTextBox(graphics, x + 25, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 139);
            GuiUtils.drawTexture(GUI_WIDGETS, graphics, addButton.getX(), addButton.getY(), 200, 16, 16, 16); // add button 

            for (GuiAreaDefinition def : removeStationButtons.values()) { 
                GuiUtils.drawTexture(GUI_WIDGETS, graphics, def.getX(), def.getY(), 232, 0, 16, 16); // delete button
            }

            int i = 0;
            for (String entry : names) {
                GuiUtils.drawString(graphics, shadowlessFont, x + 30, y + 26 + (i * STATION_ENTRY_HEIGHT) + 6, TextUtils.text(entry), 0xFFFFFF, EAlignment.LEFT, false);
                i++;
            }
            
        } else {
            MutableComponent name = TextUtils.text(trainGroup.getGroupName());
            int maxTextWidth = 129;  
            if (shadowlessFont.width(name) > maxTextWidth) {
                name = TextUtils.text(shadowlessFont.substrByWidth(name, maxTextWidth).getString()).append(Constants.ELLIPSIS_STRING);
            }
            GuiUtils.drawString(graphics, shadowlessFont, x + 30, y + 10, name, 0xFFFFFF, EAlignment.LEFT, false);

            graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 5) / 0.75f), (int)((y + 30) / 0.75f), TextUtils.translate("gui." + ExampleMod.MOD_ID + ".train_group_settings.summary", trainGroup.getTrainNames().size()), 0xDBDBDB, EAlignment.LEFT, false);
            if (trainGroup.getLastEditorName() != null && !trainGroup.getLastEditorName().isBlank()) {
                GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 5) / 0.75f), (int)((y + 38) / 0.75f), TextUtils.translate("gui." + ExampleMod.MOD_ID + ".train_group_settings.editor", trainGroup.getLastEditorName(), trainGroup.getLastEditedTimeFormatted()), 0xDBDBDB, EAlignment.LEFT, false);
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
        }
    }
    
    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
		if (suggestions != null) {
			graphics.poseStack().pushPose();
			graphics.poseStack().translate(0, -parent.getScrollOffset(partialTicks), 500);
			suggestions.render(graphics.poseStack(), mouseX, mouseY + parent.getScrollOffset(partialTicks));
			graphics.poseStack().popPose();
		}
        
        GuiUtils.renderTooltipWithOffset(parent, deleteButton, List.of(tooltipDeleteAlias), width, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        GuiUtils.renderTooltipWithOffset(parent, expandButton, List.of(expanded ? Constants.TOOLTIP_COLLAPSE : Constants.TOOLTIP_EXPAND), width, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        if (expanded) {
            GuiUtils.renderTooltipWithOffset(parent, addButton, List.of(tooltipAddStation), width, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));        
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (GuiUtils.renderTooltipWithOffset(parent, entry.getValue(), List.of(tooltipDeleteStation), width, graphics, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks))) {
                    break;
                }
            }

            for (Entry<String, GuiAreaDefinition> entry : trainGroupNames.entrySet()) {
                if (shadowlessFont.width(entry.getKey()) > 129 && GuiUtils.renderTooltipAt(parent, entry.getValue(), List.of(TextUtils.text(entry.getKey())), width, graphics, entry.getValue().getLeft() + 1, entry.getValue().getTop() - parent.getScrollOffset(partialTicks), mouseX, mouseY, 0, parent.getScrollOffset(partialTicks))) {
                    break;
                }
            }
        } else {
            if (titleBarArea.isInBounds(mouseX, mouseY + parent.getScrollOffset(partialTicks)) && shadowlessFont.width(trainGroup.getGroupName()) > 129) {
                GuiUtils.renderTooltipAt(parent, titleBarArea, List.of(TextUtils.text(trainGroup.getGroupName())), width, graphics, titleBarArea.getLeft() + 1, titleBarArea.getTop() - parent.getScrollOffset(partialTicks), mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
            }
        }
    }

    @Override
    public boolean mouseClickedLoop(double pMouseX, double pMouseY, int pButton) {
        //parent.unfocusAllEntries();

        if (suggestions != null && suggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
            return super.mouseClicked(pMouseX, pMouseY, pButton);

        return false;
    }
    
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (suggestions != null && suggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
            return super.mouseClicked(pMouseX, pMouseY, pButton);

        if (deleteButton.isInBounds(pMouseX, pMouseY)) {
            deleteTrainGroup();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expandButton.isInBounds(pMouseX, pMouseY)) {
            toggleExpanded();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expanded && addButton.isInBounds(pMouseX, pMouseY)) {
            addTrain(newEntryBox.getValue());
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expanded && removeStationButtons.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY))) {
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY)) {
                    removeTrain(entry.getKey());
                    return super.mouseClicked(pMouseX, pMouseY, pButton);
                }
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseScrolledLoop(double pMouseX, double pMouseY, double pDelta) {
        if (suggestions != null && suggestions.mouseScrolled(pMouseX, pMouseY, MathUtils.clamp(pDelta, -1.0D, 1.0D)))
			return true;

        return false;
    }

    private void clearSuggestions() {
        if (suggestions != null) {
            suggestions.getEditBox().setSuggestion("");
        }
        suggestions = null;
    }


    protected void updateEditorSubwidgets(DLEditBox field) {
        clearSuggestions();

		suggestions = new ModStationSuggestions(minecraft, parent, field, minecraft.font, getViableTrains(field), field.getHeight() + 2 + field.y);
        suggestions.setAllowSuggestions(true);
        suggestions.updateCommandInfo();
	}

    private List<String> getViableTrains(DLEditBox field) {
        return ClientTrainStationSnapshot.getInstance().getAllTrainNames().stream()
            .distinct()
            .filter(x -> !GlobalSettingsManager.getInstance().getSettingsData().isTrainBlacklisted(x) && !trainGroup.contains(x))
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
