package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.NbtUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class TeleportPlayerPacket extends NetworkPacketData {

    private static final String NBT_POS = "Pos";
    private static final String NBT_DIMENSION = "Dimension";

    private BlockPos pos;
    private ResourceLocation dimension;

    public TeleportPlayerPacket(DLStatus status) {
        super(status);
    }

    public TeleportPlayerPacket(BlockPos pos, ResourceLocation dimension) {
        super(DLStatus.OK);
        this.pos = pos;
        this.dimension = dimension;
    }

    @Override
    protected void write(CompoundTag nbt) {
        NbtUtils.putNbtPos(nbt, NBT_POS, pos);
        nbt.putString(NBT_DIMENSION, dimension.toString());
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.pos = NbtUtils.getNbtBlockPos(nbt, NBT_POS);
        this.dimension = DLUtils.resourceLocation(nbt.getString(NBT_DIMENSION));
    }

    public static void handle(TeleportPlayerPacket packet, NetworkPacketContext context) {
        context.queue(() -> {
            context.getPlayer().teleportTo(packet.pos.getX(), packet.pos.getY(), packet.pos.getZ());
        });
    }
}
