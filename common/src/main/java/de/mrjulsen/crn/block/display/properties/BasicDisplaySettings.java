package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.IColorSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

/**
 * General settings that all displays share.
 */
public class BasicDisplaySettings extends AbstractDisplaySettings implements IColorSetting {

    protected DLColor fontColor = DLColor.WHITE;
    protected DLColor backColor = DLColor.TRANSPARENT;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        if (nbt.contains(NBT_FONT_COLOR)) this.fontColor = DLColor.fromInt(nbt.getInt(NBT_FONT_COLOR));
        if (nbt.contains(NBT_BACK_COLOR)) this.backColor = DLColor.fromInt(nbt.getInt(NBT_BACK_COLOR));
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        nbt.putInt(NBT_FONT_COLOR, fontColor.getAsARGB());
        nbt.putInt(NBT_BACK_COLOR, backColor.getAsARGB());
    }

    @Override
    public void buildGui(GuiBuilderContext context) {
        this.buildColorGui(context);
    }

    @Override
    public DLColor getFontColor() {
        return fontColor;
    }

    @Override
    public void setFontColor(DLColor fontColor) {
        this.fontColor = fontColor;
    }

    @Override
    public DLColor getBackColor() {
        return backColor;
    }

    @Override
    public void setBackColor(DLColor backColor) {
        this.backColor = backColor;
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        this.copyColorSetting(oldSettings);
    }
    
}
