package de.mrjulsen.crn.network.packets.pain;

import java.util.UUID;

import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

public class UpdateTrainCategoryColorPacketData extends NetworkPacketData {

    private static final String NBT_ID = "Id";
    private static final String NBT_COLOR = "Color";

    private UUID id;
    private int color;

    public UpdateTrainCategoryColorPacketData(DLStatus status) {
        super(status);
    }    

    public UpdateTrainCategoryColorPacketData(UUID id, DLColor color) {
        super(DLStatus.OK);
        this.id = id;
        this.color = color.getAsARGB();
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.putUUID(NBT_ID, id);
        nbt.putInt(NBT_COLOR, color);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.id = nbt.getUUID(NBT_ID);
        this.color = nbt.getInt(NBT_COLOR);
    }
    
    public static EmptyNetworkPacketData handle(UpdateTrainCategoryColorPacketData packet, NetworkPacketContext context) {
        GlobalSettings.getInstance().getTrainCategory(packet.id).ifPresent(x -> {
            if (!x.getOwner().isAllowed(new Owner(context.getPlayer())) || !GlobalSettings.modificationsAllowed(context.getPlayer())) {
                return;
            }
            x.setColor(DLColor.fromInt(packet.color));
        });
        return new EmptyNetworkPacketData();
    }
}
