package de.mrjulsen.crn.api.core.query;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

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

    public static CreateStationQuery all() {
        return new CreateStationQuery(Set.of(), "", Set.of(), null, Set.of(), Set.of(), Set.of());
    }

    @QueryParam(value = "id")
    public CreateStationQuery withId(Set<UUID> id) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    @QueryParam(value = "filter")
    public CreateStationQuery withFilter(String filter) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    @QueryParam(value = "dimension")
    public CreateStationQuery withDimension(Set<ResourceLocation> dimension) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    @QueryParam(value = "assembling")
    public CreateStationQuery withAssembling(Boolean assembling) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    @QueryParam(value = "nearest_train")
    public CreateStationQuery withNearestTrain(Set<UUID> nearestTrain) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    @QueryParam(value = "imminent_train")
    public CreateStationQuery withImminentTrain(Set<UUID> imminentTrain) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

    @QueryParam(value = "present_train")
    public CreateStationQuery withPresentTrain(Set<UUID> presentTrain) {
        return new CreateStationQuery(id, filter, dimension, assembling, nearestTrain, imminentTrain, presentTrain);
    }

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
