package de.mrjulsen.crn.web.api;

import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

@FunctionalInterface
public interface IEndpointHandler {

    Response handle(Request request) throws Exception;

    default EndpointDocumentation getDocumentation() {
        return null;
    }
}
