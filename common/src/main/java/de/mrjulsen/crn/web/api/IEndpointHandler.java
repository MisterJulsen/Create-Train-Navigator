package de.mrjulsen.crn.web.api;

import org.eclipse.jetty.server.Request;

import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

@FunctionalInterface
public interface IEndpointHandler {

    Object handle(Request request) throws Exception;

    default EndpointDocumentation getDocumentation() {
        return null;
    }
}
