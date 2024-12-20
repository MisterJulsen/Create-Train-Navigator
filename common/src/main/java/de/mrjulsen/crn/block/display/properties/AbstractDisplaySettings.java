package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;

public abstract class AbstractDisplaySettings implements IDisplaySettings {

    public static final class EmptyDisplaySettings extends AbstractDisplaySettings {

        @Override
        public void deserializeNbt(CompoundTag nbt) { }

        @Override
        @Environment(EnvType.CLIENT)
        public void buildGui(GuiBuilderContext context) { }

        @Override
        public void serializeNbt(CompoundTag nbt) { }

        @Override
        public void onChangeSettings(IDisplaySettings oldSettings) { }
    }

    public AbstractDisplaySettings() {}

    @Override
    public final CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        this.serializeNbt(nbt);
        return nbt;
    }

    public abstract void serializeNbt(CompoundTag nbt);
}
