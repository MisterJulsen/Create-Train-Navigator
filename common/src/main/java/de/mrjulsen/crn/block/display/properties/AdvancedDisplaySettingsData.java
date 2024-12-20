package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeResourceKey;
import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import net.minecraft.nbt.CompoundTag;

public class AdvancedDisplaySettingsData implements INBTSerializable {

    public static final String NBT_SETTINGS = "Settings";
    public static final String NBT_DOUBLE_SIDED = "DoubleSided";

    private IDisplaySettings settings;
    private DisplayTypeResourceKey key;
    private boolean doubleSided;

    public AdvancedDisplaySettingsData() {}

    public AdvancedDisplaySettingsData(DisplayTypeResourceKey key, IDisplaySettings settings, boolean doubleSided) {
        this.key = key;
        this.settings = settings;
        this.doubleSided = doubleSided;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        key.toNbt(nbt);
        nbt.put(NBT_SETTINGS, settings.serializeNbt());
        nbt.putBoolean(NBT_DOUBLE_SIDED, doubleSided);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        this.key = DisplayTypeResourceKey.fromNbt(nbt);
        this.settings = AdvancedDisplaysRegistry.createSettings(key);
        this.settings.deserializeNbt(nbt.getCompound(NBT_SETTINGS));
        this.doubleSided = nbt.getBoolean(NBT_DOUBLE_SIDED);
    }

    public IDisplaySettings getSettings() {
        return settings;
    }

    public DisplayTypeResourceKey getKey() {
        return key;
    }

    public boolean isDoubleSided() {
        return doubleSided;
    }
}
