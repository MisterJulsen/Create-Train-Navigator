package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;

public interface IDisplaySettings extends INBTSerializable {

    /**
     * Serializes the data to NBT.
     * @return A new {@code CompoundTag} containing the serialized data.
     */
    CompoundTag serializeNbt();

    /**
     * Deserializes the NBT data.
     * @param nbt The NBT Compound Tag.
     */
    void deserializeNbt(CompoundTag nbt);

    /**
     * Called when updateing the settings of the block. Can be used to transfer old settings.
     * @param oldSettings The previous settings.
     */
    void onChangeSettings(IDisplaySettings oldSettings);

    /**
     * Called when building the "Advanced Settings" section in the Advanced Display Settings Screen.
     * @param container The container of the settings.
     * @param builder The builder to build the settings lines.
     */
    @Environment(EnvType.CLIENT) void buildGui(GuiBuilderContext context);
}
