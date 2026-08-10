package de.mrjulsen.crn.web;

import de.mrjulsen.crn.api.json.JsonConvert;
import de.mrjulsen.crn.core.navigator.Waypoint;
import de.mrjulsen.crn.web.api.EndpointRegistry;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.endpoint.*;
import de.mrjulsen.crn.web.endpoint.create.AllSignalsEndpoint;
import de.mrjulsen.crn.web.endpoint.create.AllStationsEndpoint;
import de.mrjulsen.crn.web.endpoint.create.AllTracksEndpoint;
import de.mrjulsen.crn.web.endpoint.create.AllTrainsEndpoint;
import de.mrjulsen.mcdragonlib.data.WorldLocation;

public final class ModWebEndpoints {
    private ModWebEndpoints() {}

    @SuppressWarnings("unused") // Used by auto query binder
    public static final ParamType<Waypoint> WAYPOINT_PARAM = ParamType.register(Waypoint.class, "waypoint", v -> {
        try {
            return JsonConvert.fromJson(v, Waypoint.class);
        } catch (Exception e) {
            String[] split = v.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Invalid waypoint: " + v);
            }
            return new Waypoint(split[0], Long.parseLong(split[1]));
        }
    });

    public static void init() {
        EndpointRegistry.registerGet("ping", new PingEndpoint()).alias("hello");
        EndpointRegistry.registerGet("about", new AboutEndpoint()).alias("info");

        EndpointRegistry.registerGet("navigate", new NavigateEndpoint());
        EndpointRegistry.registerGet("trains", new TrainsEndpoint());
        EndpointRegistry.registerGet("trains/positions", new TrainsPositionsEndpoint());
        EndpointRegistry.registerGet("train/{id}", new TrainEndpoint());
        EndpointRegistry.registerGet("train/{id}/journey", new TrainJourneyEndpoint());
        EndpointRegistry.registerGet("train/{id}/section", new TrainSectionEndpoint());
        EndpointRegistry.registerGet("train/{id}/position", new TrainPositionEndpoint());
        EndpointRegistry.registerGet("train/{id}/composition", new TrainCompositionEndpoint());
        EndpointRegistry.registerGet("train/{id}/stops", new TrainJourneyStopsEndpoint());
        EndpointRegistry.registerGet("train/{id}/stop", new TrainJourneyStopEndpoint());
        EndpointRegistry.registerGet("train/{id}/delay-report", new TrainDelayReportEndpoint());
        EndpointRegistry.registerGet("board/{station}", new BoardEndpoint());
        EndpointRegistry.registerGet("lines", new LinesEndpoint());
        EndpointRegistry.registerGet("line/{id}", new LineEndpoint());
        EndpointRegistry.registerGet("categories", new CategoriesEndpoint());
        EndpointRegistry.registerGet("category/{id}", new CategoryEndpoint());
        EndpointRegistry.registerGet("stations", new StationsEndpoint());
        EndpointRegistry.registerGet("station/{name}", new StationEndpoint());
        EndpointRegistry.registerGet("departure-stats/{station}", new DepartureStatsEndpoint()).alias("departure-history/{station}");
        EndpointRegistry.registerGet("time", new TimeNowEndpoint()).alias("now");
        EndpointRegistry.registerGet("backend-stats", new TrainManagerStatsEndpoint());

        EndpointRegistry.registerGet("create/stations", new AllStationsEndpoint());
        EndpointRegistry.registerGet("create/station/{id}", new de.mrjulsen.crn.web.endpoint.create.StationEndpoint());
        EndpointRegistry.registerGet("create/trains", new AllTrainsEndpoint());
        EndpointRegistry.registerGet("create/train/{id}", new de.mrjulsen.crn.web.endpoint.create.TrainEndpoint());
        EndpointRegistry.registerGet("create/signals", new AllSignalsEndpoint());
        EndpointRegistry.registerGet("create/signal/{id}", new de.mrjulsen.crn.web.endpoint.create.SignalEndpoint());
        EndpointRegistry.registerGet("create/tracks", new AllTracksEndpoint());
    }
}
