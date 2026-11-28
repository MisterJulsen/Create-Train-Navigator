/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

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
