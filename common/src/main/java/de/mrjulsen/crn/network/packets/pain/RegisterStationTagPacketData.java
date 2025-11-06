package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class RegisterStationTagPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";

    private StationTag tag;

    public RegisterStationTagPacketData(DLStatus status) {
        super(status);
    }    

    public RegisterStationTagPacketData(StationTag tag) {
        super(DLStatus.OK);
        this.tag = tag;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.put(NBT_DATA, tag.toNbt());
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.tag = StationTag.fromNbt(nbt.getCompound(NBT_DATA), null);
    }
    
    public static EmptyNetworkPacketData handle(RegisterStationTagPacketData packet, NetworkPacketContext context) {
        if (!GlobalSettings.modificationsAllowed(context.getPlayer())) {
            return new EmptyNetworkPacketData();
        }
        packet.tag.updateLastEdited(context.getPlayer());
        GlobalSettings.getInstance().registerStationTag(packet.tag);
        return new EmptyNetworkPacketData();
    }
}
