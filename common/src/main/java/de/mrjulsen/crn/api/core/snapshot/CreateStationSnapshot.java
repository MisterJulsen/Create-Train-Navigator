package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.station.GlobalStation;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record CreateStationSnapshot(
        @ResponseAlwaysInclude UUID id,
        String name,
        BlockPos position,
        ResourceLocation dimension,
        boolean assembling,
        UUID nearestTrain,
        UUID imminentTrain,
        UUID presentTrain,
        TrackNodeLocationSnapshot[] edgeLocation,
        double edgePosition
) {

    public static CreateStationSnapshot of(GlobalStation station) {
        Train nearestTrain = station.getNearestTrain();
        Train imminentTrain = station.getImminentTrain();
        Train presentTrain = station.getPresentTrain();
        return new CreateStationSnapshot(
                station.id,
                station.name,
                station.blockEntityPos,
                station.blockEntityDimension.location(),
                station.assembling,
                nearestTrain == null ? null : nearestTrain.id,
                imminentTrain == null ? null : imminentTrain.id,
                presentTrain == null ? null : presentTrain.id,
                new TrackNodeLocationSnapshot[] { TrackNodeLocationSnapshot.of(station.edgeLocation.get(true)), TrackNodeLocationSnapshot.of(station.edgeLocation.get(false)) },
                station.position
        );
    }



}
