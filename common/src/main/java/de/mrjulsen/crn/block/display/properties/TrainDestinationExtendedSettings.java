package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.IShowDoNotBoardText;
import de.mrjulsen.crn.block.display.properties.components.IShowLineColorSetting;
import de.mrjulsen.crn.block.display.properties.components.ITrainNameWidthSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.minecraft.nbt.CompoundTag;

public class TrainDestinationExtendedSettings extends BasicDisplaySettings implements ITrainNameWidthSetting, IShowLineColorSetting, IShowDoNotBoardText {

    protected byte trainNameWidth = -1;
    protected boolean showLineColor = false;
    protected boolean showDoNotBoardText = true;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_TRAIN_NAME_WIDTH)) this.trainNameWidth = nbt.getByte(NBT_TRAIN_NAME_WIDTH);
        if (nbt.contains(NBT_SHOW_LINE_COLOR)) this.showLineColor = nbt.getBoolean(NBT_SHOW_LINE_COLOR);
        if (nbt.contains(NBT_SHOW_DO_NOT_BOARD_TEXT)) this.showDoNotBoardText = nbt.getBoolean(NBT_SHOW_DO_NOT_BOARD_TEXT);

    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putByte(NBT_TRAIN_NAME_WIDTH, trainNameWidth);
        nbt.putBoolean(NBT_SHOW_LINE_COLOR, showLineColor);
        nbt.putBoolean(NBT_SHOW_DO_NOT_BOARD_TEXT, showDoNotBoardText);
    }

    @Override
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildTrainNameGui(context, true, true);
        this.buildShowLineColorGui(context);
        this.buildShowDoNotBoardTextGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyTrainNameSetting(oldSettings);
        copyShowLineColorSetting(oldSettings);
        copyShowDoNotBoardTextSetting(oldSettings);
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

    @Override
    public boolean showDoNotBoardText() {
        return showDoNotBoardText;
    }

    @Override
    public void setShowDoNotBoardText(boolean b) {
        this.showDoNotBoardText = b;
    }
    
}
