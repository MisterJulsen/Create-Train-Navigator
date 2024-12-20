package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.IShowExitDirectionSetting;
import de.mrjulsen.crn.block.display.properties.components.IShowTimeAndDateSetting;
import de.mrjulsen.crn.block.display.properties.components.IShowTrainStatsSetting;
import de.mrjulsen.crn.block.display.properties.components.ITrainTextSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.minecraft.nbt.CompoundTag;

public class PassengerInformationScrollingTextSettings extends BasicDisplaySettings implements IShowTrainStatsSetting, IShowExitDirectionSetting, IShowTimeAndDateSetting, ITrainTextSetting {

    protected boolean showStats = true;
    protected boolean showExit = true;
    protected boolean showTimeAndDate = true;
    protected ETrainTextComponents trainTextComponents = ETrainTextComponents.ALL;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_SHOW_STATS)) this.showStats = nbt.getBoolean(NBT_SHOW_STATS);
        if (nbt.contains(NBT_SHOW_EXIT)) this.showExit = nbt.getBoolean(NBT_SHOW_EXIT);
        if (nbt.contains(NBT_SHOW_TIME_AND_DATE)) this.showTimeAndDate = nbt.getBoolean(NBT_SHOW_TIME_AND_DATE);
        if (nbt.contains(NBT_TRAIN_TEXT)) this.trainTextComponents = ETrainTextComponents.getById(nbt.getByte(NBT_TRAIN_TEXT));
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putBoolean(NBT_SHOW_STATS, showStats);
        nbt.putBoolean(NBT_SHOW_EXIT, showExit);
        nbt.putBoolean(NBT_SHOW_TIME_AND_DATE, showTimeAndDate);
        nbt.putByte(NBT_TRAIN_TEXT, trainTextComponents.getId());
    }

    @Override
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildShowStatsGui(context);
        this.buildShowExitGui(context);
        this.buildShowTimeAndDateGui(context);
        this.buildTrainTextGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyShowExitSetting(oldSettings);
        copyShowStatsSetting(oldSettings);
        copyShowTimeAndDateSetting(oldSettings);
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
    public ETrainTextComponents getTrainTextComponents() {
        return trainTextComponents;
    }

    @Override
    public void setTrainTextComponents(ETrainTextComponents v) {
        this.trainTextComponents = v;
    }

    @Override
    public boolean showTimeAndDate() {
        return showTimeAndDate;
    }

    @Override
    public void setShowTimeAndDate(boolean b) {
        this.showTimeAndDate = b;
    }
}
