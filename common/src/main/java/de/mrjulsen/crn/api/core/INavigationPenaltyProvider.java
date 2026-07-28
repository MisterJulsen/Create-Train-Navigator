package de.mrjulsen.crn.api.core;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;

public interface INavigationPenaltyProvider {

    /**
     * The extra cost this point adds for the given train, which may depend on the train so that a
     * point can apply to some trains only. Zero means no obstruction. Negative values are not
     * meaningful and are clamped away.
     */
    int getNavigationPenalty(Train train);

    /**
     * The combined penalty of every provider sitting on {@code edge}.
     */
    static int calcEdgePenalty(TrackEdge edge, Train train) {
        if (edge == null || !edge.getEdgeData().hasPoints()) {
            return 0;
        }

        int sum = 0;
        for (TrackEdgePoint point : edge.getEdgeData().getPoints()) {
            if (point instanceof INavigationPenaltyProvider provider) {
                sum += Math.max(0, provider.getNavigationPenalty(train));
            }
        }
        return sum;
    }
}
