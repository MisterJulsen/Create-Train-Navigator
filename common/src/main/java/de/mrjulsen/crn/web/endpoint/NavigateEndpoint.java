package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.core.navigator.NavigationQuery;
import de.mrjulsen.crn.core.navigator.Navigator;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

public class NavigateEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        NavigationQuery query = QueryBinder.bind(request, NavigationQuery.class);
        return Response.json(Navigator.search(query));
    }
}
