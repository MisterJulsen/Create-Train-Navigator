/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.api.core.INavigationPenaltyProvider;
import de.mrjulsen.crn.util.PathCostCorrection;
import de.mrjulsen.crn.util.RedSignalBarrier;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;

@Mixin(Navigation.class)
public class NavigationMixin {

    @Shadow(remap = false)
    public Train train;


    @ModifyExpressionValue(remap = false, method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/TrackEdgePoint;canNavigateVia(Lcom/simibubi/create/content/trains/graph/TrackNode;)Z", ordinal = 0))
    private boolean crn$passStartingEdgePoint(boolean canNavigate, @Local(name = "costRelevant") boolean costRelevant, @Local(name = "point") TrackEdgePoint point, @Local(name = "initialNode2") TrackNode side, @Local(name = "initialPenalty") LocalIntRef penalty) {
        crn$accumulatePenalty(costRelevant, point, penalty);
        return canNavigate && !crn$isBarred(point, side);
    }

    @ModifyExpressionValue(remap = false, method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/TrackEdgePoint;canNavigateVia(Lcom/simibubi/create/content/trains/graph/TrackNode;)Z", ordinal = 1))
    private boolean crn$passEdgePoint(boolean canNavigate, @Local(name = "costRelevant") boolean costRelevant, @Local(name = "point") TrackEdgePoint point, @Local(name = "newNode") TrackNode side, @Local(name = "newPenalty") LocalIntRef penalty) {
        crn$accumulatePenalty(costRelevant, point, penalty);
        return canNavigate && !crn$isBarred(point, side);
    }

    @WrapOperation(remap = false, method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;test(DDLjava/util/Map;Lnet/createmod/catnip/data/Pair;Lcom/simibubi/create/content/trains/station/GlobalStation;)Z", ordinal = 1))
    private boolean crn$reportTravelledDistanceInCost(Navigation.StationTest stationTest, double distance, double cost, Map<TrackEdge, Pair<Boolean, Couple<TrackNode>>> reachedVia, Pair<Couple<TrackNode>, TrackEdge> current, GlobalStation station, Operation<Boolean> original, @Local(name = "penalty") int penalty) {
        double reported = PathCostCorrection.isActive() ? distance + penalty : cost;
        return original.call(stationTest, distance, reported, reachedVia, current, station);
    }

    @Unique
    private boolean crn$isBarred(TrackEdgePoint point, TrackNode side) {
        return RedSignalBarrier.appliesTo(train) && point instanceof SignalBoundary signal && signal.isForcedRed(side);
    }

    @Unique
    private void crn$accumulatePenalty(boolean costRelevant, TrackEdgePoint point, LocalIntRef penalty) {
        if (costRelevant && point instanceof INavigationPenaltyProvider provider) {
            penalty.set(penalty.get() + Math.max(0, provider.getNavigationPenalty(train)));
        }
    }
}
