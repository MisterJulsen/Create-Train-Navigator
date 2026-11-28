/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.core.delay;

/** Where a delay reason came from. */
public enum DelayOrigin {

    /** The backend worked the reason out for itself. */
    DETECTED,

    /** The reason was reported from outside through the API. */
    REPORTED
}
