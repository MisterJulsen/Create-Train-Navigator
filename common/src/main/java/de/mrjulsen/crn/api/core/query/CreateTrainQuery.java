package de.mrjulsen.crn.api.core.query;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.api.core.snapshot.CreateTrainSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

@QueryModel
public record CreateTrainQuery(
        Set<UUID> id,
        String filter,
        Set<ResourceLocation> dimension,
        Set<UUID> graph,
        Boolean derailed
) {

    public static CreateTrainQuery all() {
        return new CreateTrainQuery(Set.of(), "", Set.of(), Set.of(), null);
    }

    @QueryParam(value = "id")
    public CreateTrainQuery withId(Set<UUID> id) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    @QueryParam(value = "filter")
    public CreateTrainQuery withFilter(String filter) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    @QueryParam(value = "dimension")
    public CreateTrainQuery withDimension(Set<ResourceLocation> dimension) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    @QueryParam(value = "graph")
    public CreateTrainQuery withGraph(Set<UUID> graph) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    @QueryParam(value = "derailed")
    public CreateTrainQuery withDerailed(Boolean derailed) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    public boolean accept(Train train) {
        return (id.isEmpty() || id.contains(train.id)) &&
                (filter.isEmpty() || TrainUtils.stationMatches(train.name.getString(), filter)) &&
                (dimension.isEmpty() || dimension.contains(CreateTrainSnapshot.dimensionOf(train))) &&
                (graph.isEmpty() || (train.graph != null && graph.contains(train.graph.id))) &&
                (derailed == null || train.derailed == derailed)
                ;
    }
}
