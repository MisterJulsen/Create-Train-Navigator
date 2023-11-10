package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.data.GlobalSettings;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GlobalSettingsResponsePacket implements IPacketBase<GlobalSettingsResponsePacket> {
    public long id;
    public GlobalSettings settings;
    
    public GlobalSettingsResponsePacket() { }

    public GlobalSettingsResponsePacket(long id, GlobalSettings settings) {
        this.id = id;
        this.settings = settings;
    }

    @Override
    public void encode(GlobalSettingsResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeNbt(packet.settings.toNbt(new CompoundTag()));
    }

    @Override
    public GlobalSettingsResponsePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        GlobalSettings settings = GlobalSettings.fromNbt(buffer.readNbt());
        return new GlobalSettingsResponsePacket(id, settings);
    }

    @Override
    public void handle(GlobalSettingsResponsePacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            NetworkManager.executeOnClient(() -> {
                GlobalSettingsManager.createClientInstance().updateSettingsData(packet.settings);
                InstanceManager.runClientResponseReceievedAction(packet.id);
            });
        });
        
        context.get().setPacketHandled(true);      
    }    
}

