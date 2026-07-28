package de.mrjulsen.crn.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;

import de.mrjulsen.crn.registry.ModExtras;

@Mixin(Train.class)
public class TrainMixin {

    @Redirect(remap = false, method = "tickOccupiedObservers", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/graph/TrackGraph;getPoint(Lcom/simibubi/create/content/trains/graph/EdgePointType;Ljava/util/UUID;)Lcom/simibubi/create/content/trains/signal/TrackEdgePoint;"))
    private TrackEdgePoint crn$resolvePenaltyAnchors(TrackGraph graph, EdgePointType<?> type, UUID id) {
        TrackEdgePoint point = graph.getPoint(type, id);
        return point != null ? point : graph.getPoint(ModExtras.PENALTY_ANCHOR, id);
    }
}
