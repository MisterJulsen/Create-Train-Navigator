package de.mrjulsen.crn.navigator.route;

import de.mrjulsen.crn.backend.delay.DelayInstance;
import net.minecraft.nbt.CompoundTag;

/**
 * A delay reason as a route remembers it: the occurrence the backend reported, and when it stopped
 * applying.
 * <p>
 * The backend only ever reports what is wrong with a train <em>now</em>, so a reason disappears from
 * it the moment the train recovers. A journey needs the other view: the stop that was reached late
 * keeps its red times for good, so what made it late has to keep standing next to them. Hence this
 * closes an occurrence rather than dropping it.
 *
 * @param instance The occurrence as the backend last reported it.
 * @param until    When it stopped applying, or {@link #ACTIVE} while it still does.
 */
public record RecordedDelay(DelayInstance instance, long until) {

    /** Value of {@link #until()} while the reason still applies. */
    public static final long ACTIVE = -1;

    private static final String NBT_INSTANCE = "Instance";
    private static final String NBT_UNTIL = "Until";

    /** A newly seen occurrence, still applying. */
    public static RecordedDelay active(DelayInstance instance) {
        return new RecordedDelay(instance, ACTIVE);
    }

    /** Whether the reason still applies. */
    public boolean isActive() {
        return until == ACTIVE;
    }

    /** A copy that stopped applying at the given time. */
    public RecordedDelay closedAt(long time) {
        return isActive() ? new RecordedDelay(instance, time) : this;
    }

    /**
     * How long this reason applied, in ticks - up to now while it still does, up to when it ended
     * once it does not. Without the second case a reason from a journey long since travelled would
     * go on growing every time it is looked at.
     */
    public long duration(long now) {
        return Math.max(0, (isActive() ? now : until) - instance.since());
    }

    /** Serializes this record. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_INSTANCE, instance.toNbt());
        nbt.putLong(NBT_UNTIL, until);
        return nbt;
    }

    /** Deserializes a record written by {@link #toNbt()}. */
    public static RecordedDelay fromNbt(CompoundTag nbt) {
        return new RecordedDelay(DelayInstance.fromNbt(nbt.getCompound(NBT_INSTANCE)), nbt.getLong(NBT_UNTIL));
    }
}
