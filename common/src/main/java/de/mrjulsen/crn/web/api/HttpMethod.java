/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

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
