package de.mrjulsen.crn.web.api;

import java.util.Optional;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS;

    public static Optional<HttpMethod> fromName(String name) {
        for (HttpMethod method : values()) {
            if (method.name().equalsIgnoreCase(name)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }
}
