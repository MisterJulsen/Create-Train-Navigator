package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.api.core.snapshot.TrainPositionSnapshot;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

@QueryModel
public record TrainPositionQuery(
        Set<ResourceLocation> dimensionIds,
        Boolean movingOnly,
        Boolean atStationOnly
) {

    public static TrainPositionQuery all() {
        return new TrainPositionQuery(Set.of(), null, null);
    }

    @QueryParam(value = "dimensions")
    public TrainPositionQuery withDimensionIds(Set<ResourceLocation> dimensionIds) {
        Objects.requireNonNull(dimensionIds);
        return new TrainPositionQuery(dimensionIds, movingOnly, atStationOnly);
    }

    @QueryParam(value = "moving_only")
    public TrainPositionQuery withMovingOnly(@Nullable Boolean movingOnly) {
        return new TrainPositionQuery(dimensionIds, movingOnly, atStationOnly);
    }

    @QueryParam(value = "at_station_only")
    public TrainPositionQuery withAtStationOnly(@Nullable Boolean atStationOnly) {
        return new TrainPositionQuery(dimensionIds, movingOnly, atStationOnly);
    }

    public boolean accept(TrainPositionSnapshot position) {
        return (dimensionIds.isEmpty() || dimensionIds.contains(position.dimension())) &&
                (movingOnly == null || position.isMoving() == movingOnly) &&
                (atStationOnly == null || (position.distanceToNextStop() <= 0D) == atStationOnly)
                ;
    }
}
