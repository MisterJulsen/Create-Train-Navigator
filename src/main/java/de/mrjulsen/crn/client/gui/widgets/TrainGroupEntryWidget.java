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
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.ControlCollection;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.client.gui.ITickableWidget;
import de.mrjulsen.crn.client.gui.screen.TrainGroupScreen;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainStationAlias.StationInfo;
import de.mrjulsen.crn.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class TrainGroupEntryWidget extends Button implements ITickableWidget, IForegroundRendering {

    private static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings_widgets.png");
    public static final int WIDTH = 200;
    public static final int HEIGHT = 48;
    private static final int STATION_ENTRY_HEIGHT = 20;
        
    private final TrainGroupScreen parent;
    private final Font shadowlessFont;
    private final Minecraft minecraft;

    private final Runnable onUpdate;

    // Data
    private TrainGroup trainGroup;
    private boolean expanded = false;

    // Controls
    private final ModEditBox titleBox;
    private final ModEditBox newEntryBox;
    private final ModEditBox newEntryPlatformBox;
    private final ControlCollection controls = new ControlCollection();
    private final Map<String, GuiAreaDefinition> removeStationButtons = new HashMap<>();

    private GuiAreaDefinition deleteButton;
    private GuiAreaDefinition expandButton;
    private GuiAreaDefinition addButton;

	private ModStationSuggestions destinationSuggestions;

    // Tooltips
    private final TranslatableComponent tooltipDeleteAlias = new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_group_settings.delete_alias.tooltip");
    private final TranslatableComponent tooltipDeleteStation = new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_group_settings.delete_station.tooltip");
    private final TranslatableComponent tooltipAddStation = new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_group_settings.add_station.tooltip");
    

    public TrainGroupEntryWidget(TrainGroupScreen parent, int pX, int pY, TrainGroup trainGroup, Runnable onUpdate, boolean expanded) {
        super(pX, pY, 200, 48, new TextComponent(trainGroup.getGroupName()), (btn) -> {});
        
        Minecraft minecraft = Minecraft.getInstance();
        shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        this.minecraft = Minecraft.getInstance();
        this.parent = parent;
        this.trainGroup = trainGroup;
        this.expanded = expanded;
        this.onUpdate = onUpdate;

        titleBox = new ModEditBox(minecraft.font, pX + 30, pY + 10, 129, 12, new TextComponent(""));
		titleBox.setBordered(false);
		titleBox.setMaxLength(25);
		titleBox.setTextColor(0xFFFFFF);
        titleBox.setValue(trainGroup.getGroupName());
        titleBox.setOnFocusChanged((box, focused) -> {
            if (!focused) {
                if (!setGroupName(box.getValue())) {
                    titleBox.setValue(trainGroup.getGroupName());
                }
            }
        });
        titleBox.visible = expanded;
        controls.components.add(titleBox);

        
        newEntryBox = new ModEditBox(minecraft.font, pX + 30, pY + 30, 95, 12, new TextComponent(""));
		newEntryBox.setBordered(false);
		newEntryBox.setMaxLength(25);
		newEntryBox.setTextColor(0xFFFFFF);
        newEntryBox.visible = expanded;
        newEntryBox.setResponder(x -> {
            updateEditorSubwidgets(newEntryBox);
        });
        controls.components.add(newEntryBox);

        newEntryPlatformBox = new ModEditBox(minecraft.font, pX + 134, pY + 30, 25, 12, new TextComponent(""));
		newEntryPlatformBox.setBordered(false);
		newEntryPlatformBox.setMaxLength(25);
		newEntryPlatformBox.setTextColor(0xFFFFFF);
        newEntryPlatformBox.visible = expanded;
        newEntryPlatformBox.setResponder(x -> {
            //updateEditorSubwidgets(newEntryBox);
        });
        controls.components.add(newEntryPlatformBox);

        setY(pY);
    }    

    public TrainGroup getTrainGroup() {
        return trainGroup;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setY(int y) {
        this.y = y;
        deleteButton = new GuiAreaDefinition(x + 165, y + 6, 16, 16);
        expandButton = new GuiAreaDefinition(x + 182, y + 6, 16, 16);
        addButton = new GuiAreaDefinition(x + 165, y + 26 + (trainGroup.getTrainNames().size() * STATION_ENTRY_HEIGHT) + 2, 16, 16);
        titleBox.y = y + 10;
        initStationDeleteButtons();
    }

    private void initStationDeleteButtons() {
        removeStationButtons.clear();
        
        String[] names = trainGroup.getTrainNames().toArray(String[]::new);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            removeStationButtons.put(name, new GuiAreaDefinition(x + 165, y + 26 + (i * STATION_ENTRY_HEIGHT) + 2, 16, 16));
        }
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
        GlobalSettingsManager.getInstance().getSettingsData().unregisterTrainGroup(trainGroup, onUpdate);
    }

    private void addTrain(String name) {
        String prevName = trainGroup.getGroupName();
        if (ClientTrainStationSnapshot.getInstance().getAllTrainNames().stream().noneMatch(x -> x.equals(name))) {
            return;
        }

        // TODO
        trainGroup.add(name);
        trainGroup.updateLastEdited(minecraft.player.getName().getString());
        GlobalSettingsManager.getInstance().getSettingsData().updateTrainGroup(prevName, trainGroup, () -> {
            onUpdate.run();
            initStationDeleteButtons();
        });
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
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) { 
        RenderSystem.setShaderTexture(0, GUI_WIDGETS);
        blit(pPoseStack, x, y, 0, 0, WIDTH, HEIGHT);

        blit(pPoseStack, deleteButton.getX(), deleteButton.getY(), 232, 0, 16, 16); // delete button
        blit(pPoseStack, expandButton.getX(), expandButton.getY(), expanded ? 216 : 200, 0, 16, 16); // expand button  

        if (expanded) {
            Set<String> names = trainGroup.getTrainNames();
            blit(pPoseStack, x + 25, y + 5, 0, 92, 139, 18); // textbox
            newEntryBox.y = y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 6;
            newEntryPlatformBox.y = y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 6;

            for (int i = 0; i < names.size(); i++) {
                blit(pPoseStack, x, y + 26 + (i * STATION_ENTRY_HEIGHT), 0, 48, 200, STATION_ENTRY_HEIGHT);
            }
            
            blit(pPoseStack, x, y + 26 + (names.size() * STATION_ENTRY_HEIGHT), 0, 68, 200, 24);
            blit(pPoseStack, x + 25, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 0, 92, 103, 18); // textbox
            blit(pPoseStack, x + 25 + 102, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 138, 92, 1, 18); // textbox

            blit(pPoseStack, x + 129, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 0, 92, 35, 18); // textbox
            blit(pPoseStack, x + 129 + 34, y + 26 + (names.size() * STATION_ENTRY_HEIGHT) + 1, 138, 92, 1, 18); // textbox
            blit(pPoseStack, addButton.getX(), addButton.getY(), 200, 16, 16, 16); // add button 

            for (GuiAreaDefinition def : removeStationButtons.values()) { 
                blit(pPoseStack, def.getX(), def.getY(), 232, 0, 16, 16); // delete button
            }

            int i = 0;
            for (String entry : names) {
                drawString(pPoseStack, shadowlessFont, entry, x + 30, y + 26 + (i * STATION_ENTRY_HEIGHT) + 6, 0xFFFFFF);
                i++;
            }
            
        } else {
            drawString(pPoseStack, shadowlessFont, trainGroup.getGroupName(), x + 30, y + 10, 0xFFFFFF);
            pPoseStack.scale(0.75f, 0.75f, 0.75f);
            drawString(pPoseStack, shadowlessFont, new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_group_settings.summary", trainGroup.getTrainNames().size()), (int)((x + 5) / 0.75f), (int)((y + 30) / 0.75f), 0xDBDBDB);
            if (trainGroup.getLastEditorName() != null && !trainGroup.getLastEditorName().isBlank()) {
                drawString(pPoseStack, shadowlessFont, new TranslatableComponent("gui." + ModMain.MOD_ID + ".train_group_settings.editor", trainGroup.getLastEditorName(), trainGroup.getLastEditedTimeFormatted()), (int)((x + 5) / 0.75f), (int)((y + 38) / 0.75f), 0xDBDBDB);
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
        }
    }
    
    @Override
    public void renderForeground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		if (destinationSuggestions != null) {
			matrixStack.pushPose();
			matrixStack.translate(0, -parent.getScrollOffset(partialTicks), 500);
			destinationSuggestions.render(matrixStack, mouseX, mouseY + parent.getScrollOffset(partialTicks));
			matrixStack.popPose();
		}
        
        GuiUtils.renderTooltip(parent, deleteButton, List.of(tooltipDeleteAlias.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        GuiUtils.renderTooltip(parent, expandButton, List.of(expanded ? Constants.TOOLTIP_COLLAPSE.getVisualOrderText() : Constants.TOOLTIP_EXPAND.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));
        if (expanded) {
            GuiUtils.renderTooltip(parent, addButton, List.of(tooltipAddStation.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks));        
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (GuiUtils.renderTooltip(parent, entry.getValue(), List.of(tooltipDeleteStation.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, parent.getScrollOffset(partialTicks))) {
                    break;
                }
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (destinationSuggestions != null && destinationSuggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
            return super.mouseClicked(pMouseX, pMouseY, pButton);

        if (deleteButton.isInBounds(pMouseX, pMouseY)) {
            deleteAlias();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expandButton.isInBounds(pMouseX, pMouseY)) {
            toggleExpanded();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expanded && addButton.isInBounds(pMouseX, pMouseY)) {
            addTrain(newEntryBox.getValue());
            newEntryBox.setValue("");
            newEntryBox.setFocused(false);
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expanded && removeStationButtons.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY))) {
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY)) {
                    removeTrain(entry.getKey());
                    return super.mouseClicked(pMouseX, pMouseY, pButton);
                }
            }
        }

        controls.performForEach(x -> x.visible, x -> x.mouseClicked(pMouseX, pMouseY, pButton));
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (destinationSuggestions != null && destinationSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers))
			return true;

        controls.performForEach(x -> x.visible, x -> x.keyPressed(pKeyCode, pScanCode, pModifiers));
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        controls.performForEach(x -> x.visible, x -> x.charTyped(pCodePoint, pModifiers));
        return super.charTyped(pCodePoint, pModifiers);
    }  
    
    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(pMouseX, pMouseY, Mth.clamp(pDelta, -1.0D, 1.0D)))
			return true;

        return false;
    }

    private void clearSuggestions() {
        if (destinationSuggestions != null) {
            destinationSuggestions.getEditBox().setSuggestion("");
        }
        destinationSuggestions = null;
    }


    protected void updateEditorSubwidgets(EditBox field) {
        clearSuggestions();

		destinationSuggestions = new ModStationSuggestions(minecraft, parent, field, minecraft.font, getViableTrains(field), field.getHeight() + 2 + field.y);
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
	}

    private List<String> getViableTrains(EditBox field) {
        return ClientTrainStationSnapshot.getInstance().getAllTrainNames().stream()
            .distinct()
            .filter(x -> !GlobalSettingsManager.getInstance().getSettingsData().isTrainBlacklisted(x) && !trainGroup.contains(x))
            .sorted((a, b) -> a.compareTo(b))
            .toList();
	}

}
