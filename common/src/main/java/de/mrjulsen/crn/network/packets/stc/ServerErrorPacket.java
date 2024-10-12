package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;

public class ServerErrorPacket implements IPacketBase<ServerErrorPacket> {
    public String message;
    
    public ServerErrorPacket() { }

    public ServerErrorPacket(String message) {
        this.message = message;
    }

    @Override
    public void encode(ServerErrorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.message);
    }

    @Override
    public ServerErrorPacket decode(FriendlyByteBuf buffer) {        
        return new ServerErrorPacket(buffer.readUtf());
    }

    @Override
    public void handle(ServerErrorPacket packet, Supplier<PacketContext> contextSupplier) {
        EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> {
            contextSupplier.get().queue(() -> {
                ClientWrapper.handleErrorMessagePacket(packet, contextSupplier);
            });
        });
    }
}

