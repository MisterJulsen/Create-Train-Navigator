/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.web.api;

public final class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
