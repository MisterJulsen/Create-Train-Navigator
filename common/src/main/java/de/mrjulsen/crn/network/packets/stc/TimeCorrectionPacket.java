package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;

public class TimeCorrectionPacket implements IPacketBase<TimeCorrectionPacket> {
    public int amount;
    
    public TimeCorrectionPacket() { }

    public TimeCorrectionPacket(int amount) {
        this.amount = amount;
    }

    @Override
    public void encode(TimeCorrectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.amount);
    }

    @Override
    public TimeCorrectionPacket decode(FriendlyByteBuf buffer) {        
        return new TimeCorrectionPacket(buffer.readInt());
    }

    @Override
    public void handle(TimeCorrectionPacket packet, Supplier<PacketContext> contextSupplier) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            contextSupplier.get().queue(() -> {
                JourneyListenerManager.getInstance().getAllListeners().forEach(x -> x.getListeningRoute().shiftTime(packet.amount));
            });
        });
    }
}

