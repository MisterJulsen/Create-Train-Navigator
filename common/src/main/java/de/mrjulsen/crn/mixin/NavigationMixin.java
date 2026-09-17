package de.mrjulsen.crn.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;

import de.mrjulsen.crn.api.core.INavigationPenaltyProvider;

@Mixin(Navigation.class)
public class NavigationMixin {

    @Shadow(remap = false)
    public Train train;

    @Redirect(remap = false, method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object crn$addEdgePointPenalties(Map<Object, Object> penalties, Object key, Object fallback) {
        Object value = penalties.getOrDefault(key, fallback);
        if (!(value instanceof Integer penalty) || !(key instanceof TrackEdge edge)) {
            return value;
        }
        return penalty + INavigationPenaltyProvider.calcEdgePenalty(edge, train);
    }
}
