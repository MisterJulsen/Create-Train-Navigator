package de.mrjulsen.crn.web;

import de.mrjulsen.crn.api.json.JsonConvert;
import de.mrjulsen.crn.core.navigator.Waypoint;
import de.mrjulsen.crn.web.api.ApiTagRegistry;
import de.mrjulsen.crn.web.api.EndpointRegistry;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.endpoint.*;
import de.mrjulsen.crn.web.endpoint.StationEndpoint;
import de.mrjulsen.crn.web.endpoint.TrainEndpoint;
import de.mrjulsen.crn.web.endpoint.create.*;
import de.mrjulsen.crn.web.endpoint.globalsettings.*;

public final class ModWebFeatures {
    private ModWebFeatures() {}

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

    public static final ApiTagRegistry.ApiTag TAG_COMMON = ApiTagRegistry.register("Common", "Service data, health checks and the API specification.");
    public static final ApiTagRegistry.ApiTag TAG_TRAINS = ApiTagRegistry.register("Trains", "Live and scheduled data for individual trains.");
    public static final ApiTagRegistry.ApiTag TAG_STATIONS = ApiTagRegistry.register("Stations", "Stations known to the backend and what calls at them.");
    public static final ApiTagRegistry.ApiTag TAG_LINES_AND_CATEGORIES = ApiTagRegistry.register("Lines and Categories", "Train lines and categories with the trains using them.");
    public static final ApiTagRegistry.ApiTag TAG_DEPARTURES = ApiTagRegistry.register("Departures", "Station departure boards and departure statistics.");
    public static final ApiTagRegistry.ApiTag TAG_ROUTING = ApiTagRegistry.register("Routing", "Route search between stations.");
    public static final ApiTagRegistry.ApiTag TAG_CREATE = ApiTagRegistry.register("Create", "Raw data from Create, unfiltered by CRN's display rules.");
    public static final ApiTagRegistry.ApiTag TAG_GLOBAL_SETTINGS = ApiTagRegistry.register("Global Settings", "All CRN settings, like lines, categories, station tags and blacklists.");

    public static void init() {
        EndpointRegistry.registerGet("openapi", new OpenApiEndpoint()).alias("openapi.json");
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
        EndpointRegistry.registerGet("create/train/{id}/schedule", new TrainScheduleEndpoint());
        EndpointRegistry.registerGet("create/signals", new AllSignalsEndpoint());
        EndpointRegistry.registerGet("create/signal/{id}", new SignalEndpoint());
        EndpointRegistry.registerGet("create/tracks", new AllTracksEndpoint());

        EndpointRegistry.registerGet("global-settings/blacklist/stations", new StationBlacklistEndpoint());
        EndpointRegistry.registerGet("global-settings/blacklist/trains", new TrainBlacklistEndpoint());
        EndpointRegistry.registerGet("global-settings/train-categories", new TrainCategoriesEndpoint());
        EndpointRegistry.registerGet("global-settings/train-category/{id}", new TrainCategoryEndpoint());
        EndpointRegistry.registerGet("global-settings/train-lines", new TrainLinesEndpoint());
        EndpointRegistry.registerGet("global-settings/train-line/{id}", new TrainLineEndpoint());
        EndpointRegistry.registerGet("global-settings/station-tags", new StationTagsEndpoint());
        EndpointRegistry.registerGet("global-settings/station-tag/{id}", new StationTagEndpoint());
    }
}
