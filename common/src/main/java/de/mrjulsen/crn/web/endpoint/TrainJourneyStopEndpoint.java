package de.mrjulsen.crn.web.endpoint;

import com.google.common.base.Suppliers;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

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
}
