package de.mrjulsen.crn.block.display.properties.components;

import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.DepartureBoardDisplayTableSettings;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayFocusSettings;
import de.mrjulsen.crn.block.display.properties.StaticTextDisplaySettings;
import de.mrjulsen.crn.block.display.properties.components.ITextWidthSetting.TextScaleBounds;
import de.mrjulsen.crn.block.display.properties.components.ITrainTextSetting.ETrainTextComponents;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.ColorSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.DBNavigatorWidget;
import de.mrjulsen.crn.client.gui.widgets.IconSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateItemPicker;
import de.mrjulsen.crn.client.gui.widgets.create.CreateScrollNumberInput;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCycleButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLNumberPicker;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextLabel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLToggleButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.Holder.MutableHolder;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class GuiBuilderWrapper {

    static void buildColorGui(IColorSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(IColorSetting.GUI_LINE_COLORS_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16));

        ColorSlotWidget fontColor = new ColorSlotWidget(
            0, 0,
            setting.getFontColor(),
            ModUtils.getDyeColors(),
            false, false,
            (b) -> setting.setFontColor(b)
        );
        fontColor.tooltip.set(new DLTooltip(List.of(IColorSetting.textFontColor, IColorSetting.textClickToEdit), 200));
        line.addComponent(fontColor);
        
        ColorSlotWidget backColor = new ColorSlotWidget(
            0, 0,
            setting.getBackColor(),
            ModUtils.getDyeColors(),
            false, true,
            (b) -> setting.setBackColor(b)
        );
        backColor.tooltip.set(new DLTooltip(List.of(IColorSetting.textBackColor, IColorSetting.textClickToEdit), 200));
        line.addComponent(backColor);
    }

    static void buildCarriageIndexGui(ICarriageIndexSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(ICarriageIndexSetting.GUI_LINE_CARRIAGE_INDEX_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.CARRIAGE_NUMBER.getAsSprite(16, 16));

        CreateScrollNumberInput indexBox = new CreateScrollNumberInput(0, 0, 22);
        indexBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.carriage_index"));
        indexBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.carriage_index.description"));
        indexBox.min.set(0D);
        indexBox.max.set(99D);
        indexBox.shiftStep.set(5D);
        indexBox.value.set((double)setting.getCarriageIndex());
        indexBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setCarriageIndex((byte)e.value());
            return false;
        });
        line.addComponent(indexBox);

        DLCheckBox overwriteIndexBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        overwriteIndexBox.text.set(ICarriageIndexSetting.textOverwriteCarriageIndex);
        overwriteIndexBox.checked.set(setting.shouldOverwriteCarriageIndex());
        overwriteIndexBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        overwriteIndexBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setOverwriteCarriageIndex(e.checked());
            return false;
        });
        overwriteIndexBox.tooltip.set(new DLTooltip(List.of(ICarriageIndexSetting.textOverwriteCarriageIndexDescription), 200));
        line.addComponent(overwriteIndexBox);
    }

    static void buildBasicTextWidthGui(GuiBuilderContext context) {
        DLPanel line = context.container().addLine(ICustomTextWidthSetting.GUI_LINE_TEXT_SIZE_NAME);
        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.WIDTH.getAsSprite(16, 16));
    }

    static void buildPlatformWidthGui(IPlatformWidthSetting setting, GuiBuilderContext context, boolean allowAuto) {
        DLPanel line = context.container().addLine(IPlatformWidthSetting.GUI_LINE_TEXT_SIZE_NAME);

        CreateScrollNumberInput platformWidthBox = new CreateScrollNumberInput(0, 0, 32);
        platformWidthBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.platform_width"));
        platformWidthBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.platform_width.description"));
        platformWidthBox.min.set(allowAuto ? -1D : 0D);
        platformWidthBox.max.set(64D);
        platformWidthBox.shiftStep.set(4D);
        platformWidthBox.format.set(new SpecialUnitNumberFormat(0, "px", allowAuto, false, platformWidthBox.min.get(), platformWidthBox.max.get()));
        platformWidthBox.value.set((double)setting.getPlatformWidth());
        platformWidthBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setPlatformWidth((byte)e.value());
            return false;
        });
        line.addComponent(platformWidthBox);
    }

    /*
    static void buildShowArrivalGui(IShowArrivalSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(IShowArrivalSetting.GUI_LINE_SHOW_ARRIVAL_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.TARGET.getAsSprite(16, 16));

        DLCheckBox showArrivalsBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        showArrivalsBox.text.set(IShowArrivalSetting.textShowArrival);
        showArrivalsBox.checked.set(setting.showArrival());
        showArrivalsBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        showArrivalsBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setShowArrival(e.checked());
            return false;
        });
        showArrivalsBox.tooltip.set(new DLTooltip(List.of(IShowArrivalSetting.textShowArrivalDescription), 200));
        line.addComponent(showArrivalsBox);
    }

     */

    static void buildShowDoNotBoardTextGui(IShowDoNotBoardText setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(IShowDoNotBoardText.GUI_LINE_SHOW_DO_NOT_BOARD_TEXT_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.WALK.getAsSprite(16, 16));

        DLCheckBox showDoNotBoardBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        showDoNotBoardBox.text.set(IShowDoNotBoardText.textShowDoNotBoardText);
        showDoNotBoardBox.checked.set(setting.showDoNotBoardText());
        showDoNotBoardBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        showDoNotBoardBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setShowDoNotBoardText(e.checked());
            return false;
        });
        showDoNotBoardBox.tooltip.set(new DLTooltip(List.of(IShowDoNotBoardText.textShowDoNotBoardTextDescription), 200));
        line.addComponent(showDoNotBoardBox);
    }

    static void buildShowExitGui(IShowExitDirectionSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(IShowExitDirectionSetting.GUI_LINE_SHOW_ARRIVAL_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.EXIT.getAsSprite(16, 16));

        DLCheckBox showLineColorBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        showLineColorBox.text.set(IShowExitDirectionSetting.textShowExit);
        showLineColorBox.checked.set(setting.showExit());
        showLineColorBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        showLineColorBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setShowExit(e.checked());
            return false;
        });
        line.addComponent(showLineColorBox);
    }

    static void buildShowLineColorGui(IShowLineColorSetting setting, GuiBuilderContext context) {        
        DLPanel line = context.container().addLine(IShowLineColorSetting.GUI_LINE_SHOW_LINE_COLOR_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16));

        DLCheckBox showDoNotBoardBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        showDoNotBoardBox.text.set(IShowLineColorSetting.textShowLineColor);
        showDoNotBoardBox.checked.set(setting.showLineColor());
        showDoNotBoardBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        showDoNotBoardBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setShowLineColor(e.checked());
            return false;
        });
        showDoNotBoardBox.tooltip.set(new DLTooltip(List.of(IShowLineColorSetting.textShowLineColorDescription), 200));
        line.addComponent(showDoNotBoardBox);
    }

    static void buildShowConnectionGui(IShowNextConnections setting, GuiBuilderContext context) {            
        DLPanel line = context.container().addLine(IShowNextConnections.GUI_LINE_SHOW_CONNECTIONS_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.CONNECTIONS.getAsSprite(16, 16));

        DLCheckBox showConnectionsBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        showConnectionsBox.text.set(IShowNextConnections.textShowConnections);
        showConnectionsBox.checked.set(setting.showConnections());
        showConnectionsBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        showConnectionsBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setShowConnection(e.checked());
            return false;
        });
        line.addComponent(showConnectionsBox);
    }

    static void buildShowTimeAndDateGui(IShowTimeAndDateSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(IShowTimeAndDateSetting.GUI_LINE_SHOW_TIME_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.TIME.getAsSprite(16, 16));

        DLCheckBox showBuildTimeAndDateBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        showBuildTimeAndDateBox.text.set(IShowTimeAndDateSetting.textShowStats);
        showBuildTimeAndDateBox.checked.set(setting.showTimeAndDate());
        showBuildTimeAndDateBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        showBuildTimeAndDateBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setShowTimeAndDate(e.checked());
            return false;
        });
        line.addComponent(showBuildTimeAndDateBox);
    }

    static void buildShowStatsGui(IShowTrainStatsSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(IShowTrainStatsSetting.GUI_LINE_SHOW_ARRIVAL_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.TRAIN_INFO.getAsSprite(16, 16));

        DLCheckBox showBuildTimeAndDateBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        showBuildTimeAndDateBox.text.set(IShowTrainStatsSetting.textShowStats);
        showBuildTimeAndDateBox.checked.set(setting.showStats());
        showBuildTimeAndDateBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        showBuildTimeAndDateBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setShowStats(e.checked());
            return false;
        });
        line.addComponent(showBuildTimeAndDateBox);
    }

    static void buildShowTrainMultipleTimesGui(IShowTrainMultipleTimes setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(IShowTrainMultipleTimes.GUI_LINE_SHOW_TRAIN_MULTIPLE_TIMES_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.COPY.getAsSprite(16, 16));

        DLCheckBox showTrainMultipleTimesBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        showTrainMultipleTimesBox.text.set(IShowTrainMultipleTimes.textShowTrainMultipleTimes);
        showTrainMultipleTimesBox.checked.set(setting.showTrainMultipleTimes());
        showTrainMultipleTimesBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        showTrainMultipleTimesBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setShowTrainMultipleTimes(e.checked());
            return false;
        });
        showTrainMultipleTimesBox.tooltip.set(new DLTooltip(List.of(IShowTrainMultipleTimes.textShowTrainMultipleTimesDescription), 200));
        line.addComponent(showTrainMultipleTimesBox);
    }

    static void buildTimeDisplayGui(ITimeDisplaySetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(ITimeDisplaySetting.GUI_LINE_TIME_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.TIME.getAsSprite(16, 16));

        CreateItemPicker<ETimeDisplay> timeDisplayBox = new CreateItemPicker<>(0, 0, 40);
        timeDisplayBox.renderArrow.set(true);
        timeDisplayBox.title.set(TextUtils.translate("enum.createrailwaysnavigator.time_display"));
        timeDisplayBox.hint.set(TextUtils.translate("enum.createrailwaysnavigator.time_display.description"));
        timeDisplayBox.formatter.set(item -> item == null ? TextUtils.empty() : item.getValueTranslation());
        timeDisplayBox.items.addAll(ETimeDisplay.values());
        timeDisplayBox.selectedItem.set(Optional.ofNullable(setting.getTimeDisplay()));
        timeDisplayBox.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
            timeDisplayBox.selectedItem.get().ifPresent(item -> setting.setTimeDisplay(item));
            return false;
        });
        line.addComponent(timeDisplayBox);
    }

    static void buildTrainNameGui(ITrainNameWidthSetting setting, GuiBuilderContext context, boolean allowAuto, boolean allowMax) {
        DLPanel line = context.container().addLine(IPlatformWidthSetting.GUI_LINE_TEXT_SIZE_NAME);

        CreateScrollNumberInput trainNameWidthBox = new CreateScrollNumberInput(0, 0, 32);
        trainNameWidthBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.train_name_width"));
        trainNameWidthBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.train_name_width.description"));
        trainNameWidthBox.min.set((double)(ITrainNameWidthSetting.MIN_VALUE - (allowAuto ? 1 : 0)));
        trainNameWidthBox.max.set((double)(ITrainNameWidthSetting.MAX_VALUE + (allowMax ? 1 : 0)));
        trainNameWidthBox.shiftStep.set(5D);
        trainNameWidthBox.format.set(new SpecialUnitNumberFormat(0, "px", allowAuto, allowMax, trainNameWidthBox.min.get(), trainNameWidthBox.max.get()));
        trainNameWidthBox.value.set((double)setting.getTrainNameWidth());
        trainNameWidthBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setTrainNameWidth((byte)e.value());
            return false;
        });
        line.addComponent(trainNameWidthBox);
    }

    static void buildTrainTextGui(ITrainTextSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(ITrainTextSetting.GUI_LINE_SHOW_ARRIVAL_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.TEXT.getAsSprite(16, 16));

        CreateItemPicker<ETrainTextComponents> timeDisplayBox = new CreateItemPicker<>(0, 0, 0);
        timeDisplayBox.renderArrow.set(true);
        timeDisplayBox.title.set(TextUtils.translate("enum.createrailwaysnavigator.train_text_components"));
        timeDisplayBox.hint.set(TextUtils.translate("enum.createrailwaysnavigator.train_text_components.description"));
        timeDisplayBox.formatter.set(item -> item == null ? TextUtils.empty() : item.getValueTranslation());
        timeDisplayBox.items.addAll(ETrainTextComponents.values());
        timeDisplayBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        timeDisplayBox.selectedItem.set(Optional.ofNullable(setting.getTrainTextComponents()));
        timeDisplayBox.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
            timeDisplayBox.selectedItem.get().ifPresent(setting::setTrainTextComponents);
            return false;
        });
        line.addComponent(timeDisplayBox);
    }

    static void buildTrainStopTypeGui(ITrainStopTypeSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(ITrainStopTypeSetting.GUI_LINE_TRAIN_STOP_TYPE_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.EXIT.getAsSprite(16, 16));

        CreateItemPicker<ITrainStopTypeSetting.ETrainStopType> trainStopTypeBox = new CreateItemPicker<>(0, 0, 0);
        trainStopTypeBox.renderArrow.set(true);
        trainStopTypeBox.title.set(ITrainStopTypeSetting.ETrainStopType.ALL.getEnumTranslation());
        trainStopTypeBox.hint.set(setting.getTrainStopType().getValueDescriptionTranslation());
        trainStopTypeBox.formatter.set(item -> item == null ? TextUtils.empty() : item.getValueTranslation());
        trainStopTypeBox.items.addAll(ITrainStopTypeSetting.ETrainStopType.values());
        trainStopTypeBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        trainStopTypeBox.selectedItem.set(Optional.ofNullable(setting.getTrainStopType()));
        trainStopTypeBox.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
            trainStopTypeBox.selectedItem.get().ifPresent(setting::setTrainStopType);
            trainStopTypeBox.hint.set((Component)e.item().map(x -> ((ITrainStopTypeSetting.ETrainStopType)x).getValueDescriptionTranslation()).orElse(TextUtils.empty()));
            return false;
        });
        line.addComponent(trainStopTypeBox);
    }

    public static void buildPlatformDisplayFocusGui(PlatformDisplayFocusSettings setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(ICustomTextWidthSetting.GUI_LINE_TEXT_SIZE_NAME);

        CreateScrollNumberInput trainNameWidthBox = new CreateScrollNumberInput(0, 0, 32);
        trainNameWidthBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.train_name_width"));
        trainNameWidthBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.train_name_width.description"));
        trainNameWidthBox.min.set(-1D);
        trainNameWidthBox.max.set(99D);
        trainNameWidthBox.shiftStep.set(5D);
        trainNameWidthBox.format.set(new SpecialUnitNumberFormat(0, "px", true, false, trainNameWidthBox.min.get(), trainNameWidthBox.max.get()));
        trainNameWidthBox.value.set((double)setting.getTrainNameWidth());
        trainNameWidthBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setTrainNameWidth((byte)e.value());
            return false;
        });
        line.addComponent(trainNameWidthBox);
        
        CreateScrollNumberInput platformWidthBox = new CreateScrollNumberInput(0, 0, 32);
        platformWidthBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.platform_width_table"));
        platformWidthBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.platform_width.description"));
        platformWidthBox.min.set(-1D);
        platformWidthBox.max.set(64D);
        platformWidthBox.shiftStep.set(4D);
        platformWidthBox.format.set(new SpecialUnitNumberFormat(0, "px", true, false, platformWidthBox.min.get(), platformWidthBox.max.get()));
        platformWidthBox.value.set((double)setting.getPlatformWidth());
        platformWidthBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setPlatformWidth((byte)e.value());
            return false;
        });
        line.addComponent(platformWidthBox);
        
        CreateScrollNumberInput trainNameWidthNextBox = new CreateScrollNumberInput(0, 0, 32);
        trainNameWidthNextBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.train_name_width_next"));
        trainNameWidthNextBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.train_name_width.description"));
        trainNameWidthNextBox.min.set(-1D);
        trainNameWidthNextBox.max.set(99D);
        trainNameWidthNextBox.shiftStep.set(5D);
        trainNameWidthNextBox.format.set(new SpecialUnitNumberFormat(0, "px", true, false, trainNameWidthNextBox.min.get(), trainNameWidthNextBox.max.get()));
        trainNameWidthNextBox.value.set((double)setting.getTrainNameWidthNextStop());
        trainNameWidthNextBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setTrainNameWidthNextStop((byte)e.value());
            return false;
        });
        line.addComponent(trainNameWidthNextBox);
        
        CreateScrollNumberInput platformWidthNextBox = new CreateScrollNumberInput(0, 0, 32);
        platformWidthNextBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.platform_width_next"));
        platformWidthNextBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.platform_width.description"));
        platformWidthNextBox.min.set(-1D);
        platformWidthNextBox.max.set(64D);
        platformWidthNextBox.shiftStep.set(4D);
        platformWidthNextBox.format.set(new SpecialUnitNumberFormat(0, "px", true, false, platformWidthNextBox.min.get(), platformWidthNextBox.max.get()));
        platformWidthNextBox.value.set((double)setting.getPlatformWidthNextStop());
        platformWidthNextBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setPlatformWidthNextStop((byte)e.value());
            return false;
        });
        line.addComponent(platformWidthNextBox);
    }

    public static void buildDepartureBoardTableGui(DepartureBoardDisplayTableSettings setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(DepartureBoardDisplayTableSettings.GUI_LINE_TEXT_SIZE_NAME);
        
        MutableHolder<CreateScrollNumberInput> stopovers = new MutableHolder<CreateScrollNumberInput>(null);
        MutableHolder<CreateScrollNumberInput> info = new MutableHolder<CreateScrollNumberInput>(null);

        CreateScrollNumberInput stopoversWidthBox = new CreateScrollNumberInput(0, 0, 32); 
        stopovers.set(stopoversWidthBox);   
        stopoversWidthBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.stopovers_width"));
        stopoversWidthBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.stopovers_width.description"));
        stopoversWidthBox.min.set(0D);
        stopoversWidthBox.max.set(100D);
        stopoversWidthBox.shiftStep.set(5D);
        stopoversWidthBox.format.set(new SpecialUnitNumberFormat(0, "%", false, false, stopoversWidthBox.min.get(), stopoversWidthBox.max.get()));
        stopoversWidthBox.value.set((double)setting.getStopoversWidthPercentage() * 100);
        stopoversWidthBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setStopoversWidthPercentageInt((byte)e.value());
                DLUtils.doIfNotNull(info.get(), x -> {
                    x.min.set(0D);
                    x.max.set(MathUtils.clamp(100 - e.value(), 0, 100));
                });
            return false;
        });
        line.addComponent(stopoversWidthBox);
        if (stopovers.get() != null && info.get() != null) {
            stopovers.get().min.set(0D);
            stopovers.get().max.set(MathUtils.clamp(101 - info.get().value.get(), 0, 101));
            info.get().min.set(0D);
            info.get().max.set(MathUtils.clamp(101 - stopovers.get().value.get(), 0, 101));
        }

        CreateScrollNumberInput infoWidthBox = new CreateScrollNumberInput(0, 0, 32); 
        info.set(infoWidthBox);   
        infoWidthBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.info_width"));
        infoWidthBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.info_width.description"));
        infoWidthBox.min.set(0D);
        infoWidthBox.max.set(100D);
        infoWidthBox.shiftStep.set(5D);
        infoWidthBox.format.set(new SpecialUnitNumberFormat(0, "%", false, false, infoWidthBox.min.get(), infoWidthBox.max.get()));
        infoWidthBox.value.set((double)setting.getInfoWidthPercentage() * 100);
        infoWidthBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setInfoWidthPercentageInt((byte)e.value());
                DLUtils.doIfNotNull(stopovers.get(), x -> {
                    x.min.set(0D);
                    x.max.set(MathUtils.clamp(100 - e.value(), 0, 100));
                });
            return false;
        });
        line.addComponent(infoWidthBox);

        if (stopovers.get() != null && info.get() != null) {
            stopovers.get().min.set(0D);
            stopovers.get().max.set(MathUtils.clamp(100 - info.get().value.get(), 0, 100));
            info.get().min.set(0D);
            info.get().max.set(MathUtils.clamp(100 - stopovers.get().value.get(), 0, 100));
        }
    }

    public static void buildStaticTextBaseGui(StaticTextDisplaySettings setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(IColorSetting.GUI_LINE_COLORS_NAME);  

        DBNavigatorWidget dbwidget = new DBNavigatorWidget(0, 0, CreateButton.HEIGHT, setting.getSelectedComponentIndex(), StaticTextDisplaySettings.MAX_COMPONENTS,
        (/* next */) -> {
            if (setting.getSelectedComponentIndex() >= setting.getComponentsCount() - 1) {
                setting.verifyComponents();
                setting.createNewComponent();
            }
            setting.setSelectedComponentIndex(setting.getSelectedComponentIndex() + 1);
            context.container().clearLines();
            setting.buildGui(context);
        }, (/* previous */) -> {
            setting.setSelectedComponentIndex(setting.getSelectedComponentIndex() - 1);
            setting.verifyComponents();
            context.container().clearLines();
            setting.buildGui(context);
        });
        dbwidget.layoutContraint.set(FlowLayout.FlowConstraint.END);
        line.addComponent(dbwidget);
    }

    static void buildStaticTextGui(IStaticTextSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(IStaticTextSetting.GUI_STATIC_TEXT_NAME);
        
        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.TEXT.getAsSprite(16, 16));
        icon.tooltip.set(new DLTooltip(List.of(
            TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.static_text"),
            TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.static_text.description").withStyle(ChatFormatting.GRAY)
        ), 200));

        CreateTextBox textBox = new CreateTextBox(0, 0, 0);
        textBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        textBox.maxCharacters.set((int)Short.MAX_VALUE);
        textBox.text.get().set(setting.getStaticText());
        textBox.addEventListener(DLRichTextLabel.TextChangedEvent.class, (s, e) -> {
            setting.setStaticText(e.text().getPlainText());
            return false;
        });
        line.addComponent(textBox);
    }

    static void buildTextScaleGui(ITextScaleSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(ITextScaleSetting.GUI_LINE_TEXT_SIZE_NAME);
        
        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.SCALE.getAsSprite(16, 16));

        MutableHolder<CreateScrollNumberInput> scaleInput = new MutableHolder<CreateScrollNumberInput>(null);
        MutableHolder<CreateScrollNumberInput> minScaleInput = new MutableHolder<CreateScrollNumberInput>(null);
        
        CreateScrollNumberInput textMinScaleXBox = new CreateScrollNumberInput(0, 0, 32); 
        minScaleInput.set(textMinScaleXBox);   
        textMinScaleXBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_min_x_scale"));
        textMinScaleXBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_min_x_scale.description"));
        textMinScaleXBox.min.set(10D);
        textMinScaleXBox.max.set(100D);
        textMinScaleXBox.shiftStep.set(5D);
        textMinScaleXBox.format.set(new SpecialUnitNumberFormat(0, "%", false, false, textMinScaleXBox.min.get(), textMinScaleXBox.max.get()));
        textMinScaleXBox.value.set((double)setting.getMinXScale() * 100);
        textMinScaleXBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setMinXScale((float)e.value() / 100f);
                DLUtils.doIfNotNull(scaleInput.get(), x -> {
                    x.min.set(MathUtils.clamp(e.value(), 10, 100));
                    x.max.set(100D);
                });
            return false;
        });
        line.addComponent(textMinScaleXBox);
        if (scaleInput.get() != null && minScaleInput.get() != null) {
            scaleInput.get().min.set(MathUtils.clamp(minScaleInput.get().value.get(), 10, 100));
            scaleInput.get().max.set(100D);
            minScaleInput.get().min.set(10D);
            minScaleInput.get().max.set(MathUtils.clamp(scaleInput.get().value.get(), 10, 100));
        }

        
        CreateScrollNumberInput textScaleXBox = new CreateScrollNumberInput(0, 0, 32); 
        scaleInput.set(textScaleXBox);   
        textScaleXBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_x_scale"));
        textScaleXBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_x_scale.description"));
        textScaleXBox.min.set(10D);
        textScaleXBox.max.set(100D);
        textScaleXBox.shiftStep.set(5D);
        textScaleXBox.format.set(new SpecialUnitNumberFormat(0, "%", false, false, textScaleXBox.min.get(), textScaleXBox.max.get()));
        textScaleXBox.value.set((double)setting.getXScale() * 100);
        textScaleXBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setXScale((float)e.value() / 100f);
                DLUtils.doIfNotNull(minScaleInput.get(), x -> {
                    x.min.set(10D);
                    x.max.set(MathUtils.clamp(e.value(), 10, 100));
                });
            return false;
        });
        line.addComponent(textScaleXBox);
        if (scaleInput.get() != null && minScaleInput.get() != null) {
            scaleInput.get().min.set(MathUtils.clamp(minScaleInput.get().value.get(), 10, 100));
            scaleInput.get().max.set(100D);
            minScaleInput.get().min.set(10D);
            minScaleInput.get().max.set(MathUtils.clamp(scaleInput.get().value.get(), 10, 100));
        }
        
        CreateScrollNumberInput textScaleYBox = new CreateScrollNumberInput(0, 0, 32);
        textScaleYBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_y_scale"));
        textScaleYBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_y_scale.description"));
        textScaleYBox.min.set(10D);
        textScaleYBox.max.set(100D);
        textScaleYBox.shiftStep.set(5D);
        textScaleYBox.format.set(new SpecialUnitNumberFormat(0, "%", false, false, textScaleYBox.min.get(), textScaleYBox.max.get()));
        textScaleYBox.value.set((double)setting.getYScale() * 100);
        textScaleYBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setYScale((float)e.value() / 100f);
            return false;
        });
        line.addComponent(textScaleYBox);
    }

    static void buildTextPosGui(ITextPosSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(ITextPosSetting.GUI_LINE_TEXT_POS_NAME);
        
        MutableHolder<CreateScrollNumberInput> posYInput = new MutableHolder<CreateScrollNumberInput>(null);
        MutableHolder<CreateScrollNumberInput> posXInput = new MutableHolder<CreateScrollNumberInput>(null);
        MutableHolder<CreateButton> leftAlignBtn = new MutableHolder<CreateButton>(null);
        MutableHolder<CreateButton> centerAlignBtn = new MutableHolder<CreateButton>(null);
        MutableHolder<CreateButton> rightAlignBtn = new MutableHolder<CreateButton>(null);
        
        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.TEXT_LEFT_ALIGNED.getAsSprite(16, 16));
        
        CreateScrollNumberInput textPosXBox = new CreateScrollNumberInput(0, 0, 45); 
        posXInput.set(textPosXBox);   
        textPosXBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_pos_x"));
        textPosXBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_pos_x.description"));
        textPosXBox.min.set(0D);
        textPosXBox.max.set((double)ITextPosSetting.MAX_X);
        textPosXBox.step.set(0.5D);
        textPosXBox.shiftStep.set(5D);
        textPosXBox.format.set(new SpecialUnitNumberFormat(1, "px", false, false, textPosXBox.min.get(), textPosXBox.max.get()));
        textPosXBox.value.set((double)(setting.getX()));
        textPosXBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setX((float)e.value());
            return false;
        });
        line.addComponent(textPosXBox);
        
        CreateScrollNumberInput textPosYBox = new CreateScrollNumberInput(0, 0, 45); 
        posYInput.set(textPosYBox);   
        textPosYBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_pos_y"));
        textPosYBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_pos_y.description"));
        textPosYBox.min.set(0D);
        textPosYBox.max.set((double)ITextPosSetting.MAX_Y);
        textPosYBox.step.set(0.5D);
        textPosYBox.shiftStep.set(5D);
        textPosYBox.format.set(new SpecialUnitNumberFormat(1, "px", false, false, textPosYBox.min.get(), textPosYBox.max.get()));
        textPosYBox.value.set((double)(setting.getY()));
        textPosYBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setY((float)e.value());
            return false;
        });
        line.addComponent(textPosYBox);


        DLPanel alignPanel = new DLPanel(0, 0, CreateButton.WIDTH * 3, CreateButton.HEIGHT);
        FlowLayout layout = new FlowLayout();
        layout.flowDirection.set(Direction.HORIZONTAL);
        alignPanel.layout.set(layout);
        
        
        CreateButton leftAlignButton = new CreateButton(0, 0, ModGuiIcons.TEXT_LEFT_ALIGNED.getAsCreateIcon());
        leftAlignBtn.set(leftAlignButton);
        leftAlignButton.tooltip.set(new DLTooltip(List.of(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_left_aligned")), 200));
        alignPanel.addComponent(leftAlignButton);

        CreateButton centerAlignButton = new CreateButton(0, 0, ModGuiIcons.TEXT_CENTERED.getAsCreateIcon());
        centerAlignBtn.set(centerAlignButton);
        centerAlignButton.tooltip.set(new DLTooltip(List.of(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_centered")), 200));
        alignPanel.addComponent(centerAlignButton);

        CreateButton rightAlignButton = new CreateButton(0, 0, ModGuiIcons.TEXT_RIGHT_ALIGNED.getAsCreateIcon());
        rightAlignBtn.set(rightAlignButton);
        rightAlignButton.tooltip.set(new DLTooltip(List.of(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_right_aligned")), 200));
        alignPanel.addComponent(rightAlignButton);

        alignPanel.layoutContraint.set(FlowLayout.FlowConstraint.END);
        line.addComponent(alignPanel);

        leftAlignButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {            
            leftAlignBtn.get().enabled.set(false);
            centerAlignBtn.get().enabled.set(true);
            rightAlignBtn.get().enabled.set(true);
            setting.setTextAlignment(ETextAlignment.LEFT);
            return false;
        });
        
        centerAlignButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {            
            leftAlignBtn.get().enabled.set(true);
            centerAlignBtn.get().enabled.set(false);
            rightAlignBtn.get().enabled.set(true);
            setting.setTextAlignment(ETextAlignment.CENTER);
            return false;
        });
        
        rightAlignButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {            
            leftAlignBtn.get().enabled.set(true);
            centerAlignBtn.get().enabled.set(true);
            rightAlignBtn.get().enabled.set(false);
            setting.setTextAlignment(ETextAlignment.RIGHT);
            return false;
        });

        leftAlignButton.enabled.set(setting.getTextAlignment() != ETextAlignment.LEFT);
        centerAlignButton.enabled.set(setting.getTextAlignment() != ETextAlignment.CENTER);
        rightAlignButton.enabled.set(setting.getTextAlignment() != ETextAlignment.RIGHT);
    }

    static void buildTextMaxWidthGui(ITextWidthSetting setting, GuiBuilderContext context) {
        DLPanel line = context.container().addLine(ITextWidthSetting.GUI_LINE_TEXT_MAX_WIDTH_NAME);
        
        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.WIDTH.getAsSprite(16, 16));

        CreateScrollNumberInput textMaxWidthBox = new CreateScrollNumberInput(0, 0, 45); 
        textMaxWidthBox.title.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_max_width"));
        textMaxWidthBox.hint.set(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.text_max_width.description"));
        textMaxWidthBox.min.set((double)ITextWidthSetting.MIN_VALUE);
        textMaxWidthBox.max.set((double)ITextWidthSetting.MAX_VALUE);
        textMaxWidthBox.step.set(0.5D);
        textMaxWidthBox.shiftStep.set(20D);
        textMaxWidthBox.format.set(new SpecialUnitNumberFormat(1, "px", false, true, textMaxWidthBox.min.get(), textMaxWidthBox.max.get()));
        textMaxWidthBox.value.set((double)(setting.getTextMaxWidth()));
        textMaxWidthBox.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            setting.setTextMaxWidth((float)e.value());
            return false;
        });
        line.addComponent(textMaxWidthBox);        

        CreateItemPicker<TextScaleBounds> scaleBoundsBox = new CreateItemPicker<>(0, 0, 0);
        scaleBoundsBox.title.set(TextScaleBounds.CUT_OFF.getEnumTranslation());
        scaleBoundsBox.hint.set(TextScaleBounds.CUT_OFF.getEnumDescriptionTranslation());
        scaleBoundsBox.formatter.set(item -> item == null ? TextUtils.empty() : item.getValueTranslation());
        scaleBoundsBox.items.addAll(TextScaleBounds.values());
        scaleBoundsBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        scaleBoundsBox.selectedItem.set(Optional.ofNullable(setting.getBoundsAction()));
        scaleBoundsBox.addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
            scaleBoundsBox.selectedItem.get().ifPresent(item -> setting.setBoundsAction(item));
            return false;
        });
        line.addComponent(scaleBoundsBox);
    }

    static void buildTextBackgroundColorGui(ITextBackgroundColorSetting setting, GuiBuilderContext context) {        
        DLPanel line = context.container().addLine(ITextBackgroundColorSetting.GUI_BG_COLOR_NAME);

        IconSlotWidget icon = line.addComponent(new IconSlotWidget(0, 0));
        icon.icon.set(ModGuiIcons.COLOR_PALETTE.getAsSprite(16, 16));
        
        ColorSlotWidget backgroundColor = new ColorSlotWidget(
            0, 0,
            setting.getTextBackgroundColor(),
            Constants.DEFAULT_TRAIN_TYPE_COLORS,
            true, true,
            (b) -> setting.setTextBackgroundColor(b)
        );
        backgroundColor.tooltip.set(new DLTooltip(List.of(ITextBackgroundColorSetting.txtLabelBackgroundColor, IColorSetting.textClickToEdit), 200));
        line.addComponent(backgroundColor);

        DLCheckBox fullLineBox = new DLCheckBox(0, 0, 0, CreateButton.HEIGHT);
        fullLineBox.text.set(ITextBackgroundColorSetting.txtFullSize);
        fullLineBox.checked.set(setting.isFullLabelBackgroundColor());
        fullLineBox.layoutContraint.set(FlowLayout.FlowConstraint.FILL);
        fullLineBox.addEventListener(DLToggleButton.CheckedChangedEvent.class, (s, e) -> {
            setting.setFullLabelBackgroundColor(e.checked());
            return false;
        });
        fullLineBox.tooltip.set(new DLTooltip(List.of(ITextBackgroundColorSetting.txtFullSizeDescription), 200));
        line.addComponent(fullLineBox);
    }
    
}
