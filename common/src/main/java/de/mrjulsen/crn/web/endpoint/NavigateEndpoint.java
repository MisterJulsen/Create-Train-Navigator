package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.core.navigator.NavigationQuery;
import de.mrjulsen.crn.core.navigator.NavigationResult;
import de.mrjulsen.crn.core.navigator.Navigator;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class NavigateEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        NavigationQuery query = QueryBinder.bind(request, NavigationQuery.class);
        return Response.json(Navigator.search(query));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag("Routing")
            .summary("Search for routes")
            .description("Runs a route search between two stations and returns possible connection journeys. All times are in game ticks.")
            .query(NavigationQuery.class)
            .returns(NavigationResult.class)
            .shapeable()
            .build();
    }
}
