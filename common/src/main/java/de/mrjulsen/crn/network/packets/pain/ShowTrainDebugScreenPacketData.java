package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class ShowTrainDebugScreenPacketData extends NetworkPacketData {


    public ShowTrainDebugScreenPacketData(DLStatus status) {
        super(status);
    }

    public ShowTrainDebugScreenPacketData() {
        super(DLStatus.OK);
    }

    @Override
    protected void write(CompoundTag nbt) {}

    @Override
    protected void read(CompoundTag nbt) {}

    public static void handle(ShowTrainDebugScreenPacketData packet, NetworkPacketContext context) {
        ClientWrapper.showTrainDebugScreen();
    }
    
}
