package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;

import de.mrjulsen.crn.api.core.INavigationPenaltyProvider;

@Mixin(Navigation.class)
public class NavigationMixin {

    @Shadow(remap = false)
    public Train train;


    @ModifyExpressionValue(remap = false, method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/TrackEdgePoint;canNavigateVia(Lcom/simibubi/create/content/trains/graph/TrackNode;)Z", ordinal = 0))
    private boolean crn$addStartingEdgePenalty(boolean canNavigate, @Local(name = "costRelevant") boolean costRelevant, @Local(name = "point") TrackEdgePoint point, @Local(name = "initialPenalty") LocalIntRef penalty) {
        crn$accumulatePenalty(costRelevant, point, penalty);
        return canNavigate;
    }

    @ModifyExpressionValue(remap = false, method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/TrackEdgePoint;canNavigateVia(Lcom/simibubi/create/content/trains/graph/TrackNode;)Z", ordinal = 1))
    private boolean crn$addEdgePenalty(boolean canNavigate, @Local(name = "costRelevant") boolean costRelevant, @Local(name = "point") TrackEdgePoint point, @Local(name = "newPenalty") LocalIntRef penalty) {
        crn$accumulatePenalty(costRelevant, point, penalty);
        return canNavigate;
    }

    @Unique
    private void crn$accumulatePenalty(boolean costRelevant, TrackEdgePoint point, LocalIntRef penalty) {
        if (costRelevant && point instanceof INavigationPenaltyProvider provider) {
            penalty.set(penalty.get() + Math.max(0, provider.getNavigationPenalty(train)));
        }
    }
}
