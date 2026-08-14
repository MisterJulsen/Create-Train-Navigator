package de.mrjulsen.crn.api.core.query;

import com.simibubi.create.content.trains.signal.SignalBoundary;
import de.mrjulsen.crn.api.core.snapshot.CreateSignalSnapshot;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;

/**
 * A filter over Create's signals, taken straight from the track network and independent of the CRN
 * backend. Each field that is set narrows the result, and a field left empty imposes no constraint.
 * Build one from {@link #all()} and the {@code with...} methods; instances are immutable, so each
 * method returns a new query.
 *
 * @param id        If non-empty, keeps only signals with one of these ids.
 * @param dimension If non-empty, keeps only signals in one of these dimensions.
 * @param group     If non-empty, keeps only signals belonging to one of these signal groups.
 */
@QueryModel
public record CreateSignalQuery(
        Set<UUID> id,
        Set<ResourceLocation> dimension,
        Set<UUID> group
) {

    /** A query that keeps every signal, as a starting point for refinement. */
    public static CreateSignalQuery all() {
        return new CreateSignalQuery(Set.of(), Set.of(), Set.of());
    }

    /** Keeps only signals with one of the given ids. */
    @QueryParam(value = "id")
    public CreateSignalQuery withId(Set<UUID> id) {
        return new CreateSignalQuery(id, dimension, group);
    }

    /** Keeps only signals in one of the given dimensions. */
    @QueryParam(value = "dimension")
    public CreateSignalQuery withDimension(Set<ResourceLocation> dimension) {
        return new CreateSignalQuery(id, dimension, group);
    }

    /** Keeps only signals belonging to one of the given signal groups. */
    @QueryParam(value = "group")
    public CreateSignalQuery withGroup(Set<UUID> group) {
        return new CreateSignalQuery(id, dimension, group);
    }

    /** Whether the given signal passes this query. */
    public boolean accept(SignalBoundary signal) {
        return (id.isEmpty() || id.contains(signal.getId())) &&
                (dimension.isEmpty() || dimension.contains(CreateSignalSnapshot.dimensionOf(signal))) &&
                (group.isEmpty() || group.contains(signal.groups.getFirst()) || group.contains(signal.groups.getSecond()))
                ;
    }
}
