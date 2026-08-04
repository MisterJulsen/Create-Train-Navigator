package de.mrjulsen.crn.core.timing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.api.core.speed.SpeedLimitProviderRegistry;
import de.mrjulsen.crn.api.core.speed.SpeedLimitQuery;
import de.mrjulsen.crn.api.core.speed.SpeedLimitSegment;
import de.mrjulsen.crn.api.core.speed.SpeedProfileSnapshot;

public final class SpeedProfileEstimator {

    private static final double MIN_SPEED = 0.02;

    private static final int FUDGE_FACTOR = 2;

    private SpeedProfileEstimator() {}

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

    public static Integer estimate(Train train, double distance, double cruiseSpeed) {
        SpeedProfileSnapshot profile = profile(train, distance, cruiseSpeed, SpeedLimitQuery.SpeedLimitPurpose.TRAVEL_TIME_ESTIMATE);
        return profile.hasEstimate() ? profile.estimatedTicks() : null;
    }

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
