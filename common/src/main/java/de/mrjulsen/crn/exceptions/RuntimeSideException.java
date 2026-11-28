/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.exceptions;

public class RuntimeSideException extends RuntimeException {
    public RuntimeSideException(boolean expectClient) {
        super("This method can only be called on the " + (expectClient ? "client" : "server") + " side!");
    }
}
