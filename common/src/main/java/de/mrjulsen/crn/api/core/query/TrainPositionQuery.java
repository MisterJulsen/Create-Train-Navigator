package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.api.core.snapshot.TrainPositionSnapshot;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

/**
 * A filter over the train positions. Each field that is set narrows the result, and a field left
 * empty or {@code null} imposes no constraint. Build one from {@link #all()} and the {@code with...}
 * methods; instances are immutable, so each method returns a new query.
 *
 * @param dimensionIds  If non-empty, keeps only trains in one of these dimensions.
 * @param movingOnly    If set, keeps only trains that are, or are not, moving.
 * @param atStationOnly If set, keeps only trains that are, or are not, standing at a station.
 */
@QueryModel
public record TrainPositionQuery(
        Set<ResourceLocation> dimensionIds,
        Boolean movingOnly,
        Boolean atStationOnly
) {

    /** A query that keeps every train position, as a starting point for refinement. */
    public static TrainPositionQuery all() {
        return new TrainPositionQuery(Set.of(), null, null);
    }

    /** Keeps only trains in one of the given dimensions. */
    @QueryParam(value = "dimensions")
    public TrainPositionQuery withDimensionIds(Set<ResourceLocation> dimensionIds) {
        Objects.requireNonNull(dimensionIds);
        return new TrainPositionQuery(dimensionIds, movingOnly, atStationOnly);
    }

    /** Keeps only trains that are, or are not, moving. */
    @QueryParam(value = "moving_only")
    public TrainPositionQuery withMovingOnly(@Nullable Boolean movingOnly) {
        return new TrainPositionQuery(dimensionIds, movingOnly, atStationOnly);
    }

    /** Keeps only trains that are, or are not, standing at a station. */
    @QueryParam(value = "at_station_only")
    public TrainPositionQuery withAtStationOnly(@Nullable Boolean atStationOnly) {
        return new TrainPositionQuery(dimensionIds, movingOnly, atStationOnly);
    }

    /** Whether the given train position passes this query. */
    public boolean accept(TrainPositionSnapshot position) {
        return (dimensionIds.isEmpty() || dimensionIds.contains(position.dimension())) &&
                (movingOnly == null || position.isMoving() == movingOnly) &&
                (atStationOnly == null || (position.distanceToNextStop() <= 0D) == atStationOnly)
                ;
    }
}
