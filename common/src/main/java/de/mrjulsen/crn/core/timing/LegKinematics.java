package de.mrjulsen.crn.core.timing;

import java.util.Arrays;
import java.util.List;

import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.track.BezierConnection;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.core.speed.SpeedLimitQuery;
import de.mrjulsen.crn.api.core.speed.SpeedLimitSegment;
import de.mrjulsen.crn.api.core.speed.SpeedProfileSnapshot;

public final class LegKinematics {

    private static final double SAMPLE_SPACING = 0.5;
    private static final int MAX_SAMPLES = 2000;
    private static final double MIN_SPEED = 0.02;
    private static final double SCOUT_OVERSHOOT = 50;
    private static final double CURVE_MIN_VERTICAL = 1 / 16f;
    private static final double CURVE_STRAIGHT_EPSILON = 1 / 64f;
    private static final double CURVE_MILD_SLOPE = 0.225f;

    private final double length;
    private final double step;
    private final double[] cumulativeTime;
    private final int totalTicks;

    private LegKinematics(double length, double step, double[] cumulativeTime) {
        this.length = length;
        this.step = step;
        this.cumulativeTime = cumulativeTime;
        this.totalTicks = (int) Math.round(cumulativeTime[cumulativeTime.length - 1]);
    }

    public double length() {
        return length;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public int remainingTicks(double distanceToDestination) {
        double traveled = length - clamp(distanceToDestination, 0, length);
        return Math.max(0, totalTicks - (int) Math.round(timeAt(traveled)));
    }

    private double timeAt(double traveled) {
        double position = clamp(traveled / step, 0, cumulativeTime.length - 1);
        int i = (int) Math.floor(position);
        if (i >= cumulativeTime.length - 1) {
            return cumulativeTime[cumulativeTime.length - 1];
        }
        double frac = position - i;
        return cumulativeTime[i] + (cumulativeTime[i + 1] - cumulativeTime[i]) * frac;
    }

    public static LegKinematics build(Train train) {
        Navigation nav = train.navigation;
        if (nav == null || nav.destination == null || train.graph == null || train.carriages.isEmpty()) {
            return null;
        }

        try {
            return model(train, nav);
        } catch (Exception e) {
            if (CreateRailwaysNavigator.isDebug()) {
                CreateRailwaysNavigator.LOGGER.warn("[Backend] Could not build a kinematic profile for '{}'; falling back.", train.name.getString(), e);
            }
            return null;
        }
    }

    private static LegKinematics model(Train train, Navigation nav) {
        double acceleration = train.acceleration();
        double cruise = train.maxSpeed() * train.throttle;
        double turnSpeed = Math.min(cruise, train.maxTurnSpeed());
        double length = nav.distanceToDestination;
        if (acceleration <= 0 || cruise <= MIN_SPEED || length <= SAMPLE_SPACING) {
            return null;
        }

        int intervals = (int) Math.min(MAX_SAMPLES, Math.max(2, Math.ceil(length / SAMPLE_SPACING)));
        double step = length / intervals;
        int points = intervals + 1;

        double[] limit = new double[points];
        Arrays.fill(limit, cruise);
        collectCurves(train, length, turnSpeed, step, limit);
        collectSpeedSigns(train, length, cruise, step, limit);
        limit[points - 1] = 0;

        double[] brakeCeiling = new double[points];
        brakeCeiling[points - 1] = 0;
        for (int i = points - 2; i >= 0; i--) {
            double reachable = Math.sqrt(brakeCeiling[i + 1] * brakeCeiling[i + 1] + 2 * acceleration * step);
            brakeCeiling[i] = Math.min(limit[i], reachable);
        }

        double[] cumulative = new double[points];
        double speed = 0;
        for (int i = 1; i < points; i++) {
            double reachable = Math.sqrt(speed * speed + 2 * acceleration * step);
            double next = Math.min(brakeCeiling[i], reachable);
            double average = Math.max(MIN_SPEED, (speed + next) / 2);
            cumulative[i] = cumulative[i - 1] + step / average;
            speed = next;
        }

        return new LegKinematics(length, step, cumulative);
    }

    private static void collectCurves(Train train, double length, double turnSpeed, double step, double[] limit) {
        Navigation nav = train.navigation;
        boolean backwards = nav.destinationBehindTrain;
        TravellingPoint reference = backwards
            ? train.carriages.get(train.carriages.size() - 1).getTrailingPoint()
            : train.carriages.get(0).getLeadingPoint();
        if (reference == null || reference.edge == null) {
            return;
        }

        TravellingPoint scout = new TravellingPoint(reference.node1, reference.node2, reference.edge, reference.position, reference.upsideDown);
        double signedDistance = backwards ? -(length + SCOUT_OVERSHOOT) : (length + SCOUT_OVERSHOOT);
        scout.travel(train.graph, signedDistance, nav.controlSignalScout(), scout.ignoreEdgePoints(), (distance, edge) -> {
            if (!isRealCurve(edge)) {
                return;
            }
            double start = Math.abs(distance);
            applyLimit(limit, step, start, start + edge.getLength(), turnSpeed);
        });
    }

    private static void collectSpeedSigns(Train train, double length, double cruise, double step, double[] limit) {
        SpeedProfileSnapshot profile = SpeedProfileEstimator.profile(train, length, cruise, SpeedLimitQuery.SpeedLimitPurpose.TRAVEL_TIME_ESTIMATE);
        if (profile.isEmpty()) {
            return;
        }

        List<SpeedLimitSegment> segments = profile.segments();
        for (int i = 0; i < segments.size(); i++) {
            double from = segments.get(i).startDistance();
            double to = i + 1 < segments.size() ? segments.get(i + 1).startDistance() : length;
            applyLimit(limit, step, from, to, Math.min(cruise, segments.get(i).speedLimit()));
        }
    }

    private static boolean isRealCurve(TrackEdge edge) {
        BezierConnection turn = edge.getTurn();
        if (turn == null) {
            return false;
        }
        double verticalDrop = Math.abs(turn.starts.getFirst().y - turn.starts.getSecond().y);
        if (verticalDrop > CURVE_MIN_VERTICAL) {
            boolean straightHorizontally = turn.axes.getFirst().multiply(1, 0, 1).distanceTo(turn.axes.getSecond().multiply(1, 0, 1).scale(-1)) < CURVE_STRAIGHT_EPSILON;
            if (straightHorizontally && verticalDrop / turn.getLength() < CURVE_MILD_SLOPE) {
                return false;
            }
        }
        return true;
    }

    private static void applyLimit(double[] limit, double step, double from, double to, double cap) {
        int first = Math.max(0, (int) Math.floor(from / step));
        int last = Math.min(limit.length - 1, (int) Math.ceil(to / step));
        for (int i = first; i <= last; i++) {
            limit[i] = Math.min(limit[i], cap);
        }
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }
}
