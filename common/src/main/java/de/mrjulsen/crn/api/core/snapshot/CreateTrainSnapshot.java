package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * A plain data view of one of Create's trains, taken straight from the train entity.
 *
 * @param id             The train's id.
 * @param name           The train's name.
 * @param owner          The id of the player who owns the train, or {@code null}.
 * @param graph          The id of the track graph the train is on, or {@code null}.
 * @param dimension      The dimension the train's leading point is in, or {@code null}.
 * @param position       Where the train's leading point is, or {@code null} if it cannot be located.
 * @param speed          The train's current speed, in blocks per tick.
 * @param backwards      Whether the train is running in reverse.
 * @param derailed       Whether the train has derailed.
 * @param status         Create's own status flags for the train.
 * @param carriages      The train's carriages, ordered from the front.
 * @param currentStation The id of the station the train is at, or {@code null}.
 */
public record CreateTrainSnapshot(
        @ResponseAlwaysInclude UUID id,
        String name,
        UUID owner,
        UUID graph,
        ResourceLocation dimension,
        Vec3Snapshot position,
        double speed,
        boolean backwards,
        boolean derailed,
        CreateTrainStatusSnapshot status,
        List<CreateCarriageSnapshot> carriages,
        UUID currentStation
) {

    public static CreateTrainSnapshot of(Train train) {
        GlobalStation currentStation = train.getCurrentStation();
        return new CreateTrainSnapshot(
                train.id,
                train.name.getString(),
                train.owner,
                train.graph == null ? null : train.graph.id,
                dimensionOf(train),
                Vec3Snapshot.of(worldPositionOf(train)),
                train.speed,
                train.currentlyBackwards,
                train.derailed,
                CreateTrainStatusSnapshot.of(train),
                train.carriages.stream().map(carriage -> CreateCarriageSnapshot.of(carriage, train.graph)).toList(),
                currentStation == null ? null : currentStation.id
        );
    }

    private static TravellingPoint leadingPointOf(Train train) {
        if (train.graph == null || train.carriages.isEmpty()) {
            return null;
        }
        return train.carriages.get(0).getLeadingPoint();
    }

    private static Vec3 worldPositionOf(Train train) {
        TravellingPoint point = leadingPointOf(train);
        if (point == null || point.edge == null) {
            return null;
        }
        return point.getPosition(train.graph);
    }

    /** The dimension the train's leading point sits in, or {@code null} if it cannot be determined. */
    public static ResourceLocation dimensionOf(Train train) {
        TravellingPoint point = leadingPointOf(train);
        if (point == null || point.node1 == null) {
            return null;
        }
        return point.node1.getLocation().getDimension().location();
    }
}
