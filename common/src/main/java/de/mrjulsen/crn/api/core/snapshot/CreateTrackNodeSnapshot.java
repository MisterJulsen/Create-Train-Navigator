package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.graph.TrackNode;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;

public record CreateTrackNodeSnapshot(
        @ResponseAlwaysInclude int netId,
        TrackNodeLocationSnapshot location
) {

    public static CreateTrackNodeSnapshot of(TrackNode node) {
        return new CreateTrackNodeSnapshot(node.getNetId(), TrackNodeLocationSnapshot.of(node.getLocation()));
    }
}
