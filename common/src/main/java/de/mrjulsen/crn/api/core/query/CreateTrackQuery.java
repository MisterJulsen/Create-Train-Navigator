package de.mrjulsen.crn.api.core.query;

import com.simibubi.create.content.trains.graph.TrackGraph;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

/**
 * A filter over Create's track graphs, taken straight from the track network and independent of the
 * CRN backend. Each field that is set narrows the result, and a field left empty imposes no
 * constraint. Build one from {@link #all()} and the {@code with...} methods; instances are
 * immutable, so each method returns a new query.
 *
 * @param id        If non-empty, keeps only track graphs with one of these ids.
 * @param dimension If non-empty, restricts each returned graph to the nodes in these dimensions.
 */
@QueryModel
public record CreateTrackQuery(
        Set<UUID> id,
        Set<ResourceLocation> dimension
) {

    /** A query that keeps every track graph, as a starting point for refinement. */
    public static CreateTrackQuery all() {
        return new CreateTrackQuery(Set.of(), Set.of());
    }

    /** Keeps only track graphs with one of the given ids. */
    @QueryParam(value = "id")
    public CreateTrackQuery withId(Set<UUID> id) {
        return new CreateTrackQuery(id, dimension);
    }

    /** Restricts each returned graph to the nodes in the given dimensions. */
    @QueryParam(value = "dimension")
    public CreateTrackQuery withDimension(Set<ResourceLocation> dimension) {
        return new CreateTrackQuery(id, dimension);
    }

    /** Whether the given track graph passes this query. */
    public boolean accept(TrackGraph graph) {
        return id.isEmpty() || id.contains(graph.id);
    }
}
