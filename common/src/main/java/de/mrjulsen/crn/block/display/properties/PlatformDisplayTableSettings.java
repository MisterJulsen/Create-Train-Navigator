package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.*;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.minecraft.nbt.CompoundTag;

public class PlatformDisplayTableSettings extends BasicDisplaySettings implements
        ITimeDisplaySetting,
        ITrainNameWidthSetting,
        IPlatformWidthSetting,
        ITrainStopTypeSetting,
        IShowLineColorSetting,
        IShowTimeAndDateSetting,
        IShowTrainMultipleTimes
{
    
    protected ETimeDisplay timeDisplay = ETimeDisplay.ABS;
    protected byte trainNameWidth = ITrainNameWidthSetting.DEFAULT_TRAIN_NAME_WIDTH;
    protected byte platformWidth = -1;
    protected boolean showLineColor = false;
    protected boolean showTimeAndDate = true;
    protected boolean showTrainMultipleTimes = true;
    protected ETrainStopType trainStopType = ETrainStopType.DEPARTURES_PREFERRED;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_TIME_DISPLAY)) this.timeDisplay = ETimeDisplay.getById(nbt.getByte(NBT_TIME_DISPLAY));
        if (nbt.contains(NBT_TRAIN_NAME_WIDTH)) this.trainNameWidth = nbt.getByte(NBT_TRAIN_NAME_WIDTH);
        if (nbt.contains(NBT_PLATFORM_WIDTH)) this.platformWidth = nbt.getByte(NBT_PLATFORM_WIDTH);
        if (nbt.contains(NBT_SHOW_LINE_COLOR)) this.showLineColor = nbt.getBoolean(NBT_SHOW_LINE_COLOR);
        if (nbt.contains(NBT_SHOW_TIME_AND_DATE)) this.showTimeAndDate = nbt.getBoolean(NBT_SHOW_TIME_AND_DATE);
        if (nbt.contains(NBT_SHOW_TRAIN_MULTIPLE_TIMES)) this.showTrainMultipleTimes = nbt.getBoolean(NBT_SHOW_TRAIN_MULTIPLE_TIMES);
        if (nbt.contains(NBT_TRAIN_STOP_TYPE)) this.trainStopType = ETrainStopType.getById(nbt.getByte(NBT_TRAIN_STOP_TYPE));

        if (nbt.contains(LEGACY_NBT_SHOW_ARRIVAL)) this.trainStopType = nbt.getBoolean(LEGACY_NBT_SHOW_ARRIVAL) ? ETrainStopType.ALL : ETrainStopType.DEPARTURES_ONLY;
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putByte(NBT_TIME_DISPLAY, timeDisplay.getId());
        nbt.putByte(NBT_TRAIN_NAME_WIDTH, trainNameWidth);
        nbt.putByte(NBT_PLATFORM_WIDTH, platformWidth);
        nbt.putByte(NBT_TRAIN_STOP_TYPE, trainStopType.getId());
        nbt.putBoolean(NBT_SHOW_LINE_COLOR, showLineColor);
        nbt.putBoolean(NBT_SHOW_TIME_AND_DATE, showTimeAndDate);
        nbt.putBoolean(NBT_SHOW_TRAIN_MULTIPLE_TIMES, showTrainMultipleTimes);
    }

    @Override
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildTimeDisplayGui(context);
        this.buildTrainNameGui(context, true, false);
        this.buildPlatformWidthGui(context, true);
        this.buildTrainStopTypeGui(context);
        this.buildShowLineColorGui(context);
        this.buildShowTimeAndDateGui(context);
        this.buildShowTrainMultipleTimesGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyTimeDisplaySetting(oldSettings);
        copyTrainNameSetting(oldSettings);
        copyPlatformWidthSetting(oldSettings);
        copyTrainStopTypeSetting(oldSettings);
        copyShowLineColorSetting(oldSettings);
        copyShowTimeAndDateSetting(oldSettings);
        copyShowTrainMultipleTimesSetting(oldSettings);
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

    @Override
    public byte getTrainNameWidth() {
        return trainNameWidth;
    }

    @Override
    public void setTrainNameWidth(byte b) {
        this.trainNameWidth = b;
    }

    @Override
    public ETrainStopType getTrainStopType() {
        return trainStopType;
    }

    @Override
    public void setTrainStopType(ETrainStopType b) {
        this.trainStopType = b;
    }    

    @Override
    public boolean showLineColor() {
        return showLineColor;
    }

    @Override
    public void setShowLineColor(boolean b) {
        this.showLineColor = b;
    }

    @Override
    public void setShowTimeAndDate(boolean showTimeAndDate) {
        this.showTimeAndDate = showTimeAndDate;
    }

    @Override
    public boolean showTimeAndDate() {
        return showTimeAndDate;
    }


    @Override
    public void setShowTrainMultipleTimes(boolean b) {
        this.showTrainMultipleTimes = b;
    }

    @Override
    public boolean showTrainMultipleTimes() {
        return showTrainMultipleTimes;
    }
}
