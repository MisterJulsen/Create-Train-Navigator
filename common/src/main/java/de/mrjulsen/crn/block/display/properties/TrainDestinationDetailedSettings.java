package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.ICarriageIndexSetting;
import de.mrjulsen.crn.block.display.properties.components.IShowDoNotBoardText;
import de.mrjulsen.crn.block.display.properties.components.IShowLineColorSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.minecraft.nbt.CompoundTag;

public class TrainDestinationDetailedSettings extends BasicDisplaySettings implements ICarriageIndexSetting, IShowLineColorSetting, IShowDoNotBoardText {

    protected byte carriageIndexOffset = 0;
    protected boolean overwriteCarriageIndex = false;
    protected boolean showLineColor = false;
    protected boolean showDoNotBoardText = true;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_CARRIAGE_INDEX)) this.carriageIndexOffset = nbt.getByte(NBT_CARRIAGE_INDEX);
        if (nbt.contains(NBT_OVERWRITE_CARRIAGE_INDEX)) this.overwriteCarriageIndex = nbt.getBoolean(NBT_OVERWRITE_CARRIAGE_INDEX);
        if (nbt.contains(NBT_SHOW_LINE_COLOR)) this.showLineColor = nbt.getBoolean(NBT_SHOW_LINE_COLOR);
        if (nbt.contains(NBT_SHOW_DO_NOT_BOARD_TEXT)) this.showDoNotBoardText = nbt.getBoolean(NBT_SHOW_DO_NOT_BOARD_TEXT);

    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putByte(NBT_CARRIAGE_INDEX, carriageIndexOffset);
        nbt.putBoolean(NBT_OVERWRITE_CARRIAGE_INDEX, overwriteCarriageIndex);
        nbt.putBoolean(NBT_SHOW_LINE_COLOR, showLineColor);
        nbt.putBoolean(NBT_SHOW_DO_NOT_BOARD_TEXT, showDoNotBoardText);
    }

    @Override
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildCarriageIndexGui(context);
        this.buildShowLineColorGui(context);
        this.buildShowDoNotBoardTextGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyCarriageIndexSetting(oldSettings);
        copyShowLineColorSetting(oldSettings);
        copyShowDoNotBoardTextSetting(oldSettings);
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

    @Override
    public boolean showDoNotBoardText() {
        return showDoNotBoardText;
    }

    @Override
    public void setShowDoNotBoardText(boolean b) {
        this.showDoNotBoardText = b;
    }
    
}
