package de.mrjulsen.crn.api.core.query;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.api.core.snapshot.CreateTrainSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

/**
 * A filter over Create's trains, taken straight from the train entities and independent of the CRN
 * backend. Each field that is set narrows the result, and a field left empty or {@code null} imposes
 * no constraint. Build one from {@link #all()} and the {@code with...} methods; instances are
 * immutable, so each method returns a new query.
 *
 * @param id        If non-empty, keeps only trains with one of these ids.
 * @param filter    If non-empty, keeps only trains whose name matches this filter, which may use the
 *                  schedule's wildcard syntax.
 * @param dimension If non-empty, keeps only trains in one of these dimensions.
 * @param graph     If non-empty, keeps only trains on one of these track graphs.
 * @param derailed  If set, keeps only trains that are, or are not, derailed.
 */
@QueryModel
public record CreateTrainQuery(
        Set<UUID> id,
        String filter,
        Set<ResourceLocation> dimension,
        Set<UUID> graph,
        Boolean derailed
) {

    /** A query that keeps every train, as a starting point for refinement. */
    public static CreateTrainQuery all() {
        return new CreateTrainQuery(Set.of(), "", Set.of(), Set.of(), null);
    }

    /** Keeps only trains with one of the given ids. */
    @QueryParam(value = "id")
    public CreateTrainQuery withId(Set<UUID> id) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    /** Keeps only trains whose name matches the given filter. */
    @QueryParam(value = "filter")
    public CreateTrainQuery withFilter(String filter) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    /** Keeps only trains in one of the given dimensions. */
    @QueryParam(value = "dimension")
    public CreateTrainQuery withDimension(Set<ResourceLocation> dimension) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    /** Keeps only trains on one of the given track graphs. */
    @QueryParam(value = "graph")
    public CreateTrainQuery withGraph(Set<UUID> graph) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    /** Keeps only trains that are, or are not, derailed. */
    @QueryParam(value = "derailed")
    public CreateTrainQuery withDerailed(Boolean derailed) {
        return new CreateTrainQuery(id, filter, dimension, graph, derailed);
    }

    /** Whether the given train passes this query. */
    public boolean accept(Train train) {
        return (id.isEmpty() || id.contains(train.id)) &&
                (filter.isEmpty() || TrainUtils.stationMatches(train.name.getString(), filter)) &&
                (dimension.isEmpty() || dimension.contains(CreateTrainSnapshot.dimensionOf(train))) &&
                (graph.isEmpty() || (train.graph != null && graph.contains(train.graph.id))) &&
                (derailed == null || train.derailed == derailed)
                ;
    }
}
