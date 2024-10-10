package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.data.GlobalSettings;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

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
    public void handle(GlobalSettingsResponsePacket packet, Supplier<PacketContext> contextSupplier) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            contextSupplier.get().queue(() -> {
                GlobalSettingsManager.createClientInstance().updateSettingsData(packet.settings);
                InstanceManager.runClientResponseReceievedAction(packet.id);
            });
        });
    }
}

