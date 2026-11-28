/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.web.openapi;

import java.util.List;

public record RequestBodySpec(List<ContentSpec> contents, boolean required, String description) {

    public RequestBodySpec {
        contents = List.copyOf(contents);
    }

    public static RequestBodySpec json(Class<?> type) {
        return new RequestBodySpec(List.of(ContentSpec.json(SchemaSpec.of(type))), true, null);
    }

    public static RequestBodySpec of(boolean required, String description, ContentSpec... contents) {
        return new RequestBodySpec(List.of(contents), required, description);
    }
}
