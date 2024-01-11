package de.mrjulsen.crn.client.gui.widgets;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.ControlCollection;
import de.mrjulsen.crn.client.gui.GuiAreaDefinition;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.client.gui.ITickableWidget;
import de.mrjulsen.crn.client.gui.screen.AliasSettingsScreen;
import de.mrjulsen.crn.data.AliasName;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.data.TrainStationAlias.StationInfo;
import de.mrjulsen.crn.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AliasEntryWidget extends Button implements ITickableWidget, IForegroundRendering {

    private static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ModMain.MOD_ID, "textures/gui/settings_widgets.png");
    public static final int WIDTH = 200;
    public static final int HEIGHT = 48;
    private static final int STATION_ENTRY_HEIGHT = 20;
        
    private final AliasSettingsScreen parent;
    private final Font shadowlessFont;
    private final Minecraft minecraft;

    private final Runnable onUpdate;

    // Data
    private TrainStationAlias alias;
    private boolean expanded = false;

    // Controls
    private final ModEditBox titleBox;
    private final ModEditBox newEntryBox;
    private final ControlCollection controls = new ControlCollection();
    private final Map<String, GuiAreaDefinition> removeStationButtons = new HashMap<>();

    private GuiAreaDefinition deleteButton;
    private GuiAreaDefinition expandButton;
    private GuiAreaDefinition addButton;

	private ModStationSuggestions destinationSuggestions;

    // Tooltips
    private final TranslatableComponent tooltipDeleteAlias = new TranslatableComponent("gui." + ModMain.MOD_ID + ".alias_settings.delete_alias.tooltip");
    private final TranslatableComponent tooltipDeleteStation = new TranslatableComponent("gui." + ModMain.MOD_ID + ".alias_settings.delete_station.tooltip");
    private final TranslatableComponent tooltipAddStation = new TranslatableComponent("gui." + ModMain.MOD_ID + ".alias_settings.add_station.tooltip");
    

    public AliasEntryWidget(AliasSettingsScreen parent, int pX, int pY, TrainStationAlias alias, Runnable onUpdate, boolean expanded) {
        super(pX, pY, 200, 48, new TextComponent(alias.getAliasName().get()), (btn) -> {});
        
        Minecraft minecraft = Minecraft.getInstance();
        shadowlessFont = new NoShadowFontWrapper(minecraft.font);

        this.minecraft = Minecraft.getInstance();
        this.parent = parent;
        this.alias = alias;
        this.expanded = expanded;
        this.onUpdate = onUpdate;

        titleBox = new ModEditBox(minecraft.font, pX + 30, pY + 10, 129, 12, new TextComponent(""));
		titleBox.setBordered(false);
		titleBox.setMaxLength(25);
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

        
        newEntryBox = new ModEditBox(minecraft.font, pX + 30, pY + 30, 129, 12, new TextComponent(""));
		newEntryBox.setBordered(false);
		newEntryBox.setMaxLength(25);
		newEntryBox.setTextColor(0xFFFFFF);
        newEntryBox.visible = expanded;
        newEntryBox.setResponder(x -> {
            updateEditorSubwidgets(newEntryBox);
        });
        controls.components.add(newEntryBox);        

        setY(pY);
    }    

    public TrainStationAlias getAlias() {
        return alias;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setY(int y) {
        this.y = y;
        deleteButton = new GuiAreaDefinition(x + 165, y + 6, 16, 16);
        expandButton = new GuiAreaDefinition(x + 182, y + 6, 16, 16);
        addButton = new GuiAreaDefinition(x + 165, y + 26 + (alias.getAllStationNames().size() * STATION_ENTRY_HEIGHT) + 2, 16, 16);
        titleBox.y = y + 10;
        initStationDeleteButtons();
    }

    private void initStationDeleteButtons() {
        removeStationButtons.clear();
        
        String[] names = alias.getAllStationNames().toArray(String[]::new);
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
    }

    private void deleteAlias() {
        GlobalSettingsManager.getInstance().getSettingsData().unregisterAlias(alias, onUpdate);
    }

    private void addStation(String name) {
        AliasName prevName = alias.getAliasName();
        if (ClientTrainStationSnapshot.getInstance().getAllTrainStations().stream().noneMatch(x -> x.equals(name))) {
            return;
        }

        // TODO
        alias.add(name, StationInfo.empty());
        alias.updateLastEdited(minecraft.player.getName().getString());
        GlobalSettingsManager.getInstance().getSettingsData().updateAlias(prevName, alias, () -> {
            onUpdate.run();
            initStationDeleteButtons();
        });
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
    public int getHeight() {
        return height;
    }

    public int calcHeight() {
        if (expanded) {            
            height = STATION_ENTRY_HEIGHT *  alias.getAllStationNames().size() + 50;
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
            String[] names = alias.getAllStationNames().toArray(String[]::new);
            blit(pPoseStack, x + 25, y + 5, 0, 92, 139, 18); // textbox
            newEntryBox.y = y + 26 + (names.length * STATION_ENTRY_HEIGHT) + 6;

            for (int i = 0; i < names.length; i++) {
                blit(pPoseStack, x, y + 26 + (i * STATION_ENTRY_HEIGHT), 0, 48, 200, STATION_ENTRY_HEIGHT);
            }
            
            blit(pPoseStack, x, y + 26 + (names.length * STATION_ENTRY_HEIGHT), 0, 68, 200, 24);
            blit(pPoseStack, x + 25, y + 26 + (names.length * STATION_ENTRY_HEIGHT) + 1, 0, 92, 139, 18); // textbox
            blit(pPoseStack, addButton.getX(), addButton.getY(), 200, 16, 16, 16); // add button 

            for (GuiAreaDefinition def : removeStationButtons.values()) { 
                blit(pPoseStack, def.getX(), def.getY(), 232, 0, 16, 16); // delete button
            }

            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                drawString(pPoseStack, shadowlessFont, name, x + 30, y + 26 + (i * STATION_ENTRY_HEIGHT) + 6, 0xFFFFFF);
            }
            
        } else {
            drawString(pPoseStack, shadowlessFont, alias.getAliasName().get(), x + 30, y + 10, 0xFFFFFF);
            pPoseStack.scale(0.75f, 0.75f, 0.75f);
            drawString(pPoseStack, shadowlessFont, new TranslatableComponent("gui." + ModMain.MOD_ID + ".alias_settings.summary", alias.getAllStationNames().size()), (int)((x + 5) / 0.75f), (int)((y + 30) / 0.75f), 0xDBDBDB);
            if (alias.getLastEditorName() != null && !alias.getLastEditorName().isBlank()) {
                drawString(pPoseStack, shadowlessFont, new TranslatableComponent("gui." + ModMain.MOD_ID + ".alias_settings.editor", alias.getLastEditorName(), alias.getLastEditedTimeFormatted()), (int)((x + 5) / 0.75f), (int)((y + 38) / 0.75f), 0xDBDBDB);
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
            addStation(newEntryBox.getValue());
            newEntryBox.setValue("");
            newEntryBox.setFocused(false);
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        } else if (expanded && removeStationButtons.values().stream().anyMatch(x -> x.isInBounds(pMouseX, pMouseY))) {
            for (Entry<String, GuiAreaDefinition> entry : removeStationButtons.entrySet()) {
                if (entry.getValue().isInBounds(pMouseX, pMouseY)) {
                    removeStation(entry.getKey());
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
