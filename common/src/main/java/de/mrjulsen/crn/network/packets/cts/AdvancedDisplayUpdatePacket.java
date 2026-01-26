package de.mrjulsen.crn.network.packets.cts;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.IBlockGetter;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.block.properties.ESide;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeResourceKey;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class AdvancedDisplayUpdatePacket implements IPacketBase<AdvancedDisplayUpdatePacket> {
    private BlockPos pos;
    private int entityId;
    private boolean isOnContraption;

    private DisplayTypeResourceKey key;
    private boolean doubleSided;
    private IDisplaySettings settings;

    public AdvancedDisplayUpdatePacket() {}

    public AdvancedDisplayUpdatePacket(Level level, BlockPos pos, AbstractContraptionEntity contraption, DisplayTypeResourceKey key, boolean doubleSided, IDisplaySettings settings) {
        this.pos = pos;
        this.isOnContraption = contraption != null;
        this.entityId = isOnContraption ? contraption.getId() : 0;

        this.key = key;
        this.doubleSided = doubleSided;
        this.settings = settings;
        if (isOnContraption) {
            applyContraption(contraption, this);
        } else {
            apply(level, this);
        }
    }

    protected AdvancedDisplayUpdatePacket(BlockPos pos, int entityId, boolean isOnContraption, DisplayTypeResourceKey key, boolean doubleSided, IDisplaySettings settings) {
        this.pos = pos;
        this.entityId = entityId;
        this.isOnContraption = isOnContraption;

        this.key = key;
        this.doubleSided = doubleSided;
        this.settings = settings;
    }

    @Override
    public void encode(AdvancedDisplayUpdatePacket packet, FriendlyByteBuf buffer) {
        CompoundTag k = new CompoundTag();
        packet.key.toNbt(k);
        
        buffer.writeBlockPos(packet.pos);
        buffer.writeNbt(k);
        buffer.writeBoolean(packet.doubleSided);
        buffer.writeBoolean(packet.isOnContraption);
        buffer.writeInt(packet.entityId);
        buffer.writeNbt(packet.settings.serializeNbt());
    }

    @Override
    public AdvancedDisplayUpdatePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        DisplayTypeResourceKey key = DisplayTypeResourceKey.fromNbt(buffer.readNbt());
        boolean doubleSided = buffer.readBoolean();
        boolean isOnContraption = buffer.readBoolean();
        int entityId = buffer.readInt();
        IDisplaySettings settings = AdvancedDisplaysRegistry.createSettings(key);
        settings.deserializeNbt(buffer.readNbt());

        return new AdvancedDisplayUpdatePacket(pos, entityId, isOnContraption, key, doubleSided, settings);
    }

    private void apply(Level level, AdvancedDisplayUpdatePacket packet) {
        if (level.isLoaded(packet.pos)) {
            if (level.getBlockEntity(packet.pos) instanceof AdvancedDisplayBlockEntity blockEntity) {
                blockEntity.applyToAll(be -> {
                    be.setDisplayType(level, packet.key, packet.settings);
                    if (level.getBlockState(be.getBlockPos()).getBlock() instanceof AbstractAdvancedSidedDisplayBlock) {
                        BlockState state = level.getBlockState(be.getBlockPos());
                        state = state.setValue(AbstractAdvancedSidedDisplayBlock.SIDE, packet.doubleSided ? ESide.BOTH : ESide.FRONT);
                        level.setBlockAndUpdate(be.getBlockPos(), state);
                    }
                    be.notifyUpdate();                    
                }, new IBlockGetter.WorldBlockGetter(level));
            }
        }
    }

    private void applyContraption(AbstractContraptionEntity contraptionEntity, AdvancedDisplayUpdatePacket packet) {
        Contraption contraption = contraptionEntity.getContraption();
        Level level = contraption.getContraptionWorld();
        Set<BlockPos> blockEntityPositions = new HashSet<>();

        MutableBlockPos pos = packet.pos.mutable();
        MutablePair<StructureBlockInfo, MovementContext> rootActor = contraption.getActorAt(pos);
        if (rootActor == null || rootActor.right == null)
            return;

        MovementContext rootCtx = rootActor.getRight();            
		Direction side = rootCtx.state.getValue(HorizontalDirectionalBlock.FACING).getCounterClockWise();
        byte width = rootCtx.blockEntityData.getByte(AdvancedDisplayBlockEntity.NBT_XSIZE);
        byte height = rootCtx.blockEntityData.getByte(AdvancedDisplayBlockEntity.NBT_YSIZE);
            
        packet.key.toNbt(rootCtx.blockEntityData);
        packet.key.toNbt(rootCtx.data);
        rootCtx.blockEntityData.put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());
        rootCtx.data.put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());
        blockEntityPositions.add(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));

        StructureBlockInfo rootInfo = contraption.getBlocks().get(pos);
        if (rootInfo.state().getBlock() instanceof AbstractAdvancedSidedDisplayBlock) {
            BlockState newState = rootInfo.state().setValue(AbstractAdvancedSidedDisplayBlock.SIDE, packet.doubleSided ? ESide.BOTH : ESide.FRONT);
            contraption.getBlocks().put(pos, new StructureBlockInfo(rootInfo.pos(), newState, rootInfo.nbt()));
            contraption.deferInvalidate = true;
        }

        if (contraption.presentBlockEntities.containsKey(pos) && contraption.presentBlockEntities.get(pos) instanceof AdvancedDisplayBlockEntity be) {
            be.setDisplayType(level, packet.key, packet.settings);
            be.setBlockState(contraption.getBlocks().get(pos).state());
            if (level.isClientSide()) {                
                be.getRenderer().update(level, pos, be.getBlockState(), be, EUpdateReason.LAYOUT_CHANGED);
            }
        }

		for (int i = 0; i < width && i < AdvancedDisplayBlockEntity.MAX_XSIZE; i++) {
            BlockPos newPos = pos.relative(side, i);
            for (int j = 0; j < height && j < AdvancedDisplayBlockEntity.MAX_YSIZE; j++) {
                BlockPos newPos2 = newPos.relative(Direction.DOWN, j);

                MutablePair<StructureBlockInfo, MovementContext> actor = contraption.getActorAt(newPos2);
                if (actor == null || actor.right == null)
                    continue;

                blockEntityPositions.add(new BlockPos(newPos2.getX(), newPos2.getY(), newPos2.getZ()));

                MovementContext ctx = actor.getRight();  
                packet.key.toNbt(ctx.blockEntityData);
                packet.key.toNbt(ctx.data);
                ctx.blockEntityData.put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());
                ctx.data.put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());
                
                if (contraption.presentBlockEntities.containsKey(newPos2) && contraption.presentBlockEntities.get(newPos2) instanceof AdvancedDisplayBlockEntity be) {
                    be.setDisplayType(level, packet.key, packet.settings);
                }

                StructureBlockInfo info = contraption.getBlocks().get(newPos2);
                if (info.state().getBlock() instanceof AbstractAdvancedSidedDisplayBlock) {
                    BlockState newState = info.state().setValue(AbstractAdvancedSidedDisplayBlock.SIDE, packet.doubleSided ? ESide.BOTH : ESide.FRONT);
                    contraption.getBlocks().put(newPos2, new StructureBlockInfo(newPos2, newState, info.nbt()));
                }
            }
		}

        IBlockGetter getter = new IBlockGetter.ContraptionBlockGetter(contraptionEntity);
        for (MutablePair<StructureBlockInfo, MovementContext> a : contraption.getActors()) {
            BlockEntity blockEntity = getter.getBlockEntity(a.getLeft().pos());
            if (blockEntity instanceof AdvancedDisplayBlockEntity be) {
                be.updateControllerStatus2(getter);
            }
        }
    }
    
    @Override
    public void handle(AdvancedDisplayUpdatePacket packet, Supplier<PacketContext> contextSupplier) {        
        contextSupplier.get().queue(() -> {
            Player player = contextSupplier.get().getPlayer();
            if (player != null) {
                Level level = player.level();

                if (packet.isOnContraption) {
                    if (level.getEntity(packet.entityId) instanceof AbstractContraptionEntity ce) {
                        applyContraption(ce, packet);
                    }
                } else {
                    apply(level, packet);
                }
            }
        });
    }
}
