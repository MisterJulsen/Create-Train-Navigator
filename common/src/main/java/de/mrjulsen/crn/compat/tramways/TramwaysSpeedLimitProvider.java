package de.mrjulsen.crn.compat.tramways;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackNode;

import de.mrjulsen.crn.api.core.speed.ISpeedLimitProvider;
import de.mrjulsen.crn.api.core.speed.SpeedLimitKind;
import de.mrjulsen.crn.api.core.speed.SpeedLimitQuery;
import de.mrjulsen.crn.api.core.speed.SpeedLimitSegment;
import purplecreate.tramways.content.signs.TramSignPoint;
import purplecreate.tramways.content.signs.demands.SignDemand;
import purplecreate.tramways.mixinInterfaces.ITram;

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

        segments.add(SpeedLimitSegment.of(0, train.throttle * train.maxSpeed(), SpeedLimitKind.TEMPORARY));

        boolean backwards = navigation.destinationBehindTrain;
        TravellingPoint reference = backwards
            ? train.carriages.get(train.carriages.size() - 1).getTrailingPoint()
            : train.carriages.get(0).getLeadingPoint();

        if (reference == null || reference.edge == null) {
            return segments;
        }

        TravellingPoint scout = new TravellingPoint(reference.node1, reference.node2, reference.edge,
            reference.position, reference.upsideDown);

        double scanDistance = Math.min(horizonDistance, navigation.distanceToDestination);
        double signedDistance = backwards ? -scanDistance : scanDistance;

        scout.travel(train.graph, signedDistance, navigation.controlSignalScout(), (distance, couple) -> {
            if (couple.getFirst() instanceof TramSignPoint sign) {
                TrackNode node = couple.getSecond().getSecond();
                boolean primary = sign.isPrimary(node);
                for (TramSignPoint.SignData signData : sign.getSignData(primary)) {
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
            return false;
        });

        return segments;
    }
}
