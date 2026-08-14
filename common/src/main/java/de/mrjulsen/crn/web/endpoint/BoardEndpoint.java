package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.BoardQuery;
import de.mrjulsen.crn.api.core.snapshot.BoardEntry;
import de.mrjulsen.crn.web.api.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BoardEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        String station = request.pathParameter("station", ParamType.STRING);
        BoardQuery query = QueryBinder.bind(request, () -> BoardQuery.defaults().withDuplicates(true));

        List<BoardEntry> entries = new ArrayList<>(RailwayBackendApi.getBoard(station, query));
        entries.sort(Comparator.comparingLong(x -> x.realtime().arrival()));
        return Response.json(entries);
    }
}
