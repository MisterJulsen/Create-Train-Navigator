package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.ICarriageIndexSetting;
import de.mrjulsen.crn.block.display.properties.components.IShowLineColorSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;

public class TrainDestinationDetailedSettings extends BasicDisplaySettings implements ICarriageIndexSetting, IShowLineColorSetting {

    protected byte carriageIndexOffset = 0;
    protected boolean overwriteCarriageIndex = false;
    protected boolean showLineColor = false;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_CARRIAGE_INDEX)) this.carriageIndexOffset = nbt.getByte(NBT_CARRIAGE_INDEX);
        if (nbt.contains(NBT_OVERWRITE_CARRIAGE_INDEX)) this.overwriteCarriageIndex = nbt.getBoolean(NBT_OVERWRITE_CARRIAGE_INDEX);        if (nbt.contains(NBT_SHOW_LINE_COLOR)) this.showLineColor = nbt.getBoolean(NBT_SHOW_LINE_COLOR);

    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putByte(NBT_CARRIAGE_INDEX, carriageIndexOffset);
        nbt.putBoolean(NBT_OVERWRITE_CARRIAGE_INDEX, overwriteCarriageIndex);
        nbt.putBoolean(NBT_SHOW_LINE_COLOR, showLineColor);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildCarriageIndexGui(context);
        this.buildShowLineColorGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyCarriageIndexSetting(oldSettings);
        copyShowLineColorSetting(oldSettings);
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
    public boolean showLineColor() {
        return showLineColor;
    }

    @Override
    public void setShowLineColor(boolean b) {
        this.showLineColor = b;
    }
    
}
