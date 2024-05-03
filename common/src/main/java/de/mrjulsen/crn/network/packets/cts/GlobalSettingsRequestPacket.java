package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.network.packets.stc.GlobalSettingsResponsePacket;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class GlobalSettingsRequestPacket implements IPacketBase<GlobalSettingsRequestPacket> {

    public long id;

    public GlobalSettingsRequestPacket() { }
    
    public GlobalSettingsRequestPacket(long id) {
        this.id = id;
    }

    @Override
    public void encode(GlobalSettingsRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
    }

    @Override
    public GlobalSettingsRequestPacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        return new GlobalSettingsRequestPacket(id);
    }
    
    @Override
    public void handle(GlobalSettingsRequestPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            CreateRailwaysNavigator.net().CHANNEL.sendToPlayer((ServerPlayer)contextSupplier.get().getPlayer(), new GlobalSettingsResponsePacket(packet.id, GlobalSettingsManager.getInstance().getSettingsData()));
        });
    }
}
