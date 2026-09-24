package de.mrjulsen.crn.core.navigator.route;

import de.mrjulsen.crn.core.delay.DelayInstance;
import net.minecraft.nbt.CompoundTag;

/**
 * One delay reason recorded against a route leg, kept even after it stops applying so that a
 * journey's history stays complete.
 *
 * @param instance The delay reason.
 * @param until    When the reason stopped applying, or {@link #ACTIVE} while it still does.
 */
public record RecordedDelay(DelayInstance instance, long until) {

    /** The value of {@link #until} while the reason still applies. */
    public static final long ACTIVE = -1;

    private static final String NBT_INSTANCE = "Instance";
    private static final String NBT_UNTIL = "Until";

    public static RecordedDelay active(DelayInstance instance) {
        return new RecordedDelay(instance, ACTIVE);
    }

    /** Whether the reason still applies. */
    public boolean isActive() {
        return until == ACTIVE;
    }

    public RecordedDelay closedAt(long time) {
        return isActive() ? new RecordedDelay(instance, time) : this;
    }

    /** How long the reason applied for, counting up to the given time while it still applies, in ticks. */
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
