package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public final class EmptyNetworkPacketData extends NetworkPacketData {
    public EmptyNetworkPacketData(DLStatus status) {
        super(status);
    }
    
    public EmptyNetworkPacketData() {
        super(DLStatus.OK);
    }

    @Override
    protected void write(CompoundTag nbt) {
    }

    @Override
    protected void read(CompoundTag nbt) {
    }        
}
