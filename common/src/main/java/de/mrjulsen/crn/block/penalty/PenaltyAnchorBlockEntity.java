package de.mrjulsen.crn.block.penalty;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import de.mrjulsen.crn.block.behaviour.SteppedScrollValueBehaviour;
import de.mrjulsen.crn.registry.ModExtras;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.util.TextUtils;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PenaltyAnchorBlockEntity extends SmartBlockEntity implements TransformableBlockEntity {

    public static final int MAX_REDSTONE_INPUT = 15;
    private static final int NOTIFY_COOLDOWN = 20;

    public TrackTargetingBehaviour<PenaltyAnchor> edgePoint;

    private FilteringBehaviour filtering;
    private SteppedScrollValueBehaviour penalty;

    public UUID passingTrainUUID;

    private int redstoneInput;
    private int appliedPenalty = -1;
    private long lastNotifyTick;
    private boolean penaltyDirty = true;

    public PenaltyAnchorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(edgePoint = new TrackTargetingBehaviour<>(this, ModExtras.PENALTY_ANCHOR));
        behaviours.add(filtering = createFilter().withCallback(this::onFilterChanged));
        filtering.setLabel(TextUtils.translate("block." + CreateRailwaysNavigator.MOD_ID + ".penalty_anchor.filter"));

        penalty = new SteppedScrollValueBehaviour(TextUtils.translate("block." + CreateRailwaysNavigator.MOD_ID + ".penalty_anchor.penalty"), this, new CenteredSideValueBoxTransform((state, direction) -> direction.getAxis().isHorizontal()), PenaltyAnchor.PENALTY_STEP);
        penalty.requiresWrench();
        penalty.betweenStepped(PenaltyAnchor.MIN_PENALTY, PenaltyAnchor.MAX_PENALTY);
        penalty.setStepped(PenaltyAnchor.DEFAULT_PENALTY);
        penalty.withCallback($ -> penaltyDirty = true);
        behaviours.add(penalty);
    }

    private void onFilterChanged(ItemStack newFilter) {
        if (level.isClientSide())
            return;
        PenaltyAnchor anchor = getObserver();
        if (anchor != null)
            anchor.setFilterAndNotify(level, newFilter);
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide())
            return;

        updatePenalty();
        updatePoweredState();
    }

    private void updatePenalty() {
        redstoneInput = Math.max(level.getSignal(worldPosition.above(), Direction.UP), level.getSignal(worldPosition.below(), Direction.DOWN));

        int effective = getEffectivePenalty();
        if (effective == appliedPenalty) {
            penaltyDirty = false;
            return;
        }
        if (!penaltyDirty && level.getGameTime() - lastNotifyTick < NOTIFY_COOLDOWN)
            return;

        PenaltyAnchor anchor = getObserver();
        if (anchor == null)
            return;

        appliedPenalty = effective;
        lastNotifyTick = level.getGameTime();
        penaltyDirty = false;
        anchor.setPenaltyAndNotify(level, effective);
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private void updatePoweredState() {
        PenaltyAnchor anchor = getObserver();
        BlockState blockState = getBlockState();
        boolean shouldBePowered = anchor != null && anchor.isActivated();

        if (isBlockPowered() != shouldBePowered) {
            passingTrainUUID = shouldBePowered ? anchor.getCurrentTrain() : null;

            if (blockState.hasProperty(PenaltyAnchorBlock.POWERED)) {
                blockState = blockState.setValue(PenaltyAnchorBlock.POWERED, shouldBePowered);
            }
        }

        if (blockState.hasProperty(PenaltyAnchorBlock.ENABLED)) {
            blockState = blockState.setValue(PenaltyAnchorBlock.ENABLED, getEffectivePenalty() > 0);
        }

        if (blockState != getBlockState()) {
            level.setBlock(worldPosition, blockState, 3);
        }

        DisplayLinkBlock.notifyGatherers(level, worldPosition);
    }

    @Nullable
    public PenaltyAnchor getObserver() {
        return edgePoint.getEdgePoint();
    }

    public ItemStack getFilter() {
        return filtering.getFilter();
    }

    public int getPenalty() {
        return penalty.getStepped();
    }

    public int getRedstoneInput() {
        return redstoneInput;
    }

    public int getEffectivePenalty() {
        return getPenalty() * (MAX_REDSTONE_INPUT - Math.min(redstoneInput, MAX_REDSTONE_INPUT)) / MAX_REDSTONE_INPUT;
    }

    public int getComparatorOutput() {
        int effective = getEffectivePenalty();
        return effective <= PenaltyAnchor.MIN_PENALTY ? 0 : 1 + effective * 14 / PenaltyAnchor.MAX_PENALTY;
    }

    public boolean isBlockPowered() {
        return getBlockState().getOptionalValue(PenaltyAnchorBlock.POWERED).orElse(false);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition, edgePoint.getGlobalPosition()).inflate(2);
    }

    @Override
    public void transform(BlockEntity be, StructureTransform transform) {
        edgePoint.transform(be, transform);
    }

    public FilteringBehaviour createFilter() {
        return new FilteringBehaviour(this, new ValueBoxTransform() {

            @Override
            public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
                TransformStack.of(ms).rotateXDegrees(90);
            }

            @Override
            public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
                return new Vec3(0.5, 15.5 / 16d, 0.5);
            }

        });
    }
}
