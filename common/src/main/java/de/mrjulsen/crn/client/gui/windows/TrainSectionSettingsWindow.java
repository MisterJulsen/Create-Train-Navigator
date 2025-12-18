package de.mrjulsen.crn.client.gui.windows;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.AllIcons;
import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.IconSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.ModularWidgetContainer;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateItemPicker;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.schedule.instruction.TravelSectionInstruction;
import de.mrjulsen.crn.data.storage.GlobalSettingsClient;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCycleButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLToggleButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class TrainSectionSettingsWindow extends DLWindow {

	private static final ItemStack DISPLAY_ITEM = new ItemStack(AllItems.SCHEDULE.get());
    private static final int GUI_WIDTH = 212;
    
    private static final FooterSize headerSize = FooterSize.DEFAULT;
    private static final FooterSize footerSize = FooterSize.SMALL;

    private final CompoundTag nbt;

    // Settings
    private boolean includePreviousStation = false;
    private boolean usable = true;
    private UUID trainCategoryId;
    private UUID trainLineId;

    private Map<UUID, TrainCategory> categoriesById;
    private Map<UUID, TrainLine> linesById;

    // GUI
    private ModularWidgetContainer commonSettingsContainer;

    private final MutableComponent title = TextUtils.translate("gui.createrailwaysnavigator.section_settings.title");
    private final MutableComponent tooltipGlobalSettings = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.global_settings.tooltip");
    private final MutableComponent tooltipTrainCatrgory = TextUtils.translate("gui.createrailwaysnavigator.section_settings.train_categories");
    private final MutableComponent tooltipTrainLine = TextUtils.translate("gui.createrailwaysnavigator.section_settings.train_lines");
    private final MutableComponent textIncludePreviousStation = TextUtils.translate("gui.createrailwaysnavigator.section_settings.include_previous_station");
    private final MutableComponent textUsable = TextUtils.translate("gui.createrailwaysnavigator.section_settings.usable");
    private final MutableComponent textNone = TextUtils.translate("gui.createrailwaysnavigator.section_settings.none");

    @SuppressWarnings("deprecation")
    public TrainSectionSettingsWindow(DLWindowManager manager, CompoundTag nbt) {
        super(manager);
        this.nbt = nbt;
        setWidth(GUI_WIDTH);
        windowSpawnPosition.set(WindowPosition.PARENT_CENTER);

        this.includePreviousStation = nbt.contains(TravelSectionInstruction.NBT_INCLUDE_PREVIOUS_STATION) ? nbt.getBoolean(TravelSectionInstruction.NBT_INCLUDE_PREVIOUS_STATION) : false;
        this.usable = nbt.contains(TravelSectionInstruction.NBT_USABLE) ? nbt.getBoolean(TravelSectionInstruction.NBT_USABLE) : true;
        
        if (nbt.contains(TravelSectionInstruction.LEGACY_NBT_TRAIN_CATEGORY)) {
            this.trainCategoryId = nbt.getTagType(TravelSectionInstruction.LEGACY_NBT_TRAIN_CATEGORY) == Tag.TAG_STRING ? TrainCategory.genMD5Uuid(nbt.getString(TravelSectionInstruction.LEGACY_NBT_TRAIN_CATEGORY)) : nbt.getUUID(TravelSectionInstruction.LEGACY_NBT_TRAIN_CATEGORY);
        } else if (nbt.contains(TravelSectionInstruction.NBT_TRAIN_CATEGORY)) {
            this.trainCategoryId = nbt.getTagType(TravelSectionInstruction.NBT_TRAIN_CATEGORY) == Tag.TAG_STRING ? TrainCategory.genMD5Uuid(nbt.getString(TravelSectionInstruction.NBT_TRAIN_CATEGORY)) : nbt.getUUID(TravelSectionInstruction.NBT_TRAIN_CATEGORY);
        }
        
        if (nbt.contains(TravelSectionInstruction.NBT_TRAIN_LINE)) {
            this.trainLineId = nbt.getTagType(TravelSectionInstruction.NBT_TRAIN_LINE) == Tag.TAG_STRING ? TrainLine.genMD5Uuid(nbt.getString(TravelSectionInstruction.NBT_TRAIN_LINE)) : nbt.getUUID(TravelSectionInstruction.NBT_TRAIN_LINE);
        }


        
        CreateButton backButton = addComponent(new CreateButton(width() - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT, AllIcons.I_CONFIRM));
        backButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });
        
        CreateButton helpButton = addComponent(new CreateButton(width() - 17 - CreateButton.WIDTH * 2, height() - 6 - CreateButton.HEIGHT, ModGuiIcons.HELP.getAsCreateIcon()));
        helpButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            Util.getPlatform().openUri(Constants.HELP_PAGE_SCHEDULE_SECTIONS);
            return false;
        });
        helpButton.tooltip.set(new DLTooltip(List.of(Constants.TEXT_HELP), 200));
        
        CreateButton globalSettingsButton = addComponent(new CreateButton(7, 119, ModGuiIcons.SETTINGS.getAsCreateIcon()));
        globalSettingsButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new GlobalSettingsWindow(mgr));
            return false;
        });
        globalSettingsButton.tooltip.set(new DLTooltip(List.of(tooltipGlobalSettings), 200));


        commonSettingsContainer = addComponent(new ModularWidgetContainer(3, headerSize.size() + 1, width() - 6, 100));
        int minHeight = headerSize.size() + footerSize.size() + 2;

        initGui();
        
        setHeight(minHeight);
        commonSettingsContainer.addEventListener(ModularWidgetContainer.ContentLayoutUpdatedEvent.class, (s, e) -> {
            commonSettingsContainer.setHeight(e.layoutResult().contentHeight());
            setHeight(minHeight + e.layoutResult().contentHeight());
            backButton.setPosition(width() - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT);
            backButton.setPosition(width() - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT);
            globalSettingsButton.setPosition(7, height() - 6 - CreateButton.HEIGHT);
            setY(getWindowManager().getScreenHeight() / 2 - height() / 2);
            return false;
        });
    }

    @Override
    public void close() {
        if (trainCategoryId != null) {
            nbt.putUUID(TravelSectionInstruction.NBT_TRAIN_CATEGORY, trainCategoryId);
        } else {
            nbt.remove(TravelSectionInstruction.NBT_TRAIN_CATEGORY);
        }
        if (trainLineId != null) {
            nbt.putUUID(TravelSectionInstruction.NBT_TRAIN_LINE, trainLineId);
        } else {
            nbt.remove(TravelSectionInstruction.NBT_TRAIN_LINE);
        }
        nbt.putBoolean(TravelSectionInstruction.NBT_INCLUDE_PREVIOUS_STATION, includePreviousStation);
        nbt.putBoolean(TravelSectionInstruction.NBT_USABLE, usable);
    }

    protected void initGui() {
        commonSettingsContainer.clearLines();

        GlobalSettingsClient.getTrainCategories((trainCategories) -> {
            List<TrainCategory> orderedCategories = trainCategories.stream().sorted((a, b) -> a.getCategoryName().compareToIgnoreCase(b.getCategoryName())).toList();
            this.categoriesById = orderedCategories.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));

            GlobalSettingsClient.getTrainLines((trainLines) -> {
                List<TrainLine> orderedLines = trainLines.stream().sorted((a, b) -> a.getLineName().compareToIgnoreCase(b.getLineName())).toList();
                this.linesById = orderedLines.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));
                

                DLPanel lineTrainCategory = commonSettingsContainer.addLine("category");
                IconSlotWidget trainCategoryIcon = lineTrainCategory.addComponent(new IconSlotWidget(0, 0));
                trainCategoryIcon.icon.set(ModGuiIcons.TRAIN.getAsSprite(16, 16));

                /*
                CreateItemPicker<TrainCategory> trainCategoryPicker = lineTrainCategory.addComponent(new CreateItemPicker<>(0, 0, 150));
                trainCategoryPicker.title.set(tooltipTrainCatrgory);
                trainCategoryPicker.formatter.set(item -> item == null ? textNone : TextUtils.text(item.getCategoryName()));
                trainCategoryPicker.items.addAll(orderedCategories);
                trainCategoryPicker.selectedItem.set(Optional.ofNullable(categoriesById.get(trainCategoryId)));
                trainCategoryPicker.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
                    trainCategoryPicker.selectedItem.get().ifPresent(i -> trainCategoryId = i.getId());
                    return false;
                });
                */
                CreateItemPicker<String> trainCategoryPicker = lineTrainCategory.addComponent(new CreateItemPicker<>(0, 0, 150));
                trainCategoryPicker.title.set(tooltipTrainCatrgory);
                trainCategoryPicker.formatter.set(item -> item == null ? textNone : TextUtils.text(item));
                trainCategoryPicker.items.add(textNone.getString());
                trainCategoryPicker.items.addAll(orderedCategories.stream().map(x -> x.getCategoryName()).toList());
                trainCategoryPicker.selectedIndex.set(trainCategoryId != null && categoriesById.containsKey(trainCategoryId) ? orderedCategories.indexOf(categoriesById.get(trainCategoryId)) + 1 : 0);
                trainCategoryPicker.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
                    int idx = trainCategoryPicker.selectedIndex.get() - 1;
                    trainCategoryId = idx >= 0 ? orderedCategories.get(idx).getId() : null;
                    return false;
                });

                DLPanel lineTrainLine = commonSettingsContainer.addLine("line");
                IconSlotWidget trainLineIcon = lineTrainLine.addComponent(new IconSlotWidget(0, 0));
                trainLineIcon.icon.set(ModGuiIcons.MAP_PATH.getAsSprite(16, 16));

                CreateItemPicker<String> trainLinePicker = lineTrainLine.addComponent(new CreateItemPicker<>(0, 0, 150));
                trainLinePicker.title.set(tooltipTrainLine);
                trainLinePicker.formatter.set(item -> item == null ? textNone : TextUtils.text(item));
                trainLinePicker.items.add(textNone.getString());
                trainLinePicker.items.addAll(orderedLines.stream().map(x -> x.getLineName()).toList());
                trainLinePicker.selectedIndex.set(trainLineId != null && linesById.containsKey(trainLineId) ? orderedLines.indexOf(linesById.get(trainLineId)) + 1 : 0);
                trainLinePicker.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
                    int idx = trainLinePicker.selectedIndex.get() - 1;
                    trainLineId = idx >= 0 ? orderedLines.get(idx).getId() : null;
                    return false;
                });

                DLPanel lineInclude = commonSettingsContainer.addLine("include");
                DLCheckBox includePreviousStationBox = lineInclude.addComponent(new DLCheckBox(0, 0, 165, CreateButton.HEIGHT));
                includePreviousStationBox.text.set(textIncludePreviousStation);
                includePreviousStationBox.checked.set(includePreviousStation);
                includePreviousStationBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
                    this.includePreviousStation = e.checked();
                    return false;
                });

                DLPanel lineUsable = commonSettingsContainer.addLine("usable");
                DLCheckBox usableBox = lineUsable.addComponent(new DLCheckBox(0, 0, 165, CreateButton.HEIGHT));
                usableBox.text.set(textUsable);
                usableBox.checked.set(usable);
                usableBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
                    this.usable = e.checked();
                    return false;
                });
            });
        });

    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderWindow(graphics, 0, 0, width(), height(), ContainerColor.PURPLE, BarColor.GOLD, BarColor.GRAY, headerSize.size(), footerSize.size(), true);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, width() - 31, height() - footerSize.size() + 2, footerSize.size() - 4, BarColor.GRAY);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, 4, title, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);


        GuiGameElement.of(DISPLAY_ITEM).<GuiGameElement
			.GuiRenderBuilder>at(width(), height() - 48, -200)
			.scale(4f)
			.render(graphics.graphics());
    }
}
