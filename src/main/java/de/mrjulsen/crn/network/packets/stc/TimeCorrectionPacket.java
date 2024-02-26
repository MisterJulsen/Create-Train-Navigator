package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

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
    public void handle(TimeCorrectionPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            NetworkManager.executeOnClient(() -> {
                JourneyListenerManager.getInstance().getAllListeners().forEach(x -> x.getListeningRoute().shiftTime(packet.amount));
            });
        });
        
        context.get().setPacketHandled(true);      
    }    
    
    @Override
    public NetworkDirection getDirection() {
        return NetworkDirection.PLAY_TO_CLIENT;
    }
}

