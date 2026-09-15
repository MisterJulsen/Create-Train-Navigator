package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.core.navigator.NavigationQuery;
import de.mrjulsen.crn.core.navigator.NavigationResult;
import de.mrjulsen.crn.core.navigator.Navigator;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.BadRequestException;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.NotFoundException;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class NavigateEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        NavigationQuery query = QueryBinder.bind(request, NavigationQuery.class);
        NavigationResult result = Navigator.search(query);
        switch (result.status()) {
            case INCOMPLETE_QUERY -> throw new BadRequestException("The query is missing an origin or a destination.");
            case UNKNOWN_STATION -> throw new NotFoundException("An origin, destination or waypoint named a station that does not exist.");
            default -> { }
        }
        return Response.json(result);
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_ROUTING)
            .summary("Search for routes")
            .description("Runs a route search between two stations and returns possible connection journeys. All times are in game ticks. A 200 response still carries a status that says whether journeys were found (OK) or why not (NO_ROUTE, NO_DIRECT_ROUTE, SAME_STATION or BACKEND_INACTIVE); an invalid query or an unknown station is reported as a 400 or 404 instead.")
            .query(NavigationQuery.class)
            .returns(NavigationResult.class)
            .shapeable()
            .badRequest("A required parameter is missing or empty, or a parameter could not be parsed.")
            .notFound("A station named in the query (origin, destination or a waypoint) does not exist.")
            .build();
    }
}
