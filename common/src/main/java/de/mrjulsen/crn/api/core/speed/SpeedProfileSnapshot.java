package de.mrjulsen.crn.api.core.speed;

import java.util.List;

import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * How fast a train is expected to travel over the stretch ahead of it, and why. The segments are
 * ordered by distance and each applies until the next one begins.
 *
 * @param horizon        The distance this profile covers, in blocks ahead of the train.
 * @param cruiseSpeed    The speed the train would hold where nothing restricts it, in blocks per
 *                       tick.
 * @param segments       The restrictions along the way, ordered by {@code startDistance}.
 * @param estimatedTicks How long the train is expected to need for the whole distance, or
 *                       {@link #NO_ESTIMATE} if that could not be determined.
 */
public record SpeedProfileSnapshot(
    double horizon,
    double cruiseSpeed,
    List<SpeedLimitSegment> segments,
    int estimatedTicks
) {

    /** Stands for "no travel time could be estimated". */
    public static final int NO_ESTIMATE = -1;

    /** An empty profile, returned where no train or no route is known. */
    public static final SpeedProfileSnapshot NONE = new SpeedProfileSnapshot(0, 0, List.of(), NO_ESTIMATE);

    public SpeedProfileSnapshot {
        segments = segments == null ? List.of() : List.copyOf(segments);
    }

    /** Whether the profile holds no restrictions. */
    public boolean isEmpty() {
        return segments.isEmpty();
    }

    /** Whether a travel time could be estimated. */
    public boolean hasEstimate() {
        return estimatedTicks > NO_ESTIMATE;
    }

    /** The lowest speed permitted anywhere in the profile, never above the cruise speed. */
    public double slowestLimit() {
        double slowest = cruiseSpeed;
        for (SpeedLimitSegment segment : segments) {
            slowest = Math.min(slowest, segment.speedLimit());
        }
        return slowest;
    }

    /**
     * The speed permitted at the given distance ahead of the train, in blocks per tick. Yields the
     * cruise speed where no segment applies yet.
     */
    public double limitAt(double distance) {
        double limit = cruiseSpeed;
        for (SpeedLimitSegment segment : segments) {
            if (segment.startDistance() > distance) {
                break;
            }
            limit = segment.speedLimit();
        }
        return limit;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble(NBT_HORIZON, horizon);
        nbt.putDouble(NBT_CRUISE_SPEED, cruiseSpeed);
        nbt.put(NBT_SEGMENTS, NbtHelper.writeList(segments, SpeedLimitSegment::toNbt));
        nbt.putInt(NBT_ESTIMATED_TICKS, estimatedTicks);
        return nbt;
    }

    public static SpeedProfileSnapshot fromNbt(CompoundTag nbt) {
        return new SpeedProfileSnapshot(
            nbt.getDouble(NBT_HORIZON),
            nbt.getDouble(NBT_CRUISE_SPEED),
            NbtHelper.readList(nbt, NBT_SEGMENTS, SpeedLimitSegment::fromNbt),
            nbt.contains(NBT_ESTIMATED_TICKS) ? nbt.getInt(NBT_ESTIMATED_TICKS) : NO_ESTIMATE
        );
    }

    private static final String NBT_HORIZON = "Horizon";
    private static final String NBT_CRUISE_SPEED = "CruiseSpeed";
    private static final String NBT_SEGMENTS = "Segments";
    private static final String NBT_ESTIMATED_TICKS = "EstimatedTicks";
}
