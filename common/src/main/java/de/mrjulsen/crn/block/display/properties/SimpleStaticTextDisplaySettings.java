package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.block.display.properties.components.IStaticTextSetting;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.minecraft.nbt.CompoundTag;

public class SimpleStaticTextDisplaySettings extends BasicDisplaySettings implements IStaticTextSetting {

    protected String staticText = DEFAULT_TEXT;    

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        if (nbt.contains(NBT_TEXT)) this.staticText = nbt.getString(NBT_TEXT);
    }

    @Override
    public void serializeNbt(CompoundTag nbt) {
        super.serializeNbt(nbt);
        nbt.putString(NBT_TEXT, staticText);
    }

    @Override
    public void buildGui(GuiBuilderContext context) {
        super.buildGui(context);
        this.buildStaticTextGui(context);
    }

    @Override
    public void onChangeSettings(IDisplaySettings oldSettings) {
        super.onChangeSettings(oldSettings);
        copyStaticTextSettings(oldSettings);
    }


    
    @Override
    public String getStaticText() {
        return staticText == null ? "" : staticText;
    }

    @Override
    public void setStaticText(String text) {
        this.staticText = text;
    }
}
