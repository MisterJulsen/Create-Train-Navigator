package de.mrjulsen.crn.network.packets.cts;

import java.util.UUID;
import java.util.function.Supplier;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.TrainDataResponsePacket;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class TrainDataRequestPacket implements IPacketBase<TrainDataRequestPacket> {

    public long requestId;
    public UUID trainId;

    public TrainDataRequestPacket() { }
    
    public TrainDataRequestPacket(long requestId, UUID trainId) {
        this.requestId = requestId;
        this.trainId = trainId;
    }

    @Override
    public void encode(TrainDataRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requestId);
        buffer.writeUUID(packet.trainId);
    }

    @Override
    public TrainDataRequestPacket decode(FriendlyByteBuf buffer) {
        long requestId = buffer.readLong();
        UUID trainId = buffer.readUUID();

        return new TrainDataRequestPacket(requestId, trainId);
    }

    @Override
    public void handle(TrainDataRequestPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            Train train = TrainUtils.getTrain(packet.trainId);
            NetworkManager.sendToClient(new TrainDataResponsePacket(packet.requestId, new TrainData(
                packet.trainId,
                train.speed,
                train.navigation.ticksWaitingForSignal
            ), context.get().getSender().getLevel().getDayTime()), context.get().getSender());
        });
        
        context.get().setPacketHandled(true);
    }
    
    public static record TrainData(UUID trainId, double speed, int ticksWaitingForSignal) {
        private static final String NBT_TRAIN_ID = "Id";
        private static final String NBT_SPEED = "Speed";
        private static final String NBT_WAITING_FOR_SIGNAL = "WaitingForSignal";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putUUID(NBT_TRAIN_ID, trainId);
            nbt.putDouble(NBT_SPEED, speed);
            nbt.putInt(NBT_WAITING_FOR_SIGNAL, ticksWaitingForSignal);
            return nbt;
        }

        public static TrainData fromNbt(CompoundTag nbt) {
            return new TrainData(
                nbt.getUUID(NBT_TRAIN_ID),
                nbt.getDouble(NBT_SPEED),
                nbt.getInt(NBT_WAITING_FOR_SIGNAL)
            );
        }
    }
}
