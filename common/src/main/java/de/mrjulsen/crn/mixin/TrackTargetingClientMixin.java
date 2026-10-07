package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem.OverlapResult;
import com.simibubi.create.content.trains.track.TrackTargetingClient;

import de.mrjulsen.crn.client.ber.PenaltyAnchorRenderer;
import de.mrjulsen.crn.registry.ModExtras;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@Mixin(TrackTargetingClient.class)
public class TrackTargetingClientMixin {

    @Shadow(remap = false)
    static BlockPos lastHovered;

    @Shadow(remap = false)
    static EdgePointType<?> lastType;

    @Shadow(remap = false)
    static BezierTrackPointLocation lastHoveredBezierSegment;

    @Shadow(remap = false)
    static OverlapResult lastResult;

    @Shadow(remap = false)
    static TrackGraphLocation lastLocation;

    @Inject(remap = false, method = "render", at = @At("HEAD"), cancellable = true)
    private static void crn$renderPenaltyAnchorMarker(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, CallbackInfo ci) {
        if (lastType != ModExtras.PENALTY_ANCHOR)
            return;

        ci.cancel();

        if (lastLocation == null || lastResult.feedback != null)
            return;

        ms.pushPose();
        TransformStack.of(ms).translate(Vec3.atLowerCornerOf(lastHovered).subtract(camera));
        PenaltyAnchorRenderer.renderMarker(Minecraft.getInstance().level, lastHovered, lastHoveredBezierSegment, ms, buffer, 1 + 1 / 16f);
        ms.popPose();
    }
}
