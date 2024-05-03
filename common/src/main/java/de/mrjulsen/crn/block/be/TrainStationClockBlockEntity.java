package de.mrjulsen.crn.block.be;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import de.mrjulsen.crn.client.ber.TrainStationClockRenderer;
import de.mrjulsen.mcdragonlib.block.IBERInstance;
import de.mrjulsen.mcdragonlib.client.ber.IBlockEntityRendererInstance;
import de.mrjulsen.mcdragonlib.data.Cache;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TrainStationClockBlockEntity extends SmartBlockEntity implements IBERInstance<TrainStationClockBlockEntity>, IColorableBlockEntity {

    private static final String NBT_COLOR = "Color";
    private static final String NBT_GLOWING = "IsGlowing";

    private final Cache<IBlockEntityRendererInstance<TrainStationClockBlockEntity>> renderer = new Cache<>(() -> new TrainStationClockRenderer(this));

    private int color = 0xFFFFFFFF;
    private boolean glowing;

    public TrainStationClockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public IBlockEntityRendererInstance<TrainStationClockBlockEntity> getRenderer() {
        return renderer.get();
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public boolean isGlowing() {
        return glowing;
    }

    public void setColor(int color) {
        this.color = color;
        notifyUpdate();
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        notifyUpdate();
    }
    
    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt(NBT_COLOR, getColor());
        tag.putBoolean(NBT_GLOWING, isGlowing());
    }
    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains(NBT_COLOR)) {
            color = tag.getInt(NBT_COLOR);
        }
        if (tag.contains(NBT_GLOWING)) {
            glowing = tag.getBoolean(NBT_GLOWING);
        }
    }

}