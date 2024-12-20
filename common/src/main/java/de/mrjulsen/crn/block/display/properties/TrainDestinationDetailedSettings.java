package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.ICarriageIndexSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;

public class TrainDestinationDetailedSettings extends BasicDisplaySettings implements ICarriageIndexSetting {

    protected byte carriageIndexOffset = 0;
    protected boolean overwriteCarriageIndex = false;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_CARRIAGE_INDEX)) this.carriageIndexOffset = nbt.getByte(NBT_CARRIAGE_INDEX);
        if (nbt.contains(NBT_OVERWRITE_CARRIAGE_INDEX)) this.overwriteCarriageIndex = nbt.getBoolean(NBT_OVERWRITE_CARRIAGE_INDEX);
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putByte(NBT_CARRIAGE_INDEX, carriageIndexOffset);
        nbt.putBoolean(NBT_OVERWRITE_CARRIAGE_INDEX, overwriteCarriageIndex);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildCarriageIndexGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyCarriageIndexSetting(oldSettings);
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
    
}
