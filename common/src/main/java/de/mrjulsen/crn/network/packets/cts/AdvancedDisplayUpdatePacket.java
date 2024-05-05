package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.data.EDisplayInfo;
import de.mrjulsen.crn.data.EDisplayType;
import de.mrjulsen.crn.data.ESide;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplayUpdatePacket implements IPacketBase<AdvancedDisplayUpdatePacket> {
    private BlockPos pos;
    private EDisplayType type;
    private EDisplayInfo info;
    private boolean doubleSided;

    public AdvancedDisplayUpdatePacket() {}

    public AdvancedDisplayUpdatePacket(Level level, BlockPos pos, EDisplayType type, EDisplayInfo info, boolean doubleSided) {
        this.pos = pos;
        this.info = info;
        this.type = type;
        this.doubleSided = doubleSided;
        apply(level, this);
    }

    protected AdvancedDisplayUpdatePacket(BlockPos pos, EDisplayType type, EDisplayInfo info, boolean doubleSided) {
        this.pos = pos;
        this.info = info;
        this.type = type;
        this.doubleSided = doubleSided;
    }

    @Override
    public void encode(AdvancedDisplayUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.info.getId());
        buffer.writeInt(packet.type.getId());
        buffer.writeBoolean(packet.doubleSided);
    }

    @Override
    public AdvancedDisplayUpdatePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        EDisplayInfo info = EDisplayInfo.getTypeById(buffer.readInt());
        EDisplayType type = EDisplayType.getTypeById(buffer.readInt());
        boolean doubleSided = buffer.readBoolean();

        return new AdvancedDisplayUpdatePacket(pos, type, info, doubleSided);
    }

    private void apply(Level level, AdvancedDisplayUpdatePacket packet) {
        if (level.isLoaded(packet.pos)) {
            if (level.getBlockEntity(packet.pos) instanceof AdvancedDisplayBlockEntity blockEntity) {
                blockEntity.applyToAll(be -> {
                    be.setDisplayType(packet.type);
                    be.setInfoType(packet.info);
                    if (level.getBlockState(be.getBlockPos()).getBlock() instanceof AbstractAdvancedSidedDisplayBlock) {
                        BlockState state = level.getBlockState(be.getBlockPos());
                        state = state.setValue(AbstractAdvancedSidedDisplayBlock.SIDE, packet.doubleSided ? ESide.BOTH : ESide.FRONT);
                        level.setBlockAndUpdate(be.getBlockPos(), state);
                    }
                    be.notifyUpdate();                    
                });
            }
        }
    }
    
    @Override
    public void handle(AdvancedDisplayUpdatePacket packet, Supplier<PacketContext> contextSupplier) {        
        contextSupplier.get().queue(() -> {
            Player player = contextSupplier.get().getPlayer();
            if (player != null) {
                Level level = player.level();
                apply(level, packet);
            }
        });
    }
}
