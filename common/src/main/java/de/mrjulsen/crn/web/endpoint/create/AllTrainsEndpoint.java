package de.mrjulsen.crn.web.endpoint.create;

import de.mrjulsen.crn.api.core.query.CreateTrainQuery;
import de.mrjulsen.crn.api.core.snapshot.CreateTrainSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

public class AllTrainsEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        CreateTrainQuery query = QueryBinder.bind(request, CreateTrainQuery.class);
        return Response.json(TrainUtils.getAllTrains(false).stream().filter(query::accept).map(CreateTrainSnapshot::of).toList());
    }
}
