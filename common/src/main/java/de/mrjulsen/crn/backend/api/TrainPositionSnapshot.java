package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * An immutable snapshot of where a train is and how fast it is moving.
 * <p>
 * Deliberately small, so a consumer tracking many trains at once - a live map, say - can poll
 * positions without building a full {@link TrainSnapshot} per train.
 *
 * @param trainId       The id of the train.
 * @param dimension     The dimension the train's leading carriage is in, or {@code null} if the
 *                      train is not loaded anywhere.
 * @param position      The position of the leading carriage, or {@code null} if it is not loaded.
 * @param dimensions    Every dimension the train currently occupies. Usually one, more while it
 *                      spans a portal.
 * @param speed         The current speed in blocks per tick. Negative when running backwards.
 * @param targetSpeed   The speed the train is currently accelerating or braking towards.
 * @param maxSpeed      The highest speed this train could reach, in blocks per tick.
 * @param throttle      How much of its maximum speed the train is currently allowed to use, from
 *                      {@code 0} to {@code 1}.
 * @param backwards     Whether the train is running backwards along its carriage order.
 * @param distanceToNextStop The remaining distance to the train's current destination in blocks, or
 *                      {@code -1} if it is not navigating anywhere.
 */
public record TrainPositionSnapshot(
    UUID trainId,
    ResourceLocation dimension,
    BlockPos position,
    List<ResourceLocation> dimensions,
    double speed,
    double targetSpeed,
    double maxSpeed,
    double throttle,
    boolean backwards,
    double distanceToNextStop
) {

    /** A snapshot for a train whose position cannot be determined. */
    public static TrainPositionSnapshot unknown(UUID trainId) {
        return new TrainPositionSnapshot(trainId, null, null, List.of(), 0, 0, 0, 0, false, -1);
    }

    public TrainPositionSnapshot {
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
    }

    /** Captures the position of the given train. Server thread only. */
    public static TrainPositionSnapshot of(Train train) {
        Carriage leading = train.carriages == null || train.carriages.isEmpty() ? null : train.carriages.get(0);
        if (leading == null) {
            return unknown(train.id);
        }

        List<ResourceKey<Level>> present = leading.getPresentDimensions();
        ResourceLocation dimension = present.isEmpty() ? null : present.get(0).location();
        BlockPos position = present.isEmpty() ? null : leading.getPositionInDimension(present.get(0)).orElse(null);

        return new TrainPositionSnapshot(
            train.id,
            dimension,
            position,
            present.stream().map(ResourceKey::location).toList(),
            train.speed,
            train.targetSpeed,
            train.maxSpeed(),
            train.throttle,
            train.currentlyBackwards,
            train.navigation == null || train.navigation.destination == null ? -1 : train.navigation.distanceToDestination
        );
    }

    /** Whether the train's position is currently known. */
    public boolean isLoaded() {
        return position != null;
    }

    /** Whether the train is moving at all. */
    public boolean isMoving() {
        return Math.abs(speed) > 0.001;
    }

    /** The current speed as a fraction of the train's maximum, from {@code 0} to {@code 1}. */
    public double speedFraction() {
        return maxSpeed <= 0 ? 0 : Math.min(1, Math.abs(speed) / maxSpeed);
    }
}
