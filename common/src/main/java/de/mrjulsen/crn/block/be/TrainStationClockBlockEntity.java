package de.mrjulsen.crn.block.be;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import de.mrjulsen.crn.client.ber.TrainStationClockRenderer;
import de.mrjulsen.crn.client.ber.base.IBERInstance;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance;
import de.mrjulsen.mcdragonlib.data.Cache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TrainStationClockBlockEntity extends SmartBlockEntity implements IBERInstance<TrainStationClockBlockEntity> {

    private final Cache<IBlockEntityRendererInstance<TrainStationClockBlockEntity>> renderer = new Cache<>(() -> new TrainStationClockRenderer(this));

    public TrainStationClockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public IBlockEntityRendererInstance<TrainStationClockBlockEntity> getRenderer() {
        return renderer.get();
    }

}