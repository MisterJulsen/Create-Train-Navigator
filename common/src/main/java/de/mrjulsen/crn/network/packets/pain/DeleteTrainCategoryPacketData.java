package de.mrjulsen.crn.network.packets.pain;

import java.util.UUID;

import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class DeleteTrainCategoryPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";

    private UUID id;

    public DeleteTrainCategoryPacketData(DLStatus status) {
        super(status);
    }    

    public DeleteTrainCategoryPacketData(UUID id) {
        super(DLStatus.OK);
        this.id = id;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.putUUID(NBT_DATA, id);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.id = nbt.getUUID(NBT_DATA);
    }
    
    public static EmptyNetworkPacketData handle(DeleteTrainCategoryPacketData packet, NetworkPacketContext context) {
        GlobalSettings.getInstance().getTrainCategory(packet.id).ifPresent(x -> {
            if (!x.getOwner().isAllowed(new Owner(context.getPlayer())) || !GlobalSettings.modificationsAllowed(context.getPlayer())) {
                return;
            }
            GlobalSettings.getInstance().removeTrainCategory(packet.id);
        });
        return new EmptyNetworkPacketData();
    }
}
