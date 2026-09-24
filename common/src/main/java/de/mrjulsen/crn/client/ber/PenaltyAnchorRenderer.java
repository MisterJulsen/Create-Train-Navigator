package de.mrjulsen.crn.client.ber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import de.mrjulsen.crn.block.penalty.PenaltyAnchor;
import de.mrjulsen.crn.block.penalty.PenaltyAnchorBlockEntity;
import de.mrjulsen.crn.client.ModPartials;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class PenaltyAnchorRenderer extends SmartBlockEntityRenderer<PenaltyAnchorBlockEntity> {

    public PenaltyAnchorRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PenaltyAnchorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        Level level = be.getLevel();
        if (VisualizationManager.supportsVisualization(level))
            return;

        TrackTargetingBehaviour<PenaltyAnchor> target = be.edgePoint;
        BlockPos targetPosition = target.getGlobalPosition();

        ms.pushPose();
        TransformStack.of(ms).translate(targetPosition.subtract(be.getBlockPos()));
        renderMarker(level, targetPosition, target.getTargetBezier(), ms, buffer, 1);
        ms.popPose();
    }

    public static void renderMarker(LevelAccessor level, BlockPos pos, BezierTrackPointLocation bezier, PoseStack ms, MultiBufferSource buffer, float scale) {
        if (level instanceof SchematicLevel && !(level instanceof PonderLevel))
            return;

        BlockState trackState = level.getBlockState(pos);
        if (!(trackState.getBlock() instanceof ITrackBlock track))
            return;

        ms.pushPose();
        PoseTransformStack msr = TransformStack.of(ms);

        if (track.prepareTrackOverlay(msr, level, pos, trackState, bezier, AxisDirection.POSITIVE, RenderedTrackOverlayType.OBSERVER) != null)
            CachedBuffers.partial(ModPartials.TRACK_PENALTY_ANCHOR_OVERLAY, trackState)
                .translate(.5, 0, .5)
                .scale(scale)
                .translate(-.5, 0, -.5)
                .light(LevelRenderer.getLightColor(level, pos))
                .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

        ms.popPose();
    }
}
