package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.IPlatformWidthSetting;
import de.mrjulsen.crn.block.display.properties.components.IShowArrivalSetting;
import de.mrjulsen.crn.block.display.properties.components.IShowLineColorSetting;
import de.mrjulsen.crn.block.display.properties.components.IShowTimeAndDateSetting;
import de.mrjulsen.crn.block.display.properties.components.ITimeDisplaySetting;
import de.mrjulsen.crn.block.display.properties.components.ITrainNameWidthSetting;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.minecraft.nbt.CompoundTag;

public class PlatformDisplayTableSettings extends BasicDisplaySettings implements ITimeDisplaySetting, ITrainNameWidthSetting, IPlatformWidthSetting, IShowArrivalSetting, IShowLineColorSetting, IShowTimeAndDateSetting {
    
    protected ETimeDisplay timeDisplay = ETimeDisplay.ABS;
    protected byte trainNameWidth = ITrainNameWidthSetting.DEFAULT_TRAIN_NAME_WIDTH;
    protected byte platformWidth = -1;
    protected boolean showArrival = true;
    protected boolean showLineColor = false;
    protected boolean showTimeAndDate = true;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_TIME_DISPLAY)) this.timeDisplay = ETimeDisplay.getById(nbt.getByte(NBT_TIME_DISPLAY));
        if (nbt.contains(NBT_TRAIN_NAME_WIDTH)) this.trainNameWidth = nbt.getByte(NBT_TRAIN_NAME_WIDTH);
        if (nbt.contains(NBT_PLATFORM_WIDTH)) this.platformWidth = nbt.getByte(NBT_PLATFORM_WIDTH);
        if (nbt.contains(NBT_SHOW_ARRIVAL)) this.showArrival = nbt.getBoolean(NBT_SHOW_ARRIVAL);
        if (nbt.contains(NBT_SHOW_LINE_COLOR)) this.showLineColor = nbt.getBoolean(NBT_SHOW_LINE_COLOR);
        if (nbt.contains(NBT_SHOW_TIME_AND_DATE)) this.showTimeAndDate = nbt.getBoolean(NBT_SHOW_TIME_AND_DATE);
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putByte(NBT_TIME_DISPLAY, timeDisplay.getId());
        nbt.putByte(NBT_TRAIN_NAME_WIDTH, trainNameWidth);
        nbt.putByte(NBT_PLATFORM_WIDTH, platformWidth);
        nbt.putBoolean(NBT_SHOW_ARRIVAL, showArrival);
        nbt.putBoolean(NBT_SHOW_LINE_COLOR, showLineColor);
        nbt.putBoolean(NBT_SHOW_TIME_AND_DATE, showTimeAndDate);
    }

    @Override
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildTimeDisplayGui(context);
        this.buildTrainNameGui(context, true, false);
        this.buildPlatformWidthGui(context, true);
        this.buildShowArrivalGui(context);
        this.buildShowLineColorGui(context);
        this.buildShowTimeAndDateGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyTimeDisplaySetting(oldSettings);
        copyTrainNameSetting(oldSettings);
        copyPlatformWidthSetting(oldSettings);
        copyShowArrivalSetting(oldSettings);
        copyShowLineColorSetting(oldSettings);
        copyShowTimeAndDateSetting(oldSettings);
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
    public boolean showArrival() {
        return showArrival;
    }

    @Override
    public void setShowArrival(boolean b) {
        this.showArrival = b;
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
}
