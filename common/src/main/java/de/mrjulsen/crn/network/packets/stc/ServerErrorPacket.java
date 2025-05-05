package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.mcdragonlib.net.BaseNetworkPacket;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ServerErrorPacket extends BaseNetworkPacket<ServerErrorPacket> {
    public String message;
    
    public ServerErrorPacket() { }

    public ServerErrorPacket(String message) {
        this.message = message;
    }

    @Override
    public void encode(ServerErrorPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(packet.message);
    }

    @Override
    public ServerErrorPacket decode(RegistryFriendlyByteBuf buffer) {
        return new ServerErrorPacket(buffer.readUtf());
    }

    @Override
    public void handle(ServerErrorPacket packet, Supplier<PacketContext> contextSupplier) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            contextSupplier.get().queue(() -> {
                ClientWrapper.handleErrorMessagePacket(packet, contextSupplier);
            });
        });
    }
}

