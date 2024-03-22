package de.mrjulsen.crn.block.be;

import java.util.List;
import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.display.FlapDisplayBlock;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.base.IBERInstance;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance;
import de.mrjulsen.crn.data.CarriageData;
import de.mrjulsen.crn.data.EDisplayInfo;
import de.mrjulsen.crn.data.EDisplayType;
import de.mrjulsen.crn.data.ESide;
import de.mrjulsen.crn.data.IBlockEntitySerializable;
import de.mrjulsen.crn.data.DeparturePrediction.TrainExitSide;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket.TrainData;
import de.mrjulsen.crn.util.Cache;
import de.mrjulsen.crn.util.Pair;
import de.mrjulsen.crn.util.Tripple;
import de.mrjulsen.mcdragonlib.common.BlockEntityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class AdvancedDisplayBlockEntity extends SmartBlockEntity implements
    IMultiblockBlockEntity<AdvancedDisplayBlockEntity, AbstractAdvancedDisplayBlock>,
    IContraptionBlockEntity<AdvancedDisplayBlockEntity>,
    IBERInstance<AdvancedDisplayBlockEntity>,
    IBlockEntitySerializable,
    IColorableBlockEntity
{

    private static final String NBT_XSIZE = "XSize";
    private static final String NBT_YSIZE = "YSize";
    private static final String NBT_CONTROLLER = "IsController";
    private static final String NBT_COLOR = "Color";
    private static final String NBT_INFO_TYPE = "InfoType";
    private static final String NBT_DISPLAY_TYPE = "DisplayType";

    public static final byte MAX_XSIZE = 8;
    public static final byte MAX_YSIZE = 8;

    // DATA
    /** Applies only for the controller block. All other blocks will have a size of 1. */
	private byte xSize = 1;
	private byte ySize = 1;
    private boolean isController;

    // USER SETTINGS
    private int color = DyeColor.WHITE.getTextColor();
    private boolean glowing = true;
    private EDisplayInfo infoType = EDisplayInfo.SIMPLE;
    private EDisplayType displayType = EDisplayType.TRAIN_DESTINATION;

    // CLIENT DISPLAY ONLY - this data is not being saved!
    private long lastRefreshedTime;
    private TrainData trainData = TrainData.empty();
    private CarriageData carriageData = new CarriageData(0, Direction.NORTH, false);
    
    // OTHER
    private int syncTicks = 0;
    private final Cache<IBlockEntityRendererInstance<AdvancedDisplayBlockEntity>> renderer = new Cache<>(() -> new AdvancedDisplayRenderInstance(this));

    public final Cache<TrainExitSide> relativeExitDirection = new Cache<>(() -> {
        
        if (getCarriageData() == null || !getTrainData().getNextStop().isPresent() || !(getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock)) {
            return TrainExitSide.UNKNOWN;
        }
        TrainExitSide side = getTrainData().getNextStop().get().exitSide();
        Direction blockFacing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        if (!carriageData.isOppositeDirection()) {
            blockFacing = blockFacing.getOpposite();
        }

        TrainExitSide result = side;
        if (getCarriageData().assemblyDirection() == blockFacing) {
            result = result.getOpposite();
        } else if (getCarriageData().assemblyDirection().getOpposite() != blockFacing) {
            result = TrainExitSide.UNKNOWN;
        }
        return result;
    });

    public final Cache<Pair<Float, Float>> renderOffset = new Cache<>(() -> {
        if (getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock block) {
            return block.getRenderOffset(level, getBlockState(), worldPosition);
        }
        return Tripple.of(0.0F, 0.0F);
    });

    public final Cache<Pair<Float, Float>> renderZOffset = new Cache<>(() -> {
        if (getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock block) {
            return block.getRenderZOffset(level, getBlockState(), worldPosition);
        }
        return Tripple.of(0.0F, 0.0F);
    });

    public final Cache<Pair<Float, Float>> renderAspectRatio = new Cache<>(() -> {
        if (getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock block) {
            Pair<Float, Float> raw = block.getRenderAspectRatio(level, getBlockState(), worldPosition);
            float scale = 1.0f / Math.min(raw.getFirst(), raw.getSecond());
            return Pair.of(raw.getFirst() * scale, raw.getSecond() * scale);
        }
        return Pair.of(1.0F, 1.0F);
    });

    public final Cache<Float> renderScale = new Cache<>(() -> {        
        return 1.0F / Math.max(this.renderAspectRatio.get().getFirst(), this.renderAspectRatio.get().getSecond());
    }); 


    public AdvancedDisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TrainData getTrainData() {
        return trainData;
    }

    public CarriageData getCarriageData() {
        return carriageData;
    }

    public long getLastRefreshedTime() {
        return lastRefreshedTime;
    }

    public byte getXSize() {
        return xSize;
    }

    public byte getXSizeScaled() {
        return (byte)(getXSize() * renderAspectRatio.get().getFirst());
    }

    public byte getYSize() {
        return ySize;
    }

    public byte getYSizeScaled() {
        return (byte)(getYSize() * renderAspectRatio.get().getSecond());
    }

    public boolean isController() {
        return isController;
    }

    @Override
    public int getColor() {
        return color;
    }    

    @Override
    public boolean isGlowing() {
        return glowing;
    }
    
    public EDisplayInfo getInfoType() {
        return infoType;
    }

    public EDisplayType getDisplayType() {
        return displayType;
    }

    public void setColor(int color) {
		this.color = color;
        if (level.isClientSide) {
            getRenderer().update(level, worldPosition, getBlockState(), this);
        }
        BlockEntityUtil.sendUpdatePacket(this);
        this.setChanged();
    }

    public void setGlowing(boolean glowing) {
		this.glowing = glowing;
        if (level.isClientSide) {
            getRenderer().update(level, worldPosition, getBlockState(), this);
        }
        BlockEntityUtil.sendUpdatePacket(this);
        this.setChanged();
    }

    public void setInfoType(EDisplayInfo type) {
		this.infoType = type;
        if (level.isClientSide) {
            getRenderer().update(level, worldPosition, getBlockState(), this);
        }
        BlockEntityUtil.sendUpdatePacket(this);
        this.setChanged();
    }

    public void setDisplayType(EDisplayType type) {
		this.displayType = type;
        if (level.isClientSide) {
            getRenderer().update(level, worldPosition, getBlockState(), this);
        }
        BlockEntityUtil.sendUpdatePacket(this);
        this.setChanged();
    }

    
    @Override
    public void tick() {
        if (level.isClientSide) {
            getRenderer().tick(level, getBlockPos(), getBlockState(), this);
        }
        super.tick();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        updateControllerStatus();
    }

    public void updateControllerStatus() {
		if (level.isClientSide)
			return;

		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof AbstractAdvancedDisplayBlock))
			return;

		Direction leftDirection = blockState.getValue(AbstractAdvancedDisplayBlock.FACING).getClockWise();
		boolean shouldBeController = (((AbstractAdvancedDisplayBlock)blockState.getBlock()).isSingleLine(level, worldPosition) || !blockState.getValue(AbstractAdvancedDisplayBlock.UP)) && level.getBlockState(worldPosition.relative(leftDirection)) != blockState;

		byte newXSize = 1;
		byte newYSize = 1;

		if (shouldBeController) {
			for (int xOffset = 1; xOffset < 32; xOffset++) {
				if (level.getBlockState(worldPosition.relative(leftDirection.getOpposite(), xOffset)) != blockState)
					break;
				newXSize++;
			}

            if (!((AbstractAdvancedDisplayBlock)blockState.getBlock()).isSingleLine(level, worldPosition)) {                
                for (int yOffset = 0; yOffset < 32; yOffset++) {
                    if (!level.getBlockState(worldPosition.relative(Direction.DOWN, yOffset)).getOptionalValue(AbstractAdvancedDisplayBlock.DOWN).orElse(false))
                        break;
                    newYSize++;
                }
            }
		}

		if (isController == shouldBeController && newXSize == xSize && newYSize == ySize)
			return;

		isController = shouldBeController;
		xSize = newXSize;
		ySize = newYSize;
		sendData();
	}

    public AdvancedDisplayBlockEntity getController() {
		if (isController)
			return this;

		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof AbstractAdvancedDisplayBlock))
			return null;

		MutableBlockPos pos = getBlockPos().mutable();
		Direction side = blockState.getValue(AbstractAdvancedDisplayBlock.FACING)
			.getClockWise();

		for (int i = 0; i < 64; i++) {
			BlockState other = level.getBlockState(pos);

			if (!((AbstractAdvancedDisplayBlock)blockState.getBlock()).isSingleLine(level, worldPosition) && other.getOptionalValue(AbstractAdvancedDisplayBlock.UP).orElse(false)) {
				pos.move(Direction.UP);
				continue;
			}

			if ((level.getBlockState(pos.relative(side)).getBlock() instanceof AbstractAdvancedDisplayBlock block && block.isSingleLine(level, worldPosition)) || !level.getBlockState(pos.relative(side)).getOptionalValue(AbstractAdvancedDisplayBlock.UP).orElse(true)) {
				pos.move(side);
				continue;
			}

			BlockEntity found = level.getBlockEntity(pos);
			if (found instanceof AdvancedDisplayBlockEntity flap && flap.isController)
				return flap;

			break;
		}

		return null;
	}

    @Override
    public boolean connectedTo(AdvancedDisplayBlockEntity otherBlockEntity) {
        return otherBlockEntity.getBlockState().is(this.getBlockState().getBlock()) && (
            getBlockState().getValue(AbstractAdvancedDisplayBlock.SIDE) == ESide.BOTH ?
                otherBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING).getAxis() == this.getBlockState().getValue(HorizontalDirectionalBlock.FACING).getAxis()
            :
                otherBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING) == this.getBlockState().getValue(HorizontalDirectionalBlock.FACING)
        );
    }

    @Override
    public void contraptionTick(Level level, BlockPos pos, BlockState state, Contraption contraption) {
        getRenderer().tick(level, pos, state, this);
        
        if (!isController()) {
            return;
        }

        syncTicks++;
        syncTicks %= 100;

        CarriageContraption carriage = (CarriageContraption)contraption;        
        if (syncTicks == 0) {
            long id = InstanceManager.registerClientTrainDataResponseAction((data, refreshTime) -> {
                boolean b = false;
                if (trainData != null && trainData.getNextStop().isPresent() && data.getNextStop().isPresent()) {
                    SimpleDeparturePrediction prediction = trainData.getNextStop().get();
                    b = !trainData.trainName().equals(data.trainName()) ||
                        !prediction.scheduleTitle().equals(data.predictions().get(0).scheduleTitle()) ||
                        !prediction.stationName().equals(data.predictions().get(0).stationName()) ||
                        trainData.getNextStop().get().exitSide() != data.getNextStop().get().exitSide() ||
                        (getInfoType() == EDisplayInfo.INFORMATIVE && getDisplayType() == EDisplayType.PASSENGER_INFORMATION && trainData.getNextStop().get().departureTicks() + lastRefreshedTime != data.getNextStop().get().departureTicks() + refreshTime) // It's not clean but it works ... for now
                    ;
                }
                this.lastRefreshedTime = refreshTime;
                this.trainData = data;
                carriageData = new CarriageData(((CarriageContraptionEntity)carriage.entity).carriageIndex, carriage.getAssemblyDirection(), data.isOppositeDirection());
                relativeExitDirection.clear();

                if (b) {
                    getRenderer().update(level, pos, state, this);
                }
            });
            NetworkManager.getInstance().sendToServer(Minecraft.getInstance().getConnection().getConnection(), new TrainDataRequestPacket(id, ((CarriageContraptionEntity)carriage.entity).trainId, true));
        }
    }    

    @Override
    protected void write(CompoundTag pTag, boolean clientPacket) {
        super.write(pTag, clientPacket);
        pTag.putByte(NBT_XSIZE, getXSize());
        pTag.putByte(NBT_YSIZE, getYSize());
        pTag.putInt(NBT_COLOR, getColor());
        pTag.putBoolean(NBT_CONTROLLER, isController());
        pTag.putInt(NBT_INFO_TYPE, getInfoType().getId());
        pTag.putInt(NBT_DISPLAY_TYPE, getDisplayType().getId());
    }

    @Override
    public void read(CompoundTag pTag, boolean clientPacket) {
		super.read(pTag, clientPacket);
        xSize = pTag.getByte(NBT_XSIZE);
        ySize = pTag.getByte(NBT_YSIZE);
        color = pTag.getInt(NBT_COLOR);
        isController = pTag.getBoolean(NBT_CONTROLLER);
        infoType = EDisplayInfo.getTypeById(pTag.getInt(NBT_INFO_TYPE));
        displayType = EDisplayType.getTypeById(pTag.getInt(NBT_DISPLAY_TYPE));
    }    

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_INFO_TYPE, getInfoType().getId());
        nbt.putInt(NBT_DISPLAY_TYPE, getDisplayType().getId());
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        infoType = EDisplayInfo.getTypeById(nbt.getInt(NBT_INFO_TYPE));
        displayType = EDisplayType.getTypeById(nbt.getInt(NBT_DISPLAY_TYPE));
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB aabb = new AABB(worldPosition);
        if (!isController)
            return aabb;
        Vec3i normal = getDirection().getClockWise().getNormal();
        return aabb.expandTowards(normal.getX() * getXSize(), 0, normal.getZ() * getXSize());
    }

    public Direction getDirection() {
		return getBlockState().getOptionalValue(FlapDisplayBlock.HORIZONTAL_FACING)
			.orElse(Direction.SOUTH)
			.getOpposite();
	}

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithFullMetadata();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
        this.level.markAndNotifyBlock(this.worldPosition, this.level.getChunkAt(this.worldPosition), this.getBlockState(), this.getBlockState(), 3, 512);
    }

    @Override
    public IBlockEntityRendererInstance<AdvancedDisplayBlockEntity> getRenderer() {
        return renderer.get();
    }

	@Override
	protected AABB createRenderBoundingBox() {
		AABB aabb = new AABB(worldPosition);
		if (!isController)
			return aabb;
		Vec3i normal = getDirection().getClockWise().getNormal();
		return aabb.expandTowards(normal.getX() * xSize, -ySize, normal.getZ() * xSize);
	}

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public byte getMaxWidth() {
        return MAX_XSIZE;
    }

    @Override
    public byte getMaxHeight() {
        return MAX_YSIZE;
    }

    @Override
    public byte getWidth() {
        return xSize;
    }

    @Override
    public byte getHeight() {
        return ySize;
    }

    @Override
    public Class<AbstractAdvancedDisplayBlock> getBlockType() {
        return AbstractAdvancedDisplayBlock.class;
    }

    @Override
    public Class<AdvancedDisplayBlockEntity> getBlockEntityType() {
        return AdvancedDisplayBlockEntity.class;
    }
}