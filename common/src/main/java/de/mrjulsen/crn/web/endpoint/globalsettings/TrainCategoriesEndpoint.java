package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.data.settings.TrainCategory;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.web.annotation.OpenApiDescription;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.Set;
import java.util.UUID;

public class TrainCategoriesEndpoint implements IEndpointHandler {


    @QueryModel
    private record Query(
            @QueryParam("name") @OpenApiDescription("Keep only categories with one of these names.") Set<String> categoryName,
            @QueryParam("id") @OpenApiDescription("Keep only categories with one of these ids.") Set<UUID> id,
            @QueryParam("by") @OpenApiDescription("Keep only categories owned by one of these players (by name).") Set<String> owner,
            @QueryParam("by_id") @OpenApiDescription("Keep only categories owned by one of these players (by UUID).") Set<UUID> ownerId,
            @QueryParam("trusted") @OpenApiDescription("Keep only categories that have one of these players as trusted members (by name).") Set<String> trustedPlayerName,
            @QueryParam("trusted_id") @OpenApiDescription("Keep only categories that have one of these players as trusted members (by UUID).") Set<UUID> trustedPlayerId
    ) {

        public Query {
            categoryName = categoryName == null ? Set.of() : Set.copyOf(categoryName);
            id = id == null ? Set.of() : Set.copyOf(id);
            owner = owner == null ? Set.of() : Set.copyOf(owner);
            ownerId = ownerId == null ? Set.of() : Set.copyOf(ownerId);
            trustedPlayerName = trustedPlayerName == null ? Set.of() : Set.copyOf(trustedPlayerName);
            trustedPlayerId = trustedPlayerId == null ? Set.of() : Set.copyOf(trustedPlayerId);
        }

        public boolean accept(TrainCategory tag) {
            return (categoryName.isEmpty() || categoryName.contains(tag.getCategoryName())) &&
                    ModUtils.listContainsElement(tag.getId(), id, true, UUID::equals) &&
                    ModUtils.listContainsElement(tag.getOwner().getOwner(), owner, true, (a, b) -> a.map(x -> x.name().equals(b)).orElse(false)) &&
                    ModUtils.listContainsElement(tag.getOwner().getOwner(), ownerId, true, (a, b) -> a.map(x -> x.uuid().equals(b)).orElse(false)) &&
                    ModUtils.listContainsAny(tag.getOwner().getTrusted(), trustedPlayerName, true, true, (a, b) -> a.name().equals(b)) &&
                    ModUtils.listContainsAny(tag.getOwner().getTrusted(), trustedPlayerId, true, true, (a, b) -> a.uuid().equals(b))
                    ;
        }
    }

    @Override
    public Response handle(Request request) {
        Query query = QueryBinder.bind(request, Query.class);
        return Response.json(GlobalSettings.getInstance().getAllTrainCategories().stream().filter(query::accept).toList());
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag("Global Settings")
            .summary("List train categories")
            .description("All train categories matching the given query values.")
            .query(Query.class)
            .returnsList(TrainCategory.class)
            .shapeable()
            .build();
    }
}
