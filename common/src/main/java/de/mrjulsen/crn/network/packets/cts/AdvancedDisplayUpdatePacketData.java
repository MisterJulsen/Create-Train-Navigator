package de.mrjulsen.crn.network.packets.cts;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.IBlockGetter;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.block.properties.ESide;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeResourceKey;
import de.mrjulsen.crn.mixin.ContraptionAccessor;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.mcdragonlib.util.NbtUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class AdvancedDisplayUpdatePacketData extends NetworkPacketData {

    private static final String NBT_POS = "Pos";
    private static final String NBT_ENTITY_ID = "EntityId";
    private static final String NBT_IS_ON_CONTRAPTION = "IsOnContraption";
    private static final String NBT_KEY = "Key";
    private static final String NBT_DOUBLE_SIDED = "DoubleSided";
    private static final String NBT_SETTINGS = "Settings";

    private BlockPos pos;
    private int entityId;
    private boolean isOnContraption;

    private DisplayTypeResourceKey key;
    private boolean doubleSided;
    private IDisplaySettings settings;

    public AdvancedDisplayUpdatePacketData(DLStatus status) {
        super(status);
    }

    public AdvancedDisplayUpdatePacketData(Level level, BlockPos pos, AbstractContraptionEntity contraption, DisplayTypeResourceKey key, boolean doubleSided, IDisplaySettings settings) {
        super(DLStatus.OK);
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

    protected AdvancedDisplayUpdatePacketData(BlockPos pos, int entityId, boolean isOnContraption, DisplayTypeResourceKey key, boolean doubleSided, IDisplaySettings settings) {
        super(DLStatus.OK);
        this.pos = pos;
        this.entityId = entityId;
        this.isOnContraption = isOnContraption;

        this.key = key;
        this.doubleSided = doubleSided;
        this.settings = settings;
    }

    @Override
    protected void write(CompoundTag nbt) {
        CompoundTag tag = new CompoundTag();
        key.toNbt(tag);
        nbt.put(NBT_KEY, tag);
        
        NbtUtils.putNbtPos(nbt, NBT_POS, pos);
        nbt.putBoolean(NBT_DOUBLE_SIDED, doubleSided);
        nbt.putBoolean(NBT_IS_ON_CONTRAPTION, isOnContraption);
        nbt.putInt(NBT_ENTITY_ID, entityId);
        nbt.put(NBT_SETTINGS, settings.serializeNbt());
        
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.key = DisplayTypeResourceKey.fromNbt(nbt.getCompound(NBT_KEY));
        this.pos = NbtUtils.getNbtBlockPos(nbt, NBT_POS);
        this.doubleSided = nbt.getBoolean(NBT_DOUBLE_SIDED);
        this.isOnContraption = nbt.getBoolean(NBT_IS_ON_CONTRAPTION);
        this.entityId = nbt.getInt(NBT_ENTITY_ID);
        this.settings = AdvancedDisplaysRegistry.createSettings(key);
        this.settings.deserializeNbt(nbt.getCompound(NBT_SETTINGS));
    }
    
    

    public static void handle(AdvancedDisplayUpdatePacketData packet, NetworkPacketContext context) {        
        context.queue(() -> {
            Player player = context.getPlayer();
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

    

    private static void apply(Level level, AdvancedDisplayUpdatePacketData packet) {
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

    private static void applyContraption(AbstractContraptionEntity contraptionEntity, AdvancedDisplayUpdatePacketData packet) {
        Contraption contraption = contraptionEntity.getContraption();
        Level level = contraption.getContraptionWorld();
        Set<BlockPos> blockEntityPositions = new HashSet<>();

        MutableBlockPos pos = packet.pos.mutable();
        MutablePair<StructureBlockInfo, MovementContext> rootActor = contraption.getActorAt(pos);
        if (rootActor == null || rootActor.right == null)
            return;

        MovementContext rootCtx = rootActor.getRight();
        StructureBlockInfo rootInfo = contraption.getBlocks().get(pos);
        Direction side = rootCtx.state.getValue(HorizontalDirectionalBlock.FACING).getCounterClockWise();
        byte width = rootCtx.blockEntityData.getByte(AdvancedDisplayBlockEntity.NBT_XSIZE);
        byte height = rootCtx.blockEntityData.getByte(AdvancedDisplayBlockEntity.NBT_YSIZE);
        Map<BlockPos, CompoundTag> updateTags = ((ContraptionAccessor) contraption).crn$updateTags();

        // update root contexts / nbt
        packet.key.toNbt(rootCtx.blockEntityData);
        packet.key.toNbt(rootCtx.data);
        packet.key.toNbt(rootInfo.nbt());
        rootCtx.blockEntityData.put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());
        rootCtx.data.put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());
        rootInfo.nbt().put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());

        // persist root updateTag (use an immutable BlockPos and a copy of the tag)
        BlockPos immutableRootPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
        updateTags.put(immutableRootPos, rootInfo.nbt().copy());
        blockEntityPositions.add(immutableRootPos);

        if (rootInfo.state().getBlock() instanceof AbstractAdvancedSidedDisplayBlock) {
            BlockState newState = rootInfo.state().setValue(AbstractAdvancedSidedDisplayBlock.SIDE, packet.doubleSided ? ESide.BOTH : ESide.FRONT);
            contraption.getBlocks().put(pos, new StructureBlockInfo(rootInfo.pos(), newState, rootInfo.nbt()));
            contraption.resetClientContraption();
        }

        if (CRNPlatformSpecific.getClientContraptionBlockEntity(contraption, pos) instanceof AdvancedDisplayBlockEntity be) {
            be.setDisplayType(level, packet.key, packet.settings);
            be.setBlockState(contraption.getBlocks().get(pos).state());
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
                StructureBlockInfo info = contraption.getBlocks().get(newPos2);
                packet.key.toNbt(ctx.blockEntityData);
                packet.key.toNbt(ctx.data);
                packet.key.toNbt(info.nbt());
                ctx.blockEntityData.put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());
                ctx.data.put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());
                info.nbt().put(AdvancedDisplayBlockEntity.NBT_DISPLAY_TYPE_SETTINGS, packet.settings.serializeNbt());

                // persist child updateTag (immutable key + copy)
                BlockPos immutablePos = new BlockPos(newPos2.getX(), newPos2.getY(), newPos2.getZ());
                updateTags.put(immutablePos, info.nbt().copy());

                if (CRNPlatformSpecific.getClientContraptionBlockEntity(contraption, newPos2) instanceof AdvancedDisplayBlockEntity be) {
                    be.setDisplayType(level, packet.key, packet.settings);
                    be.setBlockState(contraption.getBlocks().get(newPos2).state());
                }

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
                if (level.isClientSide()) {
                    be.getRenderer().update(level, a.getLeft().pos(), be.getBlockState(), be, EUpdateReason.LAYOUT_CHANGED);
                }
                if (updateTags.containsKey(a.getLeft().pos()))
                    be.writeClient(updateTags.get(a.getLeft().pos()));
            }
        }
    }
}
