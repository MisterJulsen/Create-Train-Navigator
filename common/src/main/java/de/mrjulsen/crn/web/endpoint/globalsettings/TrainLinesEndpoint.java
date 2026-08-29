package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.data.settings.TrainLine;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.util.Set;
import java.util.UUID;

public class TrainLinesEndpoint implements IEndpointHandler {


    @QueryModel
    private record Query(
            @QueryParam("name") Set<String> lineName,
            @QueryParam("id") Set<UUID> id,
            @QueryParam("by") Set<String> owner,
            @QueryParam("by_id") Set<UUID> ownerId,
            @QueryParam("trusted") Set<String> trustedPlayerName,
            @QueryParam("trusted_id") Set<UUID> trustedPlayerId
    ) {

        public Query {
            lineName = lineName == null ? Set.of() : Set.copyOf(lineName);
            id = id == null ? Set.of() : Set.copyOf(id);
            owner = owner == null ? Set.of() : Set.copyOf(owner);
            ownerId = ownerId == null ? Set.of() : Set.copyOf(ownerId);
            trustedPlayerName = trustedPlayerName == null ? Set.of() : Set.copyOf(trustedPlayerName);
            trustedPlayerId = trustedPlayerId == null ? Set.of() : Set.copyOf(trustedPlayerId);
        }

        public boolean accept(TrainLine tag) {
            return (lineName.isEmpty() || lineName.contains(tag.getLineName())) &&
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
        return Response.json(GlobalSettings.getInstance().getAllTrainLines().stream().filter(query::accept).toList());
    }
}