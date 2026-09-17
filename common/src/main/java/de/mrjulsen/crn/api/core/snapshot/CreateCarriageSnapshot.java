package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.minecraft.world.phys.Vec3;

/**
 * A plain data view of one carriage in Create's train network.
 *
 * @param id           The carriage's id.
 * @param leading      Where the carriage's leading point is, or {@code null} if it cannot be located.
 * @param trailing     Where the carriage's trailing point is, or {@code null} if it cannot be located.
 * @param length       The distance between the leading and trailing points, in blocks.
 * @param bogeySpacing The spacing between the carriage's bogeys, in blocks.
 */
public record CreateCarriageSnapshot(
        int id,
        Vec3Snapshot leading,
        Vec3Snapshot trailing,
        double length,
        int bogeySpacing
) {

    public static CreateCarriageSnapshot of(Carriage carriage, TrackGraph graph) {
        Vec3 leading = positionOf(carriage.getLeadingPoint(), graph);
        Vec3 trailing = positionOf(carriage.getTrailingPoint(), graph);
        double length = leading != null && trailing != null ? leading.distanceTo(trailing) : 0;
        return new CreateCarriageSnapshot(carriage.id, Vec3Snapshot.of(leading), Vec3Snapshot.of(trailing), length, carriage.bogeySpacing);
    }

    private static Vec3 positionOf(TravellingPoint point, TrackGraph graph) {
        return graph == null || point == null || point.edge == null ? null : point.getPosition(graph);
    }
}
