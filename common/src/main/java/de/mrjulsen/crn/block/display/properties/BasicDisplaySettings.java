package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.IColorSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;

/**
 * General settings that all displays share.
 */
public class BasicDisplaySettings extends AbstractDisplaySettings implements IColorSetting {

    protected int fontColor = 0xFFFFFFFF;
    protected int backColor = 0;

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        if (nbt.contains(NBT_FONT_COLOR)) this.fontColor = nbt.getInt(NBT_FONT_COLOR);
        if (nbt.contains(NBT_BACK_COLOR)) this.backColor = nbt.getInt(NBT_BACK_COLOR);
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        nbt.putInt(NBT_FONT_COLOR, fontColor);
        nbt.putInt(NBT_BACK_COLOR, backColor);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void buildGui(GuiBuilderContext context) {
        this.buildColorGui(context);
    }

    @Override
    public int getFontColor() {
        return fontColor;
    }

    @Override
    public void setFontColor(int fontColor) {
        this.fontColor = fontColor;
    }

    @Override
    public int getBackColor() {
        return backColor;
    }

    @Override
    public void setBackColor(int backColor) {
        this.backColor = backColor;
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        this.copyColorSetting(oldSettings);
    }
    
}
