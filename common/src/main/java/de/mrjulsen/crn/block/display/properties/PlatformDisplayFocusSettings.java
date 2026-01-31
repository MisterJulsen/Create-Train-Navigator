package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.*;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.minecraft.nbt.CompoundTag;

public class PlatformDisplayFocusSettings extends BasicDisplaySettings implements ITimeDisplaySetting, ITrainNameWidthSetting, IPlatformWidthSetting, IShowArrivalSetting, IShowLineColorSetting, IShowTrainMultipleTimes {

    public static final String NBT_TRAIN_NAME_WIDTH_NEXT_STOP = "TrainNameWidthNextStop";
    public static final String NBT_PLATFORM_WIDTH_NEXT_STOP = "PlatformWidthNextStop";

    protected ETimeDisplay timeDisplay = ETimeDisplay.ABS;
    protected byte trainNameWidth = 12;
    protected byte trainNameWidthNextStop = -1;
    protected byte platformWidth = -1;
    protected byte platformWidthNextStop = -1;
    protected boolean showArrival = true;
    protected boolean showTrainLineColor = false;
    protected boolean showTrainMultipleTimes = true;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_TIME_DISPLAY)) this.timeDisplay = ETimeDisplay.getById(nbt.getByte(NBT_TIME_DISPLAY));
        if (nbt.contains(NBT_TRAIN_NAME_WIDTH)) this.trainNameWidth = nbt.getByte(NBT_TRAIN_NAME_WIDTH);
        if (nbt.contains(NBT_TRAIN_NAME_WIDTH_NEXT_STOP)) this.trainNameWidthNextStop = nbt.getByte(NBT_TRAIN_NAME_WIDTH_NEXT_STOP);
        if (nbt.contains(NBT_PLATFORM_WIDTH)) this.platformWidth = nbt.getByte(NBT_PLATFORM_WIDTH);
        if (nbt.contains(NBT_PLATFORM_WIDTH_NEXT_STOP)) this.platformWidthNextStop = nbt.getByte(NBT_PLATFORM_WIDTH_NEXT_STOP);
        if (nbt.contains(NBT_SHOW_ARRIVAL)) this.showArrival = nbt.getBoolean(NBT_SHOW_ARRIVAL);
        if (nbt.contains(NBT_SHOW_LINE_COLOR)) this.showTrainLineColor = nbt.getBoolean(NBT_SHOW_LINE_COLOR);
        if (nbt.contains(NBT_SHOW_TRAIN_MULTIPLE_TIMES)) this.showTrainMultipleTimes = nbt.getBoolean(NBT_SHOW_TRAIN_MULTIPLE_TIMES);
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putByte(NBT_TIME_DISPLAY, timeDisplay.getId());
        nbt.putByte(NBT_TRAIN_NAME_WIDTH, trainNameWidth);
        nbt.putByte(NBT_TRAIN_NAME_WIDTH_NEXT_STOP, trainNameWidthNextStop);
        nbt.putByte(NBT_PLATFORM_WIDTH, platformWidth);
        nbt.putByte(NBT_PLATFORM_WIDTH_NEXT_STOP, platformWidthNextStop);
        nbt.putBoolean(NBT_SHOW_ARRIVAL, showArrival);
        nbt.putBoolean(NBT_SHOW_LINE_COLOR, showTrainLineColor);
        nbt.putBoolean(NBT_SHOW_TRAIN_MULTIPLE_TIMES, showTrainMultipleTimes);

    }

    @Override
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildTimeDisplayGui(context);
        this.buildBasicTextWidthGui(context);
        GuiBuilderWrapper.buildPlatformDisplayFocusGui(this, context);
        this.buildShowArrivalGui(context);
        this.buildShowLineColorGui(context);
        this.buildShowTrainMultipleTimesGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyTimeDisplaySetting(oldSettings);
        copyTrainNameSetting(oldSettings);
        copyPlatformWidthSetting(oldSettings);
        copyShowArrivalSetting(oldSettings);
        copyShowLineColorSetting(oldSettings);
        copyShowTrainMultipleTimesSetting(oldSettings);

        if (oldSettings instanceof PlatformDisplayFocusSettings o) {
            setTrainNameWidthNextStop(o.getTrainNameWidthNextStop());
        }
    }

    @Override
    public ETimeDisplay getTimeDisplay() {
        return timeDisplay;
    }

    @Override
    public void setTimeDisplay(ETimeDisplay display) {
        this.timeDisplay = display;
    }

    @Override
    public byte getPlatformWidth() {
        return platformWidth;
    }

    @Override
    public void setPlatformWidth(byte b) {
        this.platformWidth = b;
    }

    public byte getPlatformWidthNextStop() {
        return platformWidthNextStop;
    }

    public void setPlatformWidthNextStop(byte b) {
        this.platformWidthNextStop = b;
    }

    @Override
    public byte getTrainNameWidth() {
        return trainNameWidth;
    }

    @Override
    public void setTrainNameWidth(byte b) {
        this.trainNameWidth = b;
    }

    public byte getTrainNameWidthNextStop() {
        return trainNameWidthNextStop;
    }

    public void setTrainNameWidthNextStop(byte b) {
        this.trainNameWidthNextStop = b;
    }

    @Override
    public boolean showArrival() {
        return showArrival;
    }

    @Override
    public void setShowArrival(boolean b) {
        this.showArrival = b;
    }

    @Override
    public boolean showTrainMultipleTimes() {
        return showTrainMultipleTimes;
    }

    @Override
    public void setShowTrainMultipleTimes(boolean b) {
        this.showTrainMultipleTimes = b;
    }

    public boolean isAutoTrainNameWidthNextStop() {
        return getTrainNameWidthNextStop() < 0;
    }

    public boolean isAutoPlatformWidthNextStop() {
        return getPlatformWidthNextStop() < 0;
    }

    @Override
    public void setShowLineColor(boolean b) {
        this.showTrainLineColor = b;
    }

    @Override
    public boolean showLineColor() {
        return showTrainLineColor;
    }
}
