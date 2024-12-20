package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.ICarriageIndexSetting;
import de.mrjulsen.crn.block.display.properties.components.IShowExitDirectionSetting;
import de.mrjulsen.crn.block.display.properties.components.IShowNextConnections;
import de.mrjulsen.crn.block.display.properties.components.IShowTrainStatsSetting;
import de.mrjulsen.crn.block.display.properties.components.ITimeDisplaySetting;
import de.mrjulsen.crn.block.display.properties.components.ITrainTextSetting;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;

public class PassengerInformationDetailedSettings extends BasicDisplaySettings implements
    ITimeDisplaySetting,
    IShowTrainStatsSetting,
    IShowExitDirectionSetting,
    ICarriageIndexSetting,
    IShowNextConnections,
    ITrainTextSetting
{

    protected ETimeDisplay timeDisplay = ETimeDisplay.ABS;
    protected boolean showStats = true;
    protected boolean showExit = true;
    protected boolean showConnections = true;
    protected byte carriageIndexOffset = 0;
    protected boolean overwriteCarriageIndex = false;
    protected ETrainTextComponents trainTextComponents = ETrainTextComponents.TRAIN_NAME;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_TIME_DISPLAY)) this.timeDisplay = ETimeDisplay.getById(nbt.getByte(NBT_TIME_DISPLAY));
        if (nbt.contains(NBT_SHOW_STATS)) this.showStats = nbt.getBoolean(NBT_SHOW_STATS);
        if (nbt.contains(NBT_SHOW_EXIT)) this.showExit = nbt.getBoolean(NBT_SHOW_EXIT);
        if (nbt.contains(NBT_SHOW_CONNECTIONS)) this.showConnections = nbt.getBoolean(NBT_SHOW_CONNECTIONS);
        if (nbt.contains(NBT_CARRIAGE_INDEX)) this.carriageIndexOffset = nbt.getByte(NBT_CARRIAGE_INDEX);
        if (nbt.contains(NBT_OVERWRITE_CARRIAGE_INDEX)) this.overwriteCarriageIndex = nbt.getBoolean(NBT_OVERWRITE_CARRIAGE_INDEX);
        if (nbt.contains(NBT_TRAIN_TEXT)) this.trainTextComponents = ETrainTextComponents.getById(nbt.getByte(NBT_TRAIN_TEXT));
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putByte(NBT_TIME_DISPLAY, timeDisplay.getId());
        nbt.putBoolean(NBT_SHOW_STATS, showStats);
        nbt.putBoolean(NBT_SHOW_EXIT, showExit);
        nbt.putBoolean(NBT_SHOW_CONNECTIONS, showConnections);
        nbt.putByte(NBT_CARRIAGE_INDEX, carriageIndexOffset);
        nbt.putBoolean(NBT_OVERWRITE_CARRIAGE_INDEX, overwriteCarriageIndex);
        nbt.putByte(NBT_TRAIN_TEXT, trainTextComponents.getId());
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildTimeDisplayGui(context);
        this.buildShowStatsGui(context);
        this.buildShowExitGui(context);
        this.buildShowConnectionGui(context);
        this.buildCarriageIndexGui(context);
        this.buildTrainTextGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyTimeDisplaySetting(oldSettings);
        copyShowExitSetting(oldSettings);
        copyShowStatsSetting(oldSettings);
        copyShowConnectionSetting(oldSettings);
        copyCarriageIndexSetting(oldSettings);
    }

    @Override
    public boolean showStats() {
        return showStats;
    }

    @Override
    public void setShowStats(boolean b) {
        this.showStats = b;
    }

    @Override
    public boolean showExit() {
        return showExit;
    }

    @Override
    public void setShowExit(boolean b) {
        this.showExit = b;
    }

    @Override
    public boolean showConnections() {
        return showConnections;
    }

    @Override
    public void setShowConnection(boolean b) {
        this.showConnections = b;
    }

    @Override
    public byte getCarriageIndex() {
        return carriageIndexOffset;
    }

    @Override
    public boolean shouldOverwriteCarriageIndex() {
        return overwriteCarriageIndex;
    }

    @Override
    public void setCarriageIndex(byte b) {
        this.carriageIndexOffset = b;
    }

    @Override
    public void setOverwriteCarriageIndex(boolean b) {
        this.overwriteCarriageIndex = b;
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
    public ETrainTextComponents getTrainTextComponents() {
        return trainTextComponents;
    }

    @Override
    public void setTrainTextComponents(ETrainTextComponents v) {
        this.trainTextComponents = v;
    }
}
