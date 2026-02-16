package de.mrjulsen.crn.network.packets.pain;

import java.util.UUID;

import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class UpdateStationTagNamePacketData extends NetworkPacketData {

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";

    private UUID id;
    private String name;

    public UpdateStationTagNamePacketData(DLStatus status) {
        super(status);
    }    

    public UpdateStationTagNamePacketData(UUID tagId, String name) {
        super(DLStatus.OK);
        this.id = tagId;
        this.name = name;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.putUUID(NBT_ID, id);
        nbt.putString(NBT_NAME, name);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.id = nbt.getUUID(NBT_ID);
        this.name = nbt.getString(NBT_NAME);
    }
    
    public static EmptyNetworkPacketData handle(UpdateStationTagNamePacketData packet, NetworkPacketContext context) {
        GlobalSettings.getInstance().getStationTag(packet.id).ifPresent(x -> {
            if (!x.getOwner().isAllowed(new Owner(context.getPlayer())) || !GlobalSettings.modificationsAllowed(context.getPlayer())) {
                return;
            }
            x.updateLastEdited(context.getPlayer());
            x.setName(TagName.of(packet.name));
        });
        return new EmptyNetworkPacketData();
    }
}
