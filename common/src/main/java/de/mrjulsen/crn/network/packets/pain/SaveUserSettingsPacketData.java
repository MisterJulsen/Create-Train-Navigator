package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class SaveUserSettingsPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";
    private static final String NBT_ID = "Id";

    private UserSettings settings;

    public SaveUserSettingsPacketData(DLStatus status) {
        super(status);
    }

    public SaveUserSettingsPacketData(UserSettings settings) {
        super(DLStatus.OK);
        this.settings = settings;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.put(NBT_DATA, settings.toNbt());
        nbt.putUUID(NBT_ID, settings.getOwnerId());
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.settings = UserSettings.fromNbt(nbt.getCompound(NBT_DATA), nbt.getUUID(NBT_ID), false);
    }

    public static EmptyNetworkPacketData handle(SaveUserSettingsPacketData packet, NetworkPacketContext context) {
        packet.settings.save();
        return new EmptyNetworkPacketData();
    }
    
}
