package de.mrjulsen.crn.backend.api;

import java.util.List;

/**
 * The speed limits in force along the stretch of track ahead of a train, as reported by the
 * registered {@link ISpeedLimitProvider}s and resolved into one profile.
 * <p>
 * Each entry states the limit in force from its own start until the next entry begins. Where several
 * providers overlap, the most restrictive limit wins and the entry is attributed to the provider it
 * came from, so a display can explain where a restriction originates.
 *
 * @param horizon        How far ahead the profile reaches, in blocks.
 * @param cruiseSpeed    The speed in blocks per tick the train would hold if nothing restricted it.
 * @param segments       The resolved limits in ascending order of distance. Empty if nothing
 *                       restricts the train, in which case it may hold {@link #cruiseSpeed()}
 *                       throughout.
 * @param estimatedTicks How long the train needs for the whole horizon under this profile, or
 *                       {@link #NO_ESTIMATE} if no provider contributed anything.
 */
public record SpeedProfileSnapshot(
    double horizon,
    double cruiseSpeed,
    List<SpeedLimitSegment> segments,
    int estimatedTicks
) {

    /** Value of {@link #estimatedTicks()} when no provider contributed to this profile. */
    public static final int NO_ESTIMATE = -1;

    /** An empty profile, for a train no provider has anything to say about. */
    public static final SpeedProfileSnapshot NONE = new SpeedProfileSnapshot(0, 0, List.of(), NO_ESTIMATE);

    public SpeedProfileSnapshot {
        segments = segments == null ? List.of() : List.copyOf(segments);
    }

    /** Whether any provider contributed to this profile. */
    public boolean isEmpty() {
        return segments.isEmpty();
    }

    /** Whether a travel time could be estimated from this profile. */
    public boolean hasEstimate() {
        return estimatedTicks > NO_ESTIMATE;
    }

    /** The lowest limit anywhere in the profile, or {@link #cruiseSpeed()} if it is empty. */
    public double slowestLimit() {
        double slowest = cruiseSpeed;
        for (SpeedLimitSegment segment : segments) {
            slowest = Math.min(slowest, segment.speedLimit());
        }
        return slowest;
    }

    /** The limit in force at the given distance ahead of the train, in blocks per tick. */
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
}
