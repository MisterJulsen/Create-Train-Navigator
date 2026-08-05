package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.CategoryQuery;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

public class CategoriesEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        CategoryQuery query = QueryBinder.bind(request, CategoryQuery.class);
        return Response.json(RailwayBackendApi.getAllCategories(query));
    }
}
