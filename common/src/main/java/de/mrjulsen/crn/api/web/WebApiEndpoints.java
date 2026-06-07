package de.mrjulsen.crn.api.web;

import java.util.List;

/** Canonical list of web API endpoints for the in-game UI and documentation. */
public final class WebApiEndpoints {

    public record Endpoint(String path, String description) {}

    private WebApiEndpoints() {}

    public static List<Endpoint> all() {
        return List.of(
            new Endpoint("/health", "Health check"),
            new Endpoint("/api/v1/station-stops", "Trains currently at stations (ETag supported)"),
            new Endpoint("/api/v1/station-stops/events?since=0", "Recent arrival/departure events (JSON poll)"),
            new Endpoint("/api/v1/station-stops/events", "Live SSE stream (Accept: text/event-stream)"),
            new Endpoint("/api/v1/trains", "All trains — schedule, position, line, countdowns"),
            new Endpoint("/api/v1/trains?id=<uuid>", "Single train by UUID"),
            new Endpoint("/api/v1/train?id=<uuid>", "Single train by UUID (alias)"),
            new Endpoint("/api/v1/stations/departures?station=<name>", "Upcoming departures at a station"),
            new Endpoint("/api/v1/lines", "Train lines and categories with colors")
        );
    }
}
