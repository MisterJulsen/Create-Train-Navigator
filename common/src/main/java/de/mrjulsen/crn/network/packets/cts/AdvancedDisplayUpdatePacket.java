package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
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

public class AdvancedDisplayUpdatePacket implements IPacketBase<AdvancedDisplayUpdatePacket> {
    private BlockPos pos;
    private EDisplayType type;
    private EDisplayInfo info;
    private ESide side;

    public AdvancedDisplayUpdatePacket() {}

    public AdvancedDisplayUpdatePacket(Level level, BlockPos pos, EDisplayType type, EDisplayInfo info, ESide side) {
        this.pos = pos;
        this.info = info;
        this.type = type;
        this.side = side;
        apply(level, this);
    }

    protected AdvancedDisplayUpdatePacket(BlockPos pos, EDisplayType type, EDisplayInfo info, ESide side) {
        this.pos = pos;
        this.info = info;
        this.type = type;
        this.side = side;
    }

    @Override
    public void encode(AdvancedDisplayUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.info.getId());
        buffer.writeInt(packet.type.getId());
        buffer.writeEnum(packet.side);
    }

    @Override
    public AdvancedDisplayUpdatePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        EDisplayInfo info = EDisplayInfo.getTypeById(buffer.readInt());
        EDisplayType type = EDisplayType.getTypeById(buffer.readInt());
        ESide side = buffer.readEnum(ESide.class);

        return new AdvancedDisplayUpdatePacket(pos, type, info, side);
    }

    private void apply(Level level, AdvancedDisplayUpdatePacket packet) {
        if (level.isLoaded(packet.pos)) {
            if (level.getBlockEntity(packet.pos) instanceof AdvancedDisplayBlockEntity blockEntity) {
                blockEntity.applyToAll(be -> {
                    be.setDisplayType(packet.type);
                    be.setInfoType(packet.info);
                    if (level.getBlockState(be.getBlockPos()).getBlock() instanceof AbstractAdvancedDisplayBlock) {
                        level.setBlockAndUpdate(be.getBlockPos(), level.getBlockState(be.getBlockPos()).setValue(AbstractAdvancedDisplayBlock.SIDE, packet.side));
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
                Level level = player.getLevel();
                apply(level, packet);
            }
        });
    }
}
