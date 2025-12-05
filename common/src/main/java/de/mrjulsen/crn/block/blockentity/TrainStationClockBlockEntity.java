package de.mrjulsen.crn.block.blockentity;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import de.mrjulsen.crn.client.ber.TrainStationClockRenderer;
import de.mrjulsen.mcdragonlib.block.IBERInstance;
import de.mrjulsen.mcdragonlib.client.ber.IBlockEntityRendererInstance;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TrainStationClockBlockEntity extends SmartBlockEntity implements IBERInstance<TrainStationClockBlockEntity> {

    private static final String NBT_COLOR = "Color";
    private static final String NBT_GLOWING = "IsGlowing";

    private final Cache<IBlockEntityRendererInstance<TrainStationClockBlockEntity>> renderer = new Cache<>(() -> new TrainStationClockRenderer(this), ECachingPriority.ALWAYS);

    private DLColor color = DLColor.WHITE;
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

    public DLColor getColor() {
        return color;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setColor(DLColor color) {
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
        tag.putInt(NBT_COLOR, getColor().withAlpha(255).getAsARGB());
        tag.putBoolean(NBT_GLOWING, isGlowing());
    }
    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains(NBT_COLOR)) {
            color = DLColor.fromInt(tag.getInt(NBT_COLOR));
        }
        if (tag.contains(NBT_GLOWING)) {
            glowing = tag.getBoolean(NBT_GLOWING);
        }
    }

}