package de.mrjulsen.crn.backend.api;

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
}
