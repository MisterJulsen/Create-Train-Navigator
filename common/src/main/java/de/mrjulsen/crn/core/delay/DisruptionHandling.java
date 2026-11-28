/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.core.delay;

/** Whether a train being out of service should be treated as its own fault. */
public enum DisruptionHandling {

    /** The train was taken out of service on purpose, not because of a fault. */
    DELIBERATE,

    /** The train is out of service because something went wrong. */
    FAULT
}
