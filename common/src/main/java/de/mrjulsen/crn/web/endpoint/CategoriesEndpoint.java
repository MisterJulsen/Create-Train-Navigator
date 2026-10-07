package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.CategoryQuery;
import de.mrjulsen.crn.api.core.snapshot.CategorySnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class CategoriesEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        CategoryQuery query = QueryBinder.bind(request, CategoryQuery.class);
        return ApiResult.json(RailwayBackendApi.getAllCategories(query));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_LINES_AND_CATEGORIES)
            .summary("List categories")
            .description("Every category with the trains using it.")
            .query(CategoryQuery.class)
            .returnsList(CategorySnapshot.class)
            .shapeable()
            .build();
    }
}
