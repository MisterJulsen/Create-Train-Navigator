/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.exceptions;

public class RuntimeSideException extends RuntimeException {
    public RuntimeSideException(boolean expectClient) {
        super("This method can only be called on the " + (expectClient ? "client" : "server") + " side!");
    }
}
