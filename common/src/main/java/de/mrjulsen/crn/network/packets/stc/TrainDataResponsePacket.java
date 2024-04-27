package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket.TrainData;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;

public class TrainDataResponsePacket implements IPacketBase<TrainDataResponsePacket> {
    public long id;
    public TrainData departure;
    public long time;
    
    public TrainDataResponsePacket() { }

    public TrainDataResponsePacket(long id, TrainData departure, long time) {
        this.id = id;
        this.departure = departure;
        this.time = time;
    }

    @Override
    public void encode(TrainDataResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeLong(packet.time);
        buffer.writeNbt(packet.departure.toNbt());
    }

    @Override
    public TrainDataResponsePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        long time = buffer.readLong();
        TrainData departure = TrainData.fromNbt(buffer.readNbt());

        return new TrainDataResponsePacket(id, departure, time);
    }
    @Override
    public void handle(TrainDataResponsePacket packet, Supplier<PacketContext> contextSupplier) {
        EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> {
            contextSupplier.get().queue(() -> {
                InstanceManager.runClientTrainDataResponseAction(packet.id, packet.departure, packet.time);
            });
        });
    }
}

