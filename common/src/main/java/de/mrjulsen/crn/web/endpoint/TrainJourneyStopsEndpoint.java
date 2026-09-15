package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.core.TrainManager;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.NotFoundException;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

public class TrainJourneyStopsEndpoint implements IEndpointHandler {

    private enum Timeline {
        ALL,
        PREVIOUS,
        NEXT;
    }

    @Override
    public Response handle(Request request) {
        UUID trainId = request.pathParameter("id", ParamType.UUID);
        JourneySnapshot journey = TrainManager.getInstance().getTrain(trainId).map(JourneySnapshot::of).orElseThrow(() -> new NotFoundException("No train with id " + trainId));
        Timeline timeline = request.query("direction", q -> (q == null || q.isEmpty()) ? Timeline.ALL : Timeline.valueOf(q.toUpperCase(Locale.ROOT))).orElse(Timeline.ALL);
        List<StopSnapshot> stops = switch (timeline) {
            case PREVIOUS -> journey.recentStops();
            case NEXT -> journey.upcomingStops();
            default -> journey.stops();
        };
        return Response.json(stops);
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_TRAINS)
            .summary("List all train stops")
            .description("All stops of one train.")
            .queryParam("direction", "string", "The stops to list (ALL (default), PREVIOUS or NEXT).", Stream.of(Timeline.values()).map(Enum::name).toList(), Timeline.ALL.name())
            .returnsList(StopSnapshot.class)
            .shapeable()
            .notFound("No train with that id exists.")
            .build();
    }
}
