package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.IShowLineColorSetting;
import de.mrjulsen.crn.block.display.properties.components.ITrainNameWidthSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;

public class TrainDestinationCompactSettings extends BasicDisplaySettings implements ITrainNameWidthSetting, IShowLineColorSetting {

    protected byte trainNameWidth = -1;
    protected boolean showLineColor = false;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_TRAIN_NAME_WIDTH)) this.trainNameWidth = nbt.getByte(NBT_TRAIN_NAME_WIDTH);
        if (nbt.contains(NBT_SHOW_LINE_COLOR)) this.showLineColor = nbt.getBoolean(NBT_SHOW_LINE_COLOR);
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putByte(NBT_TRAIN_NAME_WIDTH, trainNameWidth);
        nbt.putBoolean(NBT_SHOW_LINE_COLOR, showLineColor);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildTrainNameGui(context, true, true);
        this.buildShowLineColorGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyTrainNameSetting(oldSettings);
        copyShowLineColorSetting(oldSettings);
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
    public boolean showLineColor() {
        return showLineColor;
    }

    @Override
    public void setShowLineColor(boolean b) {
        this.showLineColor = b;
    }
    
}
