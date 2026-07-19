package de.mrjulsen.crn.backend.timing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.backend.api.ISpeedLimitProvider;
import de.mrjulsen.crn.backend.api.SpeedLimitProviderRegistry;
import de.mrjulsen.crn.backend.api.SpeedLimitQuery;
import de.mrjulsen.crn.backend.api.SpeedLimitSegment;
import de.mrjulsen.crn.backend.api.SpeedProfileSnapshot;

/**
 * Resolves the speed limits reported by the registered {@link ISpeedLimitProvider}s into a single
 * profile and integrates travel time over it, giving a bounded estimate instead of extrapolating
 * the train's current, possibly momentarily throttled speed across the whole distance.
 * <p>
 * Providers report independently and may overlap, so the profile takes the most restrictive limit
 * in force at each point.
 */
public final class SpeedProfileEstimator {

    /** Floor used when integrating, so a near-zero limit cannot produce an infinite estimate. */
    private static final double MIN_SPEED = 0.02;

    /** Accounts for acceleration and braking, matching the plain distance-over-speed fallback. */
    private static final int FUDGE_FACTOR = 2;

    private SpeedProfileEstimator() {}

    /**
     * Builds the speed profile for the stretch ahead of a train and estimates how long it needs
     * for it.
     * <p>
     * <b>Server thread only</b>, since it hands the train to the registered providers.
     *
     * @param train       The train to build the profile for.
     * @param distance    How far ahead the profile should reach, in blocks.
     * @param cruiseSpeed The speed the train would hold if nothing restricted it.
     * @param purpose     What the profile is for.
     * @return The resolved profile, or {@link SpeedProfileSnapshot#NONE} if no provider contributed.
     */
    public static SpeedProfileSnapshot profile(Train train, double distance, double cruiseSpeed, SpeedLimitQuery.SpeedLimitPurpose purpose) {
        if (distance <= 0 || SpeedLimitProviderRegistry.isEmpty()) {
            return SpeedProfileSnapshot.NONE;
        }

        SpeedLimitQuery query = new SpeedLimitQuery(train, distance, purpose, cruiseSpeed);
        List<SpeedLimitSegment> reported = SpeedLimitProviderRegistry.collect(query);
        if (reported.isEmpty()) {
            return SpeedProfileSnapshot.NONE;
        }

        List<SpeedLimitSegment> resolved = resolve(reported, distance, cruiseSpeed);
        return new SpeedProfileSnapshot(distance, cruiseSpeed, resolved, integrate(resolved, distance, cruiseSpeed));
    }

    /**
     * Estimates the remaining travel time over the reported limits.
     *
     * @return The estimated ticks, or {@code null} if no provider contributed anything, in which
     *         case the caller should fall back to its own estimate.
     */
    public static Integer estimate(Train train, double distance, double cruiseSpeed) {
        SpeedProfileSnapshot profile = profile(train, distance, cruiseSpeed, SpeedLimitQuery.SpeedLimitPurpose.TRAVEL_TIME_ESTIMATE);
        return profile.hasEstimate() ? profile.estimatedTicks() : null;
    }

    /**
     * Flattens the reported segments into one non-overlapping profile in ascending order, taking
     * the most restrictive limit in force at each point a limit starts or ends.
     */
    private static List<SpeedLimitSegment> resolve(List<SpeedLimitSegment> reported, double distance, double cruiseSpeed) {
        List<SpeedLimitSegment> sorted = new ArrayList<>(reported);
        sorted.sort(Comparator.comparingDouble(SpeedLimitSegment::startDistance));

        List<SpeedLimitSegment> resolved = new ArrayList<>();
        double previousLimit = Double.NaN;
        double previousStart = Double.NaN;

        for (SpeedLimitSegment candidate : sorted) {
            double start = candidate.startDistance();
            if (start > distance) {
                break;
            }
            if (start == previousStart) {
                continue;
            }
            previousStart = start;

            SpeedLimitSegment strictest = strictestAt(sorted, start, cruiseSpeed);
            if (strictest != null && strictest.speedLimit() != previousLimit) {
                resolved.add(strictest);
                previousLimit = strictest.speedLimit();
            }
        }
        return List.copyOf(resolved);
    }

    /** The most restrictive segment in force at the given distance, ignoring any above cruise speed. */
    private static SpeedLimitSegment strictestAt(List<SpeedLimitSegment> sorted, double distance, double cruiseSpeed) {
        SpeedLimitSegment strictest = null;
        for (SpeedLimitSegment segment : sorted) {
            if (segment.startDistance() > distance) {
                break;
            }
            if (segment.speedLimit() > cruiseSpeed) {
                continue;
            }
            if (strictest == null || segment.speedLimit() < strictest.speedLimit()) {
                strictest = segment;
            }
        }
        return strictest == null ? null : new SpeedLimitSegment(distance, strictest.speedLimit(),
            strictest.kind(), strictest.source(), strictest.descriptionKey());
    }

    /** Integrates travel time over the resolved profile. */
    private static int integrate(List<SpeedLimitSegment> resolved, double distance, double cruiseSpeed) {
        double ticks = 0;
        double position = 0;
        double limit = cruiseSpeed;

        for (SpeedLimitSegment segment : resolved) {
            if (segment.startDistance() > position) {
                ticks += (segment.startDistance() - position) / Math.max(limit, MIN_SPEED);
                position = segment.startDistance();
            }
            limit = segment.speedLimit();
        }
        if (position < distance) {
            ticks += (distance - position) / Math.max(limit, MIN_SPEED);
        }

        return (int) Math.min(Integer.MAX_VALUE, Math.round(ticks) * (long) FUDGE_FACTOR);
    }
}
