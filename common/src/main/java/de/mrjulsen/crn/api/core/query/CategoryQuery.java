package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.api.core.snapshot.CategorySnapshot;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;

import java.util.Set;
import java.util.UUID;

@QueryModel
public record CategoryQuery(
        Set<UUID> trains,
        Set<UUID> lines
) {

    public static CategoryQuery all() {
        return new CategoryQuery(Set.of(), Set.of());
    }

    @QueryParam(value = "trains")
    public CategoryQuery withTrains(Set<UUID> trains) {
        return new CategoryQuery(trains, lines);
    }

    @QueryParam(value = "lines")
    public CategoryQuery withLines(Set<UUID> lines) {
        return new CategoryQuery(trains, lines);
    }


    public boolean accept(CategorySnapshot category) {
        return ModUtils.listContains(lines, category.lines(), (id, v) -> v.id().equals(id)) &&
                ModUtils.listContains(trains, category.trainIds(), (id, v) -> v.equals(id));
    }
}
