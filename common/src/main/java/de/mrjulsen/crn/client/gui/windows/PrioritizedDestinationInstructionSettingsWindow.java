package de.mrjulsen.crn.client.gui.windows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.autocomplete.StationsAutocomplete;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateIndicator;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextLabel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.Orientation;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.render.DefaultGuiTextures;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class PrioritizedDestinationInstructionSettingsWindow extends DLWindow {

    private static final MutableComponent TITLE = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings");
	private static final ItemStack DISPLAY_ITEM = new ItemStack(AllItems.SCHEDULE.get());
    private static final int GUI_WIDTH = 250;
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;
    private static final FooterSize headerSize = FooterSize.DEFAULT;
    private static final FooterSize footerSize = FooterSize.EXTENDED;
    private static final int GUI_HEIGHT = 200;

    private Rectangle workingArea;
    
    private DLScrollBar scrollBar;
    private ListBox list;
    private CreateButton backButton;
    private CreateButton addBtn;
    private CreateTextBox addTextBox;
    private CreateButton avoidSignalsButton;
    private CreateIndicator avoidSignalsIndicator;
    private CreateButton avoidTrainsButton;
    private CreateIndicator avoidTrainsIndicator;

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
    
    public PrioritizedDestinationInstructionSettingsWindow(DLWindowManager manager, PrioritizedDestinationInstruction instruction, CompoundTag nbt) {
        super(manager);
        setSize(GUI_WIDTH, GUI_HEIGHT);
        windowSpawnPosition.set(WindowPosition.CENTER);
        this.instruction = instruction;
        this.nbt = nbt;

        this.stationFilters.addAll(nbt.getList(PrioritizedDestinationInstruction.NBT_FILTERS, Tag.TAG_STRING).stream().map(x -> x.getAsString()).toList());
        this.shouldAvoidSignals = nbt.contains(PrioritizedDestinationInstruction.NBT_AVOID_RED_SIGNAL) ? nbt.getBoolean(PrioritizedDestinationInstruction.NBT_AVOID_RED_SIGNAL) : true;
        this.shouldAvoidTrains = nbt.contains(PrioritizedDestinationInstruction.NBT_AVOID_TRAINS) ? nbt.getBoolean(PrioritizedDestinationInstruction.NBT_AVOID_TRAINS) : true;

        init();
    }

    protected void init() {

        ModNetworkManager.GET_ALL_STATION_NAMES.send(NetworkDirection.toServer(), (response) -> {
            this.stationNames.clear();
            this.stationNames.addAll(response.getStations());
        }, () -> {});

        workingArea = Rectangle.withSize(3, headerSize.size() + 1, GUI_WIDTH - 6, GUI_HEIGHT - headerSize.size() - footerSize.size() - 2);

        // Content
        scrollBar = new DLScrollBar((int)workingArea.right() - 5, (int)workingArea.y(), 5, (int)workingArea.height(), Orientation.VERTICAL);
        scrollBar.componentRenderer.set(ModernScrollbarComponentRenderer.INSTANCE);
        addComponent(scrollBar);

        //listBox = addRenderableWidget(new DLNewListBox<>(this, workingArea.getX() + 33, workingArea.getY() + 40, 178, workingArea.getHeight() - 40, scrollBar));

        addTextBox = addComponent(new CreateTextBox((int)workingArea.x() + 63, (int)workingArea.y() + 9, 118));
        addTextBox.autocompleteManager.set(new StationsAutocomplete());
        addTextBox.addEventListener(DLRichTextLabel.TextChangedEvent.class, (s, e) -> {
            addBtn.enabled.set(canAddMore() && e.text().getPlainText() != null && !e.text().getPlainText().isBlank());
            return false;
        });
		//addTextBox.setFilter(s -> StringUtils.countMatches(s, '*') <= 3);
        addTextBox.tooltip.set(new DLTooltip(instruction.getSecondLineTooltip(0).stream().map(x -> (FormattedText)x).toList(), 200));
        
        addBtn = addComponent(new CreateButton((int)workingArea.x() + 185, (int)workingArea.y() + 9, ModGuiIcons.ADD.getAsCreateIcon()));
        addBtn.enabled.set(false);
        addBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            if (!canAddMore()) {
                return false;
            }
            this.stationFilters.add(addTextBox.text.get().getPlainText());
            addTextBox.text.get().set("");
            reloadList();
            return false;
        });
        addBtn.tooltip.set(new DLTooltip(List.of(), 200));

        list = addComponent(new ListBox(this, scrollBar, (int)workingArea.x() + 33, (int)workingArea.y() + 40, 178, (int)workingArea.height() - 40));
        
        scrollBar.anchor.set2(EAlign.TOP, EAlign.BOTTOM, EAlign.RIGHT);
        scrollBar.scrollerSize.set(0);
        scrollBar.screenSize.set(list.height());
        scrollBar.scrollSteps.set(15);
        scrollBar.max.set(0);
        scrollBar.inputConsumptionPolicy.set((type) -> true);
        scrollBar.addEventListener(DLScrollBar.ValueChangedEvent.class, (s, e) -> {
            list.setScrollOffsetY(e.value());
            return false;
        });
        addEventListener(DLGuiStandardEvents.ScrollEvent.class, scrollBar::invokeEvent);
        

        // Buttons
        avoidSignalsButton = addComponent(new CreateButton(7, GUI_HEIGHT - 6 - CreateButton.HEIGHT, GuiGameElement.of(AllBlocks.TRACK_SIGNAL.asStack())));
        avoidSignalsIndicator = addComponent(new CreateIndicator(avoidSignalsButton.x(), avoidSignalsButton.y() - 6));
        avoidSignalsIndicator.state.set(this.shouldAvoidSignals ? State.ON : State.OFF);
        avoidSignalsButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            shouldAvoidSignals = !shouldAvoidSignals;
            avoidSignalsIndicator.state.set(this.shouldAvoidSignals ? State.ON : State.OFF);
            return false;
        });
        avoidSignalsButton.tooltip.set(new DLTooltip(List.of(txtAvoidSignalsTitle, txtAvoidSignalsDescription), 200));

        avoidTrainsButton = addComponent(new CreateButton(7 + CreateButton.WIDTH, GUI_HEIGHT - 6 - CreateButton.HEIGHT, GuiGameElement.of(AllBlocks.TRAIN_CONTROLS.asStack())));
        avoidTrainsIndicator = addComponent(new CreateIndicator(avoidTrainsButton.x(), avoidTrainsButton.y() - 6));
        avoidTrainsIndicator.state.set(this.shouldAvoidTrains ? State.ON : State.OFF);
        avoidTrainsButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            shouldAvoidTrains = !shouldAvoidTrains;
            avoidTrainsIndicator.state.set(this.shouldAvoidTrains ? State.ON : State.OFF);
            return false;
        });
        avoidTrainsButton.tooltip.set(new DLTooltip(List.of(txtAvoidTrainsTitle, txtAvoidTrainsDescription), 200));

        backButton = addComponent(new CreateButton(GUI_WIDTH - 7 - DEFAULT_ICON_BUTTON_WIDTH, GUI_HEIGHT - 6 - DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
        backButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });
        
        CreateButton helpButton = addComponent(new CreateButton(GUI_WIDTH - 17 - DEFAULT_ICON_BUTTON_WIDTH * 2, GUI_HEIGHT - 6 - DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()));
        helpButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Util.getPlatform().openUri(Constants.HELP_PAGE_PRIORITIZED_DESTINATION_INSTRUCTION);
            return false;
        });
        helpButton.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));

        reloadList();
    }
    

    @Override
    public void close() {
        ListTag list = new ListTag();
        Iterator<String> i = stationFilters.stream().limit(PrioritizedDestinationInstruction.MAX_ENTRIES).iterator();
        while (i.hasNext()) {
            list.add(StringTag.valueOf(i.next()));
        }
        nbt.put(PrioritizedDestinationInstruction.NBT_FILTERS, list);
        nbt.putBoolean(PrioritizedDestinationInstruction.NBT_AVOID_RED_SIGNAL, shouldAvoidSignals);
        nbt.putBoolean(PrioritizedDestinationInstruction.NBT_AVOID_TRAINS, shouldAvoidTrains);
    }

    private void reloadList() {
        list.reloadList();
    }

    private boolean canAddMore() {
        return stationFilters.size() < PrioritizedDestinationInstruction.MAX_ENTRIES;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderWindow(graphics, 0, 0, GUI_WIDTH, GUI_HEIGHT, ContainerColor.PURPLE, BarColor.GOLD, BarColor.GRAY, headerSize.size(), footerSize.size(), true);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, GUI_WIDTH - 31, GUI_HEIGHT - footerSize.size() + 2, footerSize.size() - 4, BarColor.GRAY);

        CreateDynamicWidgets.renderWidgetInner(graphics, (int)workingArea.x() + 31, (int)workingArea.y(), 182, (int)workingArea.height(), ColorShade.DARK);
        DefaultGuiTextures.DRAGONLIB_UI.getSprite("slot").render(graphics, (int)workingArea.x() + 41, (int)workingArea.y() + 9, 18, 18);
        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)workingArea.x() + 63, (int)workingArea.y() + 28, String.format("%s / %s", stationFilters.size(), PrioritizedDestinationInstruction.MAX_ENTRIES), DragonLib.VANILLA_BUTTON_DISABLED_FONT_COLOR, ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, 4, TITLE, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);
                
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        GuiGameElement.of(AllBlocks.TRACK_STATION.asStack()).at((int)workingArea.x() + 42, (int)workingArea.y() + 10).render(graphics.graphics());
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        GuiUtils.drawTexture(CRNGui.GUI, graphics, GUI_WIDTH - 3, GUI_HEIGHT - 24, 11, 18, 0, 12);
        GuiGameElement.of(DISPLAY_ITEM).<GuiGameElement
			.GuiRenderBuilder>at(GUI_WIDTH + 11, GUI_HEIGHT - 48, -200)
			.scale(4f)
			.render(graphics.graphics());
    }



    private static class ListBox extends DLGuiComponent {

        private int index = -1;
        private Entry draggedEntry;

        private final DLScrollBar scrollBar;

        private final PrioritizedDestinationInstructionSettingsWindow win;


        public ListBox(PrioritizedDestinationInstructionSettingsWindow win, DLScrollBar scrollBar, int x, int y, int w, int h) {
            super(x, y, w, h);
            this.win = win;
            this.scrollBar = scrollBar;
            addEventListener(DLGuiStandardEvents.ScrollEvent.class, scrollBar::invokeEvent);

            addEventListener(DLGuiStandardEvents.DragComponentOverEvent.class, (s, e) -> {
                if (e.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !e.other().isEmpty() && e.other().get(0) instanceof Entry entry) {
                    draggedEntry = entry;
                    int newIndex = (int)((s.getLocalMouseY() + getScrollOffsetY()) / Entry.HEIGHT);
                    boolean b = index != newIndex;
                    index = MathUtils.clamp(newIndex, 0, win.stationFilters.size() - 1);
                    if (b) {
                        updateLayout(entry);
                    }
                } else {
                    draggedEntry = null;
                }
                return false;
            });            
        }

        @Override
        public void tick() {
            if (draggedEntry != null && getWindowManager().isMouseDragging()) {
                if (getLocalMouseY() < 10) {
                    scrollBar.value.set(scrollBar.value.get() + 0.2f * (getLocalMouseY() - 10));
                } else if (getLocalMouseY() > height() - 10) {
                    scrollBar.value.set(scrollBar.value.get() + 0.2f * (getLocalMouseY() - height() + 10));
                }
            }
        }

        public void applyReorder() {
            index = MathUtils.clamp(index, 0, win.stationFilters.size() - 1);
            win.stationFilters.remove(draggedEntry.stationName);
            win.stationFilters.add(index, draggedEntry.stationName);
            index = -1;
            win.reloadList();
        }

        public void reloadList() {
            clearComponents();
            for (int i = 0; i < win.stationFilters.size(); i++) {                
                addComponent(new Entry(this, i, 0, 0, width(), win.stationFilters.get(i), (a) -> {                    
                    win.stationFilters.remove(a.index);
                    reloadList();
                }));
            }
            updateLayout(null);
            scrollBar.max.set(win.stationFilters.size() * Entry.HEIGHT);
        }



        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            if (index >= 0) {
                GuiUtils.drawBox(graphics, 0, (int)(index * Entry.HEIGHT - getScrollOffsetY()), width(), Entry.HEIGHT, DLColor.TRANSPARENT, DLColor.WHITE);
            }
        }
        

        public void updateLayout(DLGuiComponent draggingComponent) {
            int y = 0;
            int i = 0;
            for (DLGuiComponent component : getComponents()) {
                if (component == draggingComponent) {
                    continue;
                }
                if (i == index) {
                    y += Entry.HEIGHT;
                }
                component.setPosition(0, y);
                component.setWidth(width());
                y += component.height();
                i++;
            }
        }
        
    }



    private static class Entry extends DLGuiComponent {
        
        static final int HEIGHT = FlatIconButton.HEIGHT + 2;

        private final ListBox listBox;

        private final String stationName;
        private final Component txtDragNDrop = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.drag_and_drop");
        private final Component txtPriorities = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.priorities").withStyle(ChatFormatting.GRAY);
        private final Function<Integer, Component> txtPriority = (i) -> TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction.prioritized_destination_instruction.settings.priority_pos", i).withStyle(ChatFormatting.DARK_GRAY);
        private final FlatIconButton deleteBtn;

        private final Rectangle tooltipRectangle = Rectangle.withSize(4, 0, HEIGHT, HEIGHT);
        private final int index;

        private double mouseDownX;
        private double mouseDownY;

        public Entry(ListBox listBox, int idx, int x, int y, int w, String stationName, Consumer<Entry> onDelete) {
            super(x, y, w, HEIGHT);
            this.listBox = listBox;
            this.stationName = stationName;
            this.index = idx;

            cursor.set(CursorType.ALLRESIZE);

            deleteBtn = addComponent(new FlatIconButton(w - 26, 1, ModGuiIcons.DELETE.getAsCreateIcon()));
            deleteBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                onDelete.accept(this);
                return false;
            });

            addEventListener(DLGuiStandardEvents.MouseDownEvent.class, (s, e) -> {
                this.mouseDownX = e.mouseX();
                this.mouseDownY = e.mouseY();
                return false;
            });            
            addEventListener(DLGuiStandardEvents.DragEndEvent.class, (s, e) -> {
                listBox.applyReorder();
                return false;
            });

            addEventListener(DLGuiStandardEvents.RenderOnScreenEvent.class, (src, e) -> {
                return false;
            });

            inputConsumptionPolicy.set(c -> c != ConsumptionType.DRAG && c != ConsumptionType.SCROLL);
        }

        public int getIndex() {
            return index;
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            if (isDragged()) return;
            CreateDynamicWidgets.renderTextSlotOverlay(graphics, 30, 1, 118, HEIGHT - 2);
            CreateDynamicWidgets.renderGrabber(graphics, 8, 2);
            GuiUtils.drawString(graphics, graphics.defaultFont(), 30 + 5, 6, stationName, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        }

        @Override
        public void renderFrontLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            if (isDragged()) {                
                RenderSystem.enableBlend();
                graphics.poseStack().pushPose();
                graphics.poseStack().translate(mouseX - mouseDownX, mouseY - mouseDownY - listBox.getScrollOffsetY(), 0);
                CreateDynamicWidgets.renderShadow(graphics, -2, -2, width() + 4, height() + 4);
                CreateDynamicWidgets.renderSingleShadeWidget(graphics, -2, -2, width() + 4, height() + 4, ColorShade.DARK);
                
                CreateDynamicWidgets.renderTextSlotOverlay(graphics, 30, 1, 118, HEIGHT - 2);
                CreateDynamicWidgets.renderGrabber(graphics, 8, 2);
                GuiUtils.drawString(graphics, graphics.defaultFont(), 30 + 5, 6, stationName, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);

                graphics.poseStack().popPose();
            }
        }

        @Override
        public void renderOnScreen(DLGuiGraphics graphics, double mouseX, double mouseY) {
            if (isSelected()) {
                Point pos = toScreenCoordinates();
                if (tooltipRectangle.collision(mouseX - pos.x(), mouseY - pos.y())) {
                    GuiUtils.drawTooltip(graphics, graphics.defaultFont(), (int)mouseX, (int)mouseY, List.of(txtDragNDrop, txtPriorities, txtPriority.apply(getIndex() + 1)), 200);
                }
            }
        }
        
    }
}