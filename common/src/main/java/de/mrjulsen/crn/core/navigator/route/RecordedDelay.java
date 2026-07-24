package de.mrjulsen.crn.core.navigator.route;

import de.mrjulsen.crn.core.delay.DelayInstance;
import net.minecraft.nbt.CompoundTag;

public record RecordedDelay(DelayInstance instance, long until) {

    public static final long ACTIVE = -1;

    private static final String NBT_INSTANCE = "Instance";
    private static final String NBT_UNTIL = "Until";

    public static RecordedDelay active(DelayInstance instance) {
        return new RecordedDelay(instance, ACTIVE);
    }

    public boolean isActive() {
        return until == ACTIVE;
    }

    public RecordedDelay closedAt(long time) {
        return isActive() ? new RecordedDelay(instance, time) : this;
    }

    public long duration(long now) {
        return Math.max(0, (isActive() ? now : until) - instance.since());
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_INSTANCE, instance.toNbt());
        nbt.putLong(NBT_UNTIL, until);
        return nbt;
    }

    public static RecordedDelay fromNbt(CompoundTag nbt) {
        return new RecordedDelay(DelayInstance.fromNbt(nbt.getCompound(NBT_INSTANCE)), nbt.getLong(NBT_UNTIL));
    }
}
