package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;

public class NearestStationResponsePacket implements IPacketBase<NearestStationResponsePacket> {
    public long id;
    public NearestTrackStationResult result;
    
    public NearestStationResponsePacket() { }

    public NearestStationResponsePacket(long id, NearestTrackStationResult result) {
        this.id = id;
        this.result = result;
    }

    @Override
    public void encode(NearestStationResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        packet.result.serialize(buffer);
    }

    @Override
    public NearestStationResponsePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        NearestTrackStationResult result = NearestTrackStationResult.deserialize(buffer);
        return new NearestStationResponsePacket(id, result);
    }

    @Override
    public void handle(NearestStationResponsePacket packet, Supplier<PacketContext> contextSupplier) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            contextSupplier.get().queue(() -> {
                InstanceManager.runClientNearestStationResponseAction(packet.id, packet.result);
            });
        });
    }
}

