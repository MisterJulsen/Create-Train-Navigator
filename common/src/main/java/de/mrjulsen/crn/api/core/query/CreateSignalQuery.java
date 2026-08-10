package de.mrjulsen.crn.api.core.query;

import com.simibubi.create.content.trains.signal.SignalBoundary;
import de.mrjulsen.crn.api.core.snapshot.CreateSignalSnapshot;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

@QueryModel
public record CreateSignalQuery(
        Set<UUID> id,
        Set<ResourceLocation> dimension,
        Set<UUID> group
) {

    public static CreateSignalQuery all() {
        return new CreateSignalQuery(Set.of(), Set.of(), Set.of());
    }

    @QueryParam(value = "id")
    public CreateSignalQuery withId(Set<UUID> id) {
        return new CreateSignalQuery(id, dimension, group);
    }

    @QueryParam(value = "dimension")
    public CreateSignalQuery withDimension(Set<ResourceLocation> dimension) {
        return new CreateSignalQuery(id, dimension, group);
    }

    @QueryParam(value = "group")
    public CreateSignalQuery withGroup(Set<UUID> group) {
        return new CreateSignalQuery(id, dimension, group);
    }

    public boolean accept(SignalBoundary signal) {
        return (id.isEmpty() || id.contains(signal.getId())) &&
                (dimension.isEmpty() || dimension.contains(CreateSignalSnapshot.dimensionOf(signal))) &&
                (group.isEmpty() || group.contains(signal.groups.getFirst()) || group.contains(signal.groups.getSecond()))
                ;
    }
}
