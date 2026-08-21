package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

public class TrainLinesEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        return Response.json(GlobalSettings.getInstance().getAllTrainLines());
    }
}
