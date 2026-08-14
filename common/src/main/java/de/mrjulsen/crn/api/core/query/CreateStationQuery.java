package de.mrjulsen.crn.api.core.query;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

/**
 * A filter over Create's stations, taken straight from the track network and independent of the CRN
 * backend. Each field that is set narrows the result, and a field left empty or {@code null} imposes
 * no constraint. Build one from {@link #all()} and the {@code with...} methods; instances are
 * immutable, so each method returns a new query.
 *
 * @param id            If non-empty, keeps only stations with one of these ids.
 * @param filter        If non-empty, keeps only stations whose name matches this filter, which may
 *                      use the schedule's wildcard syntax.
 * @param dimension     If non-empty, keeps only stations in one of these dimensions.
 * @param assembling    If set, keeps only stations that are, or are not, assembling a train.
 * @param nearestTrain  If non-empty, keeps only stations whose nearest train is one of these.
 * @param imminentTrain If non-empty, keeps only stations whose imminent train is one of these.
 * @param presentTrain  If non-empty, keeps only stations with one of these trains currently present.
 */
@QueryModel
public record CreateStationQuery(
        Set<UUID> id,
        String filter,
        Set<ResourceLocation> dimension,
        Boolean assembling,
        Set<UUID> nearestTrain,
        Set<UUID> imminentTrain,
        Set<UUID> presentTrain
) {

    /** A query that keeps every station, as a starting point for refinement. */
    public static CreateStationQuery all() {
        return new CreateStationQuery(Set.of(), "", Set.of(), null, Set.of(), Set.of(), Set.of());
    }

    /** Keeps only stations with one of the given ids. */
    @QueryParam(value = "id")
    public CreateStationQuery withId(Set<UUID> id) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    /** Keeps only stations whose name matches the given filter. */
    @QueryParam(value = "filter")
    public CreateStationQuery withFilter(String filter) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    /** Keeps only stations in one of the given dimensions. */
    @QueryParam(value = "dimension")
    public CreateStationQuery withDimension(Set<ResourceLocation> dimension) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    /** Keeps only stations that are, or are not, assembling a train. */
    @QueryParam(value = "assembling")
    public CreateStationQuery withAssembling(Boolean assembling) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    /** Keeps only stations whose nearest train is one of the given ones. */
    @QueryParam(value = "nearest_train")
    public CreateStationQuery withNearestTrain(Set<UUID> nearestTrain) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    /** Keeps only stations whose imminent train is one of the given ones. */
    @QueryParam(value = "imminent_train")
    public CreateStationQuery withImminentTrain(Set<UUID> imminentTrain) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    /** Keeps only stations with one of the given trains currently present. */
    @QueryParam(value = "present_train")
    public CreateStationQuery withPresentTrain(Set<UUID> presentTrain) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    /** Whether the given station passes this query. */
    public boolean accept(GlobalStation station) {
        Train nearestTrain = station.getNearestTrain();
        Train imminentTrain = station.getImminentTrain();
        Train presentTrain = station.getPresentTrain();
        return (id.isEmpty() || id.contains(station.id)) &&
                (filter.isEmpty() || TrainUtils.stationMatches(station.name, filter)) &&
                (dimension.isEmpty() || dimension.contains(station.blockEntityDimension.location())) &&
                (assembling == null || station.assembling == assembling) &&
                (this.nearestTrain.isEmpty() || (nearestTrain != null && this.nearestTrain.contains(nearestTrain.id))) &&
                (this.imminentTrain.isEmpty() || (imminentTrain != null && this.imminentTrain.contains(imminentTrain.id))) &&
                (this.presentTrain.isEmpty() || (presentTrain != null && this.presentTrain.contains(presentTrain.id)))
                ;
    }
}
