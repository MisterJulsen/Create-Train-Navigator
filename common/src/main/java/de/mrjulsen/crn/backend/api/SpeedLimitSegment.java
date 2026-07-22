package de.mrjulsen.crn.backend.api;

import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A speed limit affecting a train somewhere ahead on its current path.
 * <p>
 * A segment applies from {@link #startDistance()} until the next segment reported by the same
 * provider, or until the end of the queried horizon if it is the last one. Providers are therefore
 * independent of one another: each describes its own view of the line, and the backend takes the
 * most restrictive limit in force at any point.
 *
 * @param startDistance Distance in blocks from the train's current position at which this limit
 *                      begins to apply. {@code 0} means it is already in force. Never negative.
 * @param speedLimit    The maximum permitted speed in blocks per tick, the same unit as the train's
 *                      own maximum speed. Never negative.
 * @param kind          Why this limit applies. Purely descriptive; it does not affect the estimate.
 * @param source        The id of the provider that reported this segment, filled in by the registry.
 *                      {@code null} on a segment that has not been through a provider query.
 * @param descriptionKey Optional translation key describing this limit to a player, or {@code null}.
 */
public record SpeedLimitSegment(
    double startDistance,
    double speedLimit,
    SpeedLimitKind kind,
    ResourceLocation source,
    String descriptionKey
) {

    public SpeedLimitSegment {
        startDistance = Math.max(0, startDistance);
        speedLimit = Math.max(0, speedLimit);
        kind = kind == null ? SpeedLimitKind.OTHER : kind;
    }

    /** A permanent limit, the common case. */
    public static SpeedLimitSegment of(double startDistance, double speedLimit) {
        return new SpeedLimitSegment(startDistance, speedLimit, SpeedLimitKind.PERMANENT, null, null);
    }

    /** A limit of the given kind. */
    public static SpeedLimitSegment of(double startDistance, double speedLimit, SpeedLimitKind kind) {
        return new SpeedLimitSegment(startDistance, speedLimit, kind, null, null);
    }

    /** A copy of this segment described by the given translation key. */
    public SpeedLimitSegment describedBy(String descriptionKey) {
        return new SpeedLimitSegment(startDistance, speedLimit, kind, source, descriptionKey);
    }

    /** A copy of this segment attributed to the given provider. */
    public SpeedLimitSegment attributedTo(ResourceLocation source) {
        return new SpeedLimitSegment(startDistance, speedLimit, kind, source, descriptionKey);
    }

    /** Whether a description is available for this limit. */
    public boolean hasDescription() {
        return descriptionKey != null && !descriptionKey.isBlank();
    }

    /** Serializes this segment. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble(NBT_START_DISTANCE, startDistance);
        nbt.putDouble(NBT_SPEED_LIMIT, speedLimit);
        nbt.putString(NBT_KIND, kind.name());
        if (source != null) {
            nbt.putString(NBT_SOURCE, source.toString());
        }
        if (descriptionKey != null) {
            nbt.putString(NBT_DESCRIPTION_KEY, descriptionKey);
        }
        return nbt;
    }

    /** Deserializes a segment written by {@link #toNbt()}. */
    public static SpeedLimitSegment fromNbt(CompoundTag nbt) {
        return new SpeedLimitSegment(
            nbt.getDouble(NBT_START_DISTANCE),
            nbt.getDouble(NBT_SPEED_LIMIT),
            NbtHelper.readEnum(nbt.getString(NBT_KIND), SpeedLimitKind.class, SpeedLimitKind.OTHER),
            nbt.contains(NBT_SOURCE) ? new ResourceLocation(nbt.getString(NBT_SOURCE)) : null,
            nbt.contains(NBT_DESCRIPTION_KEY) ? nbt.getString(NBT_DESCRIPTION_KEY) : null
        );
    }

    private static final String NBT_START_DISTANCE = "StartDistance";
    private static final String NBT_SPEED_LIMIT = "SpeedLimit";
    private static final String NBT_KIND = "Kind";
    private static final String NBT_SOURCE = "Source";
    private static final String NBT_DESCRIPTION_KEY = "DescriptionKey";
}
