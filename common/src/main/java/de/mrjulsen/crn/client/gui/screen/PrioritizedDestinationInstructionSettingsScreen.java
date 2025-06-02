package de.mrjulsen.crn.client.gui.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.lang.FontHelper;
import org.apache.commons.lang3.StringUtils;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.item.TooltipHelper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIndicator;
import de.mrjulsen.crn.client.gui.widgets.DLCreateTextBox;
import de.mrjulsen.crn.client.gui.widgets.DLNewListBox;
import de.mrjulsen.crn.client.gui.widgets.ModStationSuggestions;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.ButtonState;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.WidgetsCollection;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class PrioritizedDestinationInstructionSettingsScreen extends DLScreen {

    private static final MutableComponent TITLE = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings");
	private static final ItemStack DISPLAY_ITEM = new ItemStack(AllItems.SCHEDULE.get());
    private static final int GUI_WIDTH = 250;
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;
    private static final FooterSize headerSize = FooterSize.DEFAULT;
    private static final FooterSize footerSize = FooterSize.EXTENDED;
    private static final int GUI_HEIGHT = 200;

    private int guiLeft, guiTop;
    private GuiAreaDefinition workingArea;

    private DLCreateIconButton backButton;
	private ModStationSuggestions destinationSuggestions;
    private DLNewListBox<String, DestinationEntry> listBox;
    private DLCreateIconButton addBtn;
    private DLCreateTextBox addTextBox;

    
    private DLCreateIconButton avoidSignalsButton;
    private DLCreateIndicator avoidSignalsIndicator;
    private DLCreateIconButton avoidTrainsButton;
    private DLCreateIndicator avoidTrainsIndicator;
    private final Map<IconButton, Pair<Component, Component>> toggleButtonTooltips = new LinkedHashMap<>();
    private final WidgetsCollection toggleButtons = new WidgetsCollection();

    private final Screen lastScreen;
    private final PrioritizedDestinationInstruction instruction;
    private final CompoundTag nbt;
    private final List<String> stationFilters = new ArrayList<>();
    private boolean shouldAvoidSignals;
    private boolean shouldAvoidTrains;

    private final List<String> stationNames = new ArrayList<>();

    private final MutableComponent txtAvoidSignalsTitle = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.avoid_signals");
    private final MutableComponent txtAvoidSignalsDescription = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.avoid_signals_description").withStyle(ChatFormatting.GRAY);
    private final MutableComponent txtAvoidTrainsTitle = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.avoid_trains");
    private final MutableComponent txtAvoidTrainsDescription = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.avoid_trains_description").withStyle(ChatFormatting.GRAY);
    
    public PrioritizedDestinationInstructionSettingsScreen(Screen lastScreen, PrioritizedDestinationInstruction instruction, CompoundTag nbt) {
        super(TITLE);
        this.instruction = instruction;
        this.lastScreen = lastScreen;
        this.nbt = nbt;

        this.stationFilters.addAll(nbt.getList(PrioritizedDestinationInstruction.NBT_FILTERS, Tag.TAG_STRING).stream().map(x -> x.getAsString()).toList());
        this.shouldAvoidSignals = nbt.contains(PrioritizedDestinationInstruction.NBT_AVOID_RED_SIGNAL) ? nbt.getBoolean(PrioritizedDestinationInstruction.NBT_AVOID_RED_SIGNAL) : true;
        this.shouldAvoidTrains = nbt.contains(PrioritizedDestinationInstruction.NBT_AVOID_TRAINS) ? nbt.getBoolean(PrioritizedDestinationInstruction.NBT_AVOID_TRAINS) : true;
    }

    @Override
    public void onClose() {
        ListTag list = new ListTag();
        Iterator<String> i = stationFilters.stream().limit(PrioritizedDestinationInstruction.MAX_ENTRIES).iterator();
        while (i.hasNext()) {
            list.add(StringTag.valueOf(i.next()));
        }
        nbt.put(PrioritizedDestinationInstruction.NBT_FILTERS, list);
        nbt.putBoolean(PrioritizedDestinationInstruction.NBT_AVOID_RED_SIGNAL, shouldAvoidSignals);
        nbt.putBoolean(PrioritizedDestinationInstruction.NBT_AVOID_TRAINS, shouldAvoidTrains);
        super.onClose();
        Minecraft.getInstance().setScreen(lastScreen);
    }

    @Override
    public void tick() {
        super.tick();

        DLUtils.doIfNotNull(destinationSuggestions, x -> {            
            x.tick();
            if (!destinationSuggestions.getEditBox().canConsumeInput()) {
                clearSuggestions();
            }
        });
        
        toggleButtons.performForEachOfType(IconButton.class, x -> {
            if (!toggleButtonTooltips.containsKey(x)) {
                return;
            }

            x.setToolTip(toggleButtonTooltips.get(x).getFirst());
            x.getToolTip().add(TooltipHelper.holdShift(FontHelper.Palette.YELLOW, hasShiftDown()));

            if (hasShiftDown()) {
                x.getToolTip().add(toggleButtonTooltips.get(x).getSecond());
            }
        });
    }

    @Override
    protected void init() {
        super.init();
        toggleButtons.clear();
        toggleButtonTooltips.clear();
        
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_STATION_NAMES, (names) -> {
            this.stationNames.clear();
            this.stationNames.addAll(names);
        });

        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;
        workingArea = new GuiAreaDefinition(guiLeft + 3, guiTop + headerSize.size() + 1, GUI_WIDTH - 6, GUI_HEIGHT - headerSize.size() - footerSize.size() - 2);

        // Content
        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(this, workingArea.getRight() - 5, workingArea.getY(), workingArea.getHeight(), GuiAreaDefinition.empty());

        listBox = addRenderableWidget(new DLNewListBox<>(this, workingArea.getX() + 33, workingArea.getY() + 40, 178, workingArea.getHeight() - 40, scrollBar));
        addTextBox = addRenderableWidget(new DLCreateTextBox(font, workingArea.getX() + 63, workingArea.getY() + 9, 118, TextUtils.empty()));
        addTextBox.setResponder(b -> {
            updateEditorSubwidgets(addTextBox);
            addBtn.set_active(canAddMore() && addTextBox.getValue() != null && !addTextBox.getValue().isBlank());
        });        
		addTextBox.setFilter(s -> StringUtils.countMatches(s, '*') <= 3);
        addTooltip(DLTooltip.of(instruction.getSecondLineTooltip(0).stream().map(x -> (FormattedText)x).toList()).assignedTo(addTextBox));
        
        addBtn = addRenderableWidget(new DLCreateIconButton(workingArea.getX() + 185, workingArea.getY() + 9, ModGuiIcons.ADD.getAsCreateIcon())
            .withCallback(() -> {
                this.stationFilters.add(addTextBox.getValue());
                addTextBox.setValue("");
                reloadList();
            })
        );
        addTooltip(DLTooltip.of(Constants.TEXT_ADD).assignedTo(addBtn));
        addRenderableWidget(scrollBar);
        
        // Buttons
        avoidSignalsButton = addRenderableWidget(new DLCreateIconButton(guiLeft + 7, guiTop + GUI_HEIGHT - 6 - DLIconButton.DEFAULT_BUTTON_HEIGHT, GuiGameElement.of(AllBlocks.TRACK_SIGNAL.asStack())));
        avoidSignalsIndicator = this.addRenderableWidget(new DLCreateIndicator(avoidSignalsButton.x(), avoidSignalsButton.y() - 6, TextUtils.empty()));
        avoidSignalsIndicator.state = this.shouldAvoidSignals ? State.ON : State.OFF;
        avoidSignalsButton.withCallback(() -> {
            this.shouldAvoidSignals = !this.shouldAvoidSignals;
            this.avoidSignalsIndicator.state = this.shouldAvoidSignals ? State.ON : State.OFF; 
        });
        toggleButtons.add(avoidSignalsButton);
        toggleButtonTooltips.put(avoidSignalsButton, Pair.of(txtAvoidSignalsTitle, txtAvoidSignalsDescription));

        avoidTrainsButton = addRenderableWidget(new DLCreateIconButton(guiLeft + 7 + DLIconButton.DEFAULT_BUTTON_WIDTH, guiTop + GUI_HEIGHT - 6 - DLIconButton.DEFAULT_BUTTON_HEIGHT, GuiGameElement.of(AllBlocks.TRAIN_CONTROLS.asStack())));
        avoidTrainsIndicator = this.addRenderableWidget(new DLCreateIndicator(avoidTrainsButton.x(), avoidTrainsButton.y() - 6, TextUtils.empty()));
        avoidTrainsIndicator.state = this.shouldAvoidTrains ? State.ON : State.OFF;
        avoidTrainsButton.withCallback(() -> {
            this.shouldAvoidTrains = !this.shouldAvoidTrains;
            this.avoidTrainsIndicator.state = this.shouldAvoidTrains ? State.ON : State.OFF; 
        });
        toggleButtons.add(avoidTrainsButton);
        toggleButtonTooltips.put(avoidTrainsButton, Pair.of(txtAvoidTrainsTitle, txtAvoidTrainsDescription));

        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - 7 - DEFAULT_ICON_BUTTON_WIDTH, guiTop + GUI_HEIGHT - 6 - DEFAULT_ICON_BUTTON_HEIGHT, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
        backButton.withCallback(() -> {
            onClose();
        });
        
        DLCreateIconButton helpButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + GUI_WIDTH - 17 - DEFAULT_ICON_BUTTON_WIDTH * 2, guiTop + GUI_HEIGHT - 6 - DEFAULT_ICON_BUTTON_HEIGHT, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                Util.getPlatform().openUri(Constants.HELP_PAGE_PRIORITIZED_DESTINATION_INSTRUCTION);
            }
        });
        addTooltip(DLTooltip.of(Constants.TEXT_HELP).assignedTo(helpButton));

        reloadList();
    }

    private void reloadList() {
        listBox.displayData(stationFilters, (list, idx, data) -> new DestinationEntry(listBox, idx, data,
            (btn) -> {
                this.stationFilters.remove(btn.getIndex());
                reloadList();
            })
        );
        
        addBtn.set_active(canAddMore() && addTextBox.getValue() != null && !addTextBox.getValue().isBlank());
        addTextBox.set_active(canAddMore());
        addTextBox.setEditable(canAddMore());
    }

    private boolean canAddMore() {
        return stationFilters.size() < PrioritizedDestinationInstruction.MAX_ENTRIES;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBlurredBackground(pPartialTick);
        this.renderMenuBackground(graphics.graphics());
        CreateDynamicWidgets.renderWindow(graphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, ContainerColor.PURPLE, BarColor.GOLD, BarColor.GRAY, headerSize.size(), footerSize.size(), true);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, guiLeft + GUI_WIDTH - 31, guiTop + GUI_HEIGHT - footerSize.size() + 2, footerSize.size() - 4, BarColor.GRAY);

        CreateDynamicWidgets.renderWidgetInner(graphics, workingArea.getX() + 31, workingArea.getY(), 182, workingArea.getHeight(), ColorShade.DARK);
        DynamicGuiRenderer.renderArea(graphics, new GuiAreaDefinition(workingArea.getX() + 41, workingArea.getY() + 9, 18, 18), AreaStyle.GRAY, ButtonState.DOWN);
        GuiUtils.drawString(graphics, font, workingArea.getX() + 63, workingArea.getY() + 28, String.format("%s / %s", stationFilters.size(), PrioritizedDestinationInstruction.MAX_ENTRIES), DragonLib.NATIVE_BUTTON_FONT_COLOR_DISABLED, EAlignment.LEFT, false);

        GuiUtils.drawString(graphics, font, guiLeft + 6, guiTop + 4, TITLE, DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.LEFT, false);
        
        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);

        GuiGameElement.of(AllBlocks.TRACK_STATION.asStack()).at(workingArea.getX() + 42, workingArea.getY() + 10).render(graphics.graphics());
        
        GuiUtils.drawTexture(CRNGui.GUI, graphics, guiLeft + GUI_WIDTH - 3, guiTop + GUI_HEIGHT - 24, 11, 18, 0, 12, CRNGui.GUI_WIDTH, CRNGui.GUI_HEIGHT);
        GuiGameElement.of(DISPLAY_ITEM).<GuiGameElement
			.GuiRenderBuilder>at(guiLeft + GUI_WIDTH + 11, guiTop + GUI_HEIGHT - 48, -200)
			.scale(4f)
			.render(graphics.graphics());
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTick);

        toggleButtons.performForEach(widget -> {
            if (widget instanceof AbstractSimiWidget simiWidget && simiWidget instanceof IDragonLibWidget dlw && dlw.isMouseSelected()) {
				List<Component> tooltip = simiWidget.getToolTip();
				if (tooltip.isEmpty())
					return;
				int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
				int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
				graphics.graphics().renderComponentTooltip(font, tooltip, ttx, tty);
			}
        });
                
        DLUtils.doIfNotNull(destinationSuggestions, x -> {   
            x.render(graphics.graphics(), mouseX, mouseY);
        });
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(mouseX, mouseY, Mth.clamp(scrollY, -1.0D, 1.0D)))
			return true;

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public void updateEditorSubwidgets(EditBox field) {
        clearSuggestions();
        
        destinationSuggestions = new ModStationSuggestions(Minecraft.getInstance(), this, field, font, getViableStations(stationNames, field), field.getHeight() + 2 + field.getY());
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
	}

    private List<String> getViableStations(Collection<String> src, EditBox field) {
        return src.stream()
            .distinct()
            .sorted((a, b) -> a.compareTo(b))
            .toList();
	}

    private void clearSuggestions() {
        if (destinationSuggestions != null) {
            destinationSuggestions.getEditBox().setSuggestion("");
        }
        destinationSuggestions = null;
    }


    private static class DestinationEntry extends DLNewListBox.Entry<String, DestinationEntry> {

        private final MutableComponent txtDragNDrop = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.drag_and_drop");
        private final MutableComponent txtPriorities = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.priorities").withStyle(ChatFormatting.GRAY);
        private final Function<Integer, MutableComponent> txtPriority = (i) -> TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.priority_pos", i).withStyle(ChatFormatting.DARK_GRAY);
        private final DLIconButton deleteBtn;

        protected DestinationEntry(DLNewListBox<String, DestinationEntry> list, int index, String data, Consumer<DestinationEntry> onDelete) {
            super(list, index, data, 20);
            deleteBtn = addRenderableWidget(new DLIconButton(
                ButtonType.DEFAULT,
                AreaStyle.FLAT,
                ModGuiIcons.DELETE.getAsSprite(16, 16),
                x() + list.width() - 26,
                y() + 1,
                TextUtils.empty(),
                btn -> onDelete.accept(this)
            ));
            deleteBtn.setBackColor(0);
        }
        
        @Override
        public void renderItem(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.renderItem(graphics, mouseX, mouseY, partialTicks);
            CreateDynamicWidgets.renderTextSlotOverlay(graphics, x + 30, y + 1, 118, 18);
            CreateDynamicWidgets.renderGrabber(graphics, x + 8, y + 2);
            GuiUtils.drawString(graphics, font, x + 30 + 5, y + 6, getData(), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
        }
        
        @Override
        public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);
            if (!getList().getParent().isDragging()) {
                GuiUtils.renderTooltip(getList().getParent(), deleteBtn, List.of(Constants.TEXT_REMOVE), 200, graphics, mouseX, mouseY);
                if (isMouseSelected() && mouseX > x() + 4 && mouseX < x() + 20) {
                    GuiUtils.renderTooltip(getList().getParent(), GuiAreaDefinition.of(getList().getParent()), List.of(txtDragNDrop, txtPriorities, txtPriority.apply(getIndex() + 1)), 200, graphics, mouseX, mouseY);
                }
            }
        }
    }
}