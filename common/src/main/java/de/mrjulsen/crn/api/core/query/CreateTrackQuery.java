package de.mrjulsen.crn.api.core.query;

import com.simibubi.create.content.trains.graph.TrackGraph;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

@QueryModel
public record CreateTrackQuery(
        Set<UUID> id,
        Set<ResourceLocation> dimension
) {

    public static CreateTrackQuery all() {
        return new CreateTrackQuery(Set.of(), Set.of());
    }

    @QueryParam(value = "id")
    public CreateTrackQuery withId(Set<UUID> id) {
        return new CreateTrackQuery(id, dimension);
    }

    @QueryParam(value = "dimension")
    public CreateTrackQuery withDimension(Set<ResourceLocation> dimension) {
        return new CreateTrackQuery(id, dimension);
    }

    public boolean accept(TrackGraph graph) {
        return id.isEmpty() || id.contains(graph.id);
    }
}
