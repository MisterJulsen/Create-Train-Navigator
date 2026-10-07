package de.mrjulsen.crn.api.core;

import com.simibubi.create.content.trains.entity.Train;

/**
 * Implemented by a track point that makes the stretch of track it sits on costlier for the
 * pathfinder, so trains route around it where they can. The penalty is folded into Create's own path
 * cost at the point's position along the edge, exactly like a signal or station, so it only weighs on
 * routes that actually pass it (see {@code NavigationMixin}). The penalty may depend on the train so
 * that a point applies to some trains only.
 */
public interface INavigationPenaltyProvider {

    /**
     * The extra cost this point adds for the given train, which may depend on the train so that a
     * point can apply to some trains only. Zero means no obstruction. Negative values are not
     * meaningful and are clamped away.
     */
    int getNavigationPenalty(Train train);
}
