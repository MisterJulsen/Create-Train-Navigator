package de.mrjulsen.crn.client.ber;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;

import de.mrjulsen.crn.block.penalty.PenaltyAnchor;
import de.mrjulsen.crn.block.penalty.PenaltyAnchorBlockEntity;
import de.mrjulsen.crn.client.ModPartials;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PenaltyAnchorVisual extends AbstractBlockEntityVisual<PenaltyAnchorBlockEntity> implements SimpleTickableVisual {

    private final TransformedInstance overlay;
    private BlockPos oldTargetPos;

    public PenaltyAnchorVisual(VisualizationContext ctx, PenaltyAnchorBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        overlay = ctx.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, Models.partial(ModPartials.TRACK_PENALTY_ANCHOR_OVERLAY))
            .createInstance();

        setupVisual();
    }

    @Override
    public void tick(Context context) {
        setupVisual();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(overlay);
    }

    @Override
    protected void _delete() {
        overlay.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(overlay);
    }

    private void setupVisual() {
        TrackTargetingBehaviour<PenaltyAnchor> target = blockEntity.edgePoint;
        BlockPos targetPosition = target.getGlobalPosition();
        Level level = blockEntity.getLevel();
        BlockState trackState = level.getBlockState(targetPosition);

        if (!(trackState.getBlock() instanceof ITrackBlock trackBlock)) {
            overlay.setZeroTransform().setChanged();
            oldTargetPos = null;
            return;
        }

        if (targetPosition.equals(oldTargetPos))
            return;

        oldTargetPos = targetPosition;
        overlay.setIdentityTransform().translate(targetPosition.subtract(renderOrigin()));
        trackBlock.prepareTrackOverlay(overlay, level, targetPosition, trackState, target.getTargetBezier(), AxisDirection.POSITIVE, RenderedTrackOverlayType.OBSERVER);
        overlay.setChanged();
    }
}
