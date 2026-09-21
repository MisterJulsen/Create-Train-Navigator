package de.mrjulsen.crn.web.endpoint;

import org.eclipse.jetty.server.Request;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.BoardQuery;
import de.mrjulsen.crn.api.core.snapshot.BoardEntry;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.*;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BoardEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        String station = RequestParams.path(request, "station", ParamType.STRING);
        BoardQuery query = QueryBinder.bind(request, () -> BoardQuery.defaults().withDuplicates(true));

        List<BoardEntry> entries = new ArrayList<>(RailwayBackendApi.getBoard(station, query));
        entries.sort(Comparator.comparingLong(x -> x.realtime().arrival()));
        return ApiResult.json(entries);
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_DEPARTURES)
            .summary("Station board")
            .description("The calls at a station, ordered by arrival, just like a departure board would show them.")
            .query(BoardQuery.class)
            .returnsList(BoardEntry.class)
            .shapeable()
            .build();
    }
}
