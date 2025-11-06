package de.mrjulsen.crn.network.packets.pain;

import java.util.UUID;

import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class TrainHardResetPacketData extends NetworkPacketData {

    private static final String NBT_ID = "Id";

    private UUID id;

    public TrainHardResetPacketData(DLStatus status) {
        super(status);
    }

    public TrainHardResetPacketData(UUID id) {
        super(DLStatus.OK);
        this.id = id;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.putUUID(NBT_ID, id);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.id = nbt.getUUID(NBT_ID);
    }

    public static void handle(TrainHardResetPacketData packet, NetworkPacketContext context) {
        TrainListener.getTrainData(packet.id).ifPresent(TrainData::hardResetPredictions);
    }
    
}
