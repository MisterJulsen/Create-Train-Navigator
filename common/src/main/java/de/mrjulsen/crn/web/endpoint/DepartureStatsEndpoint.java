/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.RequestParams;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.core.history.DepartureStats;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class DepartureStatsEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        String stationName = RequestParams.path(request, "station", ParamType.STRING);
        return ApiResult.json(RailwayBackendApi.getDepartureStats(stationName));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_DEPARTURES)
            .summary("Station departure statistics")
            .description("Summary about the departures recorded at a specific station (e.g. punctually).")
            .returns(DepartureStats.class)
            .shapeable()
            .build();
    }
}
