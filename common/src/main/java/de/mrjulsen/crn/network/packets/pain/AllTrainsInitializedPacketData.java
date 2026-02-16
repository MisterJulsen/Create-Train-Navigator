package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class AllTrainsInitializedPacketData extends NetworkPacketData {
    
    private static final String NBT_DATA = "Data";

    private boolean data;

    public AllTrainsInitializedPacketData(DLStatus status) {
        super(status);
    }
    
    public AllTrainsInitializedPacketData(boolean data) {
        super(DLStatus.OK);
        this.data = data;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.putBoolean(NBT_DATA, data);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.data = nbt.getBoolean(NBT_DATA);
    }
    
    public boolean getResult() {
        return data;
    }

    public static AllTrainsInitializedPacketData handle(NetworkPacketContext context) {
        return new AllTrainsInitializedPacketData(TrainListener.allTrainsInitialized());
    }
    
}
