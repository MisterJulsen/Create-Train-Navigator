package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A plain data view of one of Create's signals, taken straight from the track network.
 *
 * @param id             The signal's id.
 * @param edgeLocation   The two track nodes of the edge the signal sits on.
 * @param position       How far along that edge the signal sits.
 * @param dimension      The dimension the signal is in.
 * @param types          The signal type on each side of the boundary.
 * @param groups         The id of the signal group on each side of the boundary.
 * @param states         The current signal state on each side of the boundary.
 * @param blockPositions The block positions of the signal's parts in the world.
 */
public record CreateSignalSnapshot(
        @ResponseAlwaysInclude UUID id,
        TrackNodeLocationSnapshot[] edgeLocation,
        double position,
        ResourceLocation dimension,
        String[] types,
        UUID[] groups,
        String[] states,
        List<BlockPos> blockPositions
) {

    public static CreateSignalSnapshot of(SignalBoundary signal) {
        Couple<TrackNodeLocation> edge = signal.edgeLocation;
        TrackNodeLocation first = edge == null ? null : edge.getFirst();
        TrackNodeLocation second = edge == null ? null : edge.getSecond();
        return new CreateSignalSnapshot(
                signal.getId(),
                new TrackNodeLocationSnapshot[] {
                        first == null ? null : TrackNodeLocationSnapshot.of(first),
                        second == null ? null : TrackNodeLocationSnapshot.of(second)
                },
                signal.position,
                dimensionOf(signal),
                new String[] { signal.types.getFirst().name(), signal.types.getSecond().name() },
                new UUID[] { signal.groups.getFirst(), signal.groups.getSecond() },
                new String[] { signal.cachedStates.getFirst().name(), signal.cachedStates.getSecond().name() },
                blockPositionsOf(signal)
        );
    }

    /** The dimension the signal sits in, or {@code null} if it cannot be determined. */
    public static ResourceLocation dimensionOf(SignalBoundary signal) {
        if (signal.edgeLocation == null || signal.edgeLocation.getFirst() == null) {
            return null;
        }
        return signal.edgeLocation.getFirst().getDimension().location();
    }

    private static List<BlockPos> blockPositionsOf(SignalBoundary signal) {
        List<BlockPos> positions = new ArrayList<>();
        signal.blockEntities.forEach(side -> positions.addAll(side.keySet()));
        return positions;
    }
}
