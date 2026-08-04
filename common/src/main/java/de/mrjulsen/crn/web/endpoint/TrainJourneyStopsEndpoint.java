package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.core.TrainManager;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TrainJourneyStopsEndpoint implements IEndpointHandler {

    private enum Timeline {
        ALL,
        PREVIOUS,
        NEXT;
    }

    @Override
    public Response handle(Request request) {
        UUID trainId = request.pathParameter("id", ParamType.UUID);
        JourneySnapshot journey = TrainManager.getInstance().getTrain(trainId).map(JourneySnapshot::of).orElseThrow();
        Timeline timeline = request.query("direction", q -> (q == null || q.isEmpty()) ? Timeline.ALL : Timeline.valueOf(q.toUpperCase(Locale.ROOT))).orElse(Timeline.ALL);
        List<StopSnapshot> stops = switch (timeline) {
            case PREVIOUS -> journey.recentStops();
            case NEXT -> journey.upcomingStops();
            default -> journey.stops();
        };
        return Response.json(stops);
    }
}
