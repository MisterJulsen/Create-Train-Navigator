/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.graph.TrackNode;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;

/**
 * A plain data view of one node in Create's track graph.
 *
 * @param netId    The node's network id, unique within its graph.
 * @param location Where the node sits in the world.
 */
public record CreateTrackNodeSnapshot(
        @ResponseAlwaysInclude int netId,
        TrackNodeLocationSnapshot location
) {

    public static CreateTrackNodeSnapshot of(TrackNode node) {
        return new CreateTrackNodeSnapshot(node.getNetId(), TrackNodeLocationSnapshot.of(node.getLocation()));
    }
}
