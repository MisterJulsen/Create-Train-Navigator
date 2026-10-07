package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import net.minecraft.nbt.CompoundTag;

public interface IDisplaySettings extends INBTSerializable {

    CompoundTag serializeNbt();

    void deserializeNbt(CompoundTag nbt);

    void onChangeSettings(IDisplaySettings oldSettings);

 void buildGui(GuiBuilderContext context);
}
