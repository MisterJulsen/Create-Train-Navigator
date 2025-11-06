package de.mrjulsen.crn.network.packets.stc;

import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.nbt.CompoundTag;

public class ServerErrorPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";

    public String message;
    
    public ServerErrorPacketData(DLStatus status) {
        super(status);
    }

    public ServerErrorPacketData(String message) {
        super(DLStatus.OK);
        this.message = message;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.putString(NBT_DATA, message);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.message = nbt.getString(NBT_DATA);
    }

    public static void handle(ServerErrorPacketData packet, NetworkPacketContext context) {
        EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> {
            context.queue(() -> {
                ClientWrapper.handleErrorMessagePacket(packet, context);
            });
        });
    }
}

