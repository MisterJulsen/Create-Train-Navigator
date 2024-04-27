package de.mrjulsen.crn.data;

import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import net.minecraft.nbt.CompoundTag;

public class TrainData implements INBTSerializable {
    
    private static final String NBT_NAME = "Name";
    private static final String NBT_UUID = "UUID";


    private String name;
    private UUID id;

    public TrainData() {
        
    }

    public TrainData(Train train) {
        this.name = train.name.getString();
        this.id = train.id;
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_NAME, name);
        nbt.putUUID(NBT_UUID, id);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        name = nbt.getString(NBT_NAME);
        id = nbt.getUUID(NBT_UUID);
    }
}
