package de.mrjulsen.crn.block;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import de.mrjulsen.crn.CRNPlatformSpecific;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public interface IBlockGetter {
    BlockState getBlockState(BlockPos pos);
    BlockEntity getBlockEntity(BlockPos pos);
    boolean isClientSide();
    void updateBlockEntity(SmartBlockEntity be, BlockPos pos);
    void updateBlockState(BlockState state, BlockPos pos);

    public static class ContraptionBlockGetter implements IBlockGetter {
        private final AbstractContraptionEntity contraptionEntity;

        public ContraptionBlockGetter(AbstractContraptionEntity contraptionEntity) {
            this.contraptionEntity = contraptionEntity;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            Contraption contraption = contraptionEntity.getContraption();
            return contraption.getBlocks().containsKey(pos) ? contraption.getBlocks().get(pos).state() : null;
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            Contraption contraption = contraptionEntity.getContraption();
            return CRNPlatformSpecific.getClientContraptionBlockEntity(contraption, pos);
        }

        @Override
        public boolean isClientSide() {
            Contraption contraption = contraptionEntity.getContraption();
            return contraption.getContraptionWorld().isClientSide();
        }

        @Override
        public void updateBlockEntity(SmartBlockEntity be, BlockPos pos) {
            Contraption contraption = contraptionEntity.getContraption();
            MutablePair<StructureBlockInfo, MovementContext> actor = contraption.getActorAt(pos);
            if (actor == null || actor.getRight() == null)
                return;

            MovementContext ctx = actor.getRight();
            be.saveAdditional(ctx.blockEntityData);
        }

        @Override
        public void updateBlockState(BlockState state, BlockPos pos) {
            Contraption contraption = contraptionEntity.getContraption();
            StructureBlockInfo info = contraption.getBlocks().get(pos);
            contraption.getBlocks().put(pos, new StructureBlockInfo(pos, state, info.nbt()));
        }
    }

    public static class WorldBlockGetter implements IBlockGetter {
        private final BlockGetter level;
        private final boolean isClientSide;

        public WorldBlockGetter(BlockAndTintGetter level) {
            this.level = level;
            this.isClientSide = true;
        }

        public WorldBlockGetter(Level level) {
            this.level = level;
            this.isClientSide = level.isClientSide;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return level.getBlockState(pos);
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return level.getBlockEntity(pos);
        }

        @Override
        public void updateBlockEntity(SmartBlockEntity be, BlockPos pos) {            
        }

        @Override
        public void updateBlockState(BlockState state, BlockPos pos) {            
        }

        @Override
        public boolean isClientSide() {
            return isClientSide;
        }
    }
}

    