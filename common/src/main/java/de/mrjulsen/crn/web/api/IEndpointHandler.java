package de.mrjulsen.crn.web.api;

@FunctionalInterface
public interface IEndpointHandler {
    Response handle(Request request) throws Exception;
}
