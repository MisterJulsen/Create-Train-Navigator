package de.mrjulsen.crn.backend.api;

import java.util.List;

import com.simibubi.create.content.trains.entity.Carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * An immutable snapshot of one carriage of a train, including where it is and how large it is, so
 * a consumer can draw the train to scale on a map or list its composition.
 *
 * @param index          The position of this carriage in the train, counting from the front.
 * @param lengthBlocks   The distance between this carriage's bogeys in blocks, i.e. how much track
 *                       it occupies. {@code 0} for a carriage riding on a single bogey.
 * @param onTwoBogeys    Whether the carriage rides on two bogeys rather than one.
 * @param dimension      The dimension the carriage is currently in, or {@code null} if it is not
 *                       loaded anywhere.
 * @param position       The carriage's position, or {@code null} if it is not currently loaded.
 * @param blocked        Whether the carriage cannot move because something is in the way.
 * @param stalled        Whether the carriage is stalled, e.g. under excessive stress.
 * @param hasConductor   Whether a conductor is present on this carriage.
 * @param hasStorage     Whether this carriage carries cargo storage.
 * @param inMultipleDimensions Whether the carriage currently spans a portal.
 */
public record CarriageSnapshot(
    int index,
    int lengthBlocks,
    boolean onTwoBogeys,
    ResourceLocation dimension,
    BlockPos position,
    boolean blocked,
    boolean stalled,
    boolean hasConductor,
    boolean hasStorage,
    boolean inMultipleDimensions
) {

    /** Captures the given carriage. Server thread only, as it reads the live train state. */
    public static CarriageSnapshot of(Carriage carriage, int index) {
        ResourceLocation dimension = null;
        BlockPos position = null;

        List<ResourceKey<Level>> dimensions = carriage.getPresentDimensions();
        if (!dimensions.isEmpty()) {
            ResourceKey<Level> key = dimensions.get(0);
            dimension = key.location();
            position = carriage.getPositionInDimension(key).orElse(null);
        }

        return new CarriageSnapshot(
            index,
            carriage.bogeySpacing,
            carriage.isOnTwoBogeys(),
            dimension,
            position,
            carriage.blocked,
            carriage.stalled,
            carriage.presentConductors != null && (carriage.presentConductors.getFirst() || carriage.presentConductors.getSecond()),
            carriage.storage != null,
            carriage.presentInMultipleDimensions()
        );
    }

    /** Whether this carriage's position is currently known. */
    public boolean isLoaded() {
        return position != null;
    }
}
