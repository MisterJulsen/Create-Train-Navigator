package de.mrjulsen.crn.network.packets.pain;

import java.util.UUID;

import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class DeleteTrainLinePacketData extends NetworkPacketData {

    private static final String NBT_ID = "Id";

    private UUID id;

    public DeleteTrainLinePacketData(DLStatus status) {
        super(status);
    }

    public DeleteTrainLinePacketData(UUID id) {
        super(DLStatus.OK);
        this.id = id;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.putUUID(NBT_ID, id);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.id = nbt.getUUID(NBT_ID);
    }

    public static EmptyNetworkPacketData handle(DeleteTrainLinePacketData packet, NetworkPacketContext context) {
        GlobalSettings.getInstance().getTrainLine(packet.id).ifPresent(x -> {
            if (!x.getOwner().isAllowed(new Owner(context.getPlayer())) || !GlobalSettings.modificationsAllowed(context.getPlayer())) {
                return;
            }
            GlobalSettings.getInstance().removeTrainLine(packet.id);
        });
        return new EmptyNetworkPacketData();
    }
    
}
