package de.mrjulsen.crn.compat.tramways;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackNode;

import de.mrjulsen.crn.backend.api.ISpeedLimitProvider;
import de.mrjulsen.crn.backend.api.SpeedLimitKind;
import de.mrjulsen.crn.backend.api.SpeedLimitQuery;
import de.mrjulsen.crn.backend.api.SpeedLimitSegment;
import purplecreate.tramways.content.signs.TramSignPoint;
import purplecreate.tramways.content.signs.demands.SignDemand;
import purplecreate.tramways.mixinInterfaces.ITram;

/**
 * Reports the speed limits imposed by Tramways' speed signs to CRN's timetable prediction (see
 * {@link ISpeedLimitProvider} for the problem this solves).
 * <p>
 * Tramways itself only tracks signs reactively, within a short braking-distance scan window that
 * shrinks with the train's current speed (see Tramways' {@code NavigationMixin#tramways$tickSign}
 * / Create's {@code Navigation#tick}). That's enough to actually enforce a limit, but useless for
 * a long-range ETA: exactly while a train is crawling through a slow zone, the scan window all
 * but disappears. This performs its own, independent scan of the train's remaining path up to the
 * horizon CRN asks for, so upcoming limits are known well before the train reaches them.
 * <p>
 * This class is only ever loaded while Tramways is present (guarded by {@link TramwaysCompat},
 * which callers must check {@code Platform.isModLoaded("tramways")} before invoking) and depends
 * only on Tramways' public API as of release 0.3.3, so it works against an unmodified Tramways
 * install and can be tested right away. It is kept
 * deliberately self-contained: the scanning logic is the part worth eventually moving into
 * Tramways itself as a PR (where it could read a sign's target speed directly instead of via
 * {@link SignDemand#execute}, see the note below) - at that point this class becomes obsolete.
 */
public class TramwaysSpeedLimitProvider implements ISpeedLimitProvider {

    @Override
    public List<SpeedLimitSegment> getSpeedLimits(SpeedLimitQuery query) {
        Train train = query.train();
        double horizonDistance = query.horizon();
        if (!(train instanceof ITram) || train.graph == null || train.carriages.isEmpty()) {
            return List.of();
        }

        Navigation navigation = train.navigation;
        if (navigation == null || navigation.destination == null) {
            return List.of();
        }

        List<SpeedLimitSegment> segments = new ArrayList<>();

        // Whatever Tramways currently enforces applies from right now until whichever sign we
        // find ahead (if any) overrides it.
        segments.add(SpeedLimitSegment.of(0, train.throttle * train.maxSpeed(), SpeedLimitKind.TEMPORARY));

        boolean backwards = navigation.destinationBehindTrain;
        TravellingPoint reference = backwards
            ? train.carriages.get(train.carriages.size() - 1).getTrailingPoint()
            : train.carriages.get(0).getLeadingPoint();

        if (reference == null || reference.edge == null) {
            return segments;
        }

        // Work on a throwaway copy so we never disturb the train's actual travelling point,
        // mirroring how Navigation#tick clones its leading point into its own signal scout.
        TravellingPoint scout = new TravellingPoint(reference.node1, reference.node2, reference.edge,
            reference.position, reference.upsideDown);

        double scanDistance = Math.min(horizonDistance, navigation.distanceToDestination);
        double signedDistance = backwards ? -scanDistance : scanDistance;

        scout.travel(train.graph, signedDistance, navigation.controlSignalScout(), (distance, couple) -> {
            if (couple.getFirst() instanceof TramSignPoint sign) {
                TrackNode node = couple.getSecond().getSecond();
                boolean primary = sign.isPrimary(node);
                for (TramSignPoint.SignData signData : sign.getSignData(primary)) {
                    // Reuses Tramways' own demand execution rather than reading its (internal)
                    // sign settings directly. Its distance-gated braking-kinematics decision is
                    // built for live tick-by-tick enforcement rather than a "what speed will this
                    // impose" lookup, so a sign asking for a *higher* speed than the train's
                    // current one won't show up here until the train is basically on top of it -
                    // acceptable, since it only means we fall back to treating that stretch as
                    // still speed-limited, never that we overestimate the train's speed.
                    SignDemand.Result result = signData.execute(train, distance);
                    if (result == null) {
                        continue;
                    }
                    Double target = result.temporary != null ? result.temporary : result.permanent;
                    if (target != null) {
                        segments.add(SpeedLimitSegment.of(distance, target * train.maxSpeed(), SpeedLimitKind.PERMANENT));
                    }
                }
            }
            return false; // never stop early: we want every sign up to horizonDistance
        });

        return segments;
    }
}
