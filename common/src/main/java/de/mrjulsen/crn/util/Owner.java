package de.mrjulsen.crn.util;

import java.util.UUID;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class Owner {

    private static final String NBT_ID = "OwnerId";

    private final UUID id;
    private final String name;

    public Owner(Player player) {
        this(player.getUUID());
    }

    public Owner(UUID id) {
        this.name = id == null ? "Server" : CRNPlatformSpecific.getLastKnownPlayerName(id).orElse("");
        this.id = id;
    }

    public Owner() {
        this((UUID)null);
    }

    public UUID uuid() {
        return id == null ? Constants.ZERO_UUID : id;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Owner o) {
            return id == o.id || (id != null && o.id != null && id.equals(o.id));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id == null ? Constants.ZERO_UUID.hashCode() : id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", name, id == null ? Constants.ZERO_UUID : id);
    }
    
    public void toNbt(CompoundTag nbt) {
        if (id != null) nbt.putUUID(NBT_ID, id);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        toNbt(nbt);
        return nbt;
    }

    public static Owner fromNbt(CompoundTag nbt) {
        return nbt.contains(NBT_ID) ? new Owner(nbt.getUUID(NBT_ID)) : new Owner((UUID)null);
    }
}
