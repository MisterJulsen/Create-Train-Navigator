package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.core.TrainManager;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class TrainJourneyStopEndpoint implements IEndpointHandler {

    private enum Timeline {
        PREVIOUS,
        CURRENT,
        NEXT;
    }

    @Override
    public Response handle(Request request) {
        UUID trainId = request.pathParameter("id", ParamType.UUID);
        Timeline timeline = request.query("direction", q -> (q == null || q.isEmpty()) ? Timeline.CURRENT : Timeline.valueOf(q.toUpperCase(Locale.ROOT))).orElse(Timeline.CURRENT);
        Optional<String> station = request.query("station", ParamType.STRING);

        JourneySnapshot journey = TrainManager.getInstance().getTrain(trainId).map(JourneySnapshot::of).orElseThrow();
        Optional<StopSnapshot> stop = switch (timeline) {
            case PREVIOUS -> station.map(journey::previousCallAt).orElse(journey.lastStop());
            case NEXT -> station.map(q -> journey.nextCallAt(q, RailwayBackendApi.getCurrentTime())).orElse(journey.nextStop());
            default -> journey.currentStop();
        };
        return Response.json(stop.orElse(null));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_TRAINS)
            .summary("Get one train stop")
            .description("A single stop of a train.")
            .queryParam("direction", "string", "The stop to return (PREVIOUS, CURRENT (default) or NEXT).", Stream.of(Timeline.values()).map(Enum::name).toList(), Timeline.CURRENT.name())
            .queryParam("station", "string", "The stop to be searched for in the selected direction, instead of the next one.")
            .returns(StopSnapshot.class)
            .shapeable()
            .build();
    }
}
