package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.api.core.snapshot.CategorySnapshot;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;

import java.util.Set;
import java.util.UUID;

/**
 * A filter over the configured categories. Each field that is set narrows the result, and a field
 * left empty imposes no constraint. Build one from {@link #all()} and the {@code with...} methods;
 * instances are immutable, so each method returns a new query.
 *
 * @param trains If non-empty, keeps only categories run under by one of these trains.
 * @param lines  If non-empty, keeps only categories covering one of these lines.
 */
@QueryModel
public record CategoryQuery(
        Set<UUID> trains,
        Set<UUID> lines
) {

    /** A query that keeps every category, as a starting point for refinement. */
    public static CategoryQuery all() {
        return new CategoryQuery(Set.of(), Set.of());
    }

    /** Keeps only categories run under by one of the given trains. */
    @QueryParam(value = "trains")
    public CategoryQuery withTrains(Set<UUID> trains) {
        return new CategoryQuery(trains, lines);
    }

    /** Keeps only categories covering one of the given lines. */
    @QueryParam(value = "lines")
    public CategoryQuery withLines(Set<UUID> lines) {
        return new CategoryQuery(trains, lines);
    }

    /** Whether the given category passes this query. */
    public boolean accept(CategorySnapshot category) {
        return ModUtils.listContains(lines, category.lines(), (id, v) -> v.id().equals(id)) &&
                ModUtils.listContains(trains, category.trainIds(), (id, v) -> v.equals(id));
    }
}
