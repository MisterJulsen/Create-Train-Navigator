package de.mrjulsen.crn.block.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.trains.display.FlapDisplayBlock;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource.ETimeDisplay;
import de.mrjulsen.crn.block.properties.EDisplayInfo;
import de.mrjulsen.crn.block.properties.EDisplayType;
import de.mrjulsen.crn.block.properties.EDisplayType.EDisplayTypeDataSource;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeInfo;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeResourceKey;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.data.CarriageData;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainStopDisplayData;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.registry.ModDisplayTypes;
import de.mrjulsen.mcdragonlib.block.IBERInstance;
import de.mrjulsen.mcdragonlib.client.ber.IBlockEntityRendererInstance;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import de.mrjulsen.mcdragonlib.util.ListUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
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
    IColorableBlockEntity
{
    private static final String NBT_XSIZE = "XSize";
    private static final String NBT_YSIZE = "YSize";
    private static final String NBT_CONTROLLER = "IsController";
    private static final String NBT_COLOR = "Color";
    private static final String NBT_GLOWING = "Glowing";
    private static final String NBT_DISPLAY_TYPE_KEY = "DisplayTypeKey";
    private static final String NBT_FILTER = "Filter";
    private static final String NBT_LAST_REFRESH_TIME = "LastRefreshed";
    private static final String NBT_PLATFORM_WIDTH = "PlatformWidth";
    private static final String NBT_TRAIN_NAME_WIDTH = "TrainNameWidth";
    private static final String NBT_TIME_DISPLAY = "TimeDisplay";
    private static final String NBT_TRAIN_STOPS = "TrainStops";
    @Deprecated private static final String LEGACY_NBT_INFO_TYPE = "InfoType";
    @Deprecated private static final String LEGACY_NBT_DISPLAY_TYPE = "DisplayType";

    public static final byte MAX_XSIZE = 16;
    public static final byte MAX_YSIZE = 16;

    private static final int REFRESH_FREQUENCY = 100;

    // DATA
    private byte xSize = 1;
	private byte ySize = 1;
    private boolean isController;
    private List<StationDisplayData> predictions;
    private boolean dataOrderChanged = false;
    private String stationNameFilter;
    private StationInfo stationInfo;
    private byte trainNameWidth;
    private byte platformWidth;
    private ETimeDisplay timeDisplay = ETimeDisplay.ABS; 

    // USER SETTINGS
    private int color = DyeColor.WHITE.getTextColor();
    private boolean glowing = false;
    private DisplayTypeResourceKey displayTypeKey = ModDisplayTypes.TRAIN_DESTINATION_SIMPLE;
    

    // CLIENT DISPLAY ONLY - this data is not saved!
    private long lastRefreshedTime;
    private TrainDisplayData trainData = TrainDisplayData.empty();
    private CarriageData carriageData = new CarriageData(0, Direction.NORTH, false);
    
    // OTHER
    private int syncTicks = REFRESH_FREQUENCY - 1;
    private final Cache<IBlockEntityRendererInstance<AdvancedDisplayBlockEntity>> renderer = new Cache<>(() -> new AdvancedDisplayRenderInstance(this));

    public final Cache<TrainExitSide> relativeExitDirection = new Cache<>(() -> {        
        if (getCarriageData() == null || !getTrainData().getNextStop().isPresent() || !(getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock)) {
            return TrainExitSide.UNKNOWN;
        }
        TrainExitSide side = getTrainData().getNextStopExitSide();
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

    public final Cache<Tripple<Float, Float, Float>> renderRotation = new Cache<>(() -> {
        if (getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock block) {
            return block.getRenderRotation(level, getBlockState(), worldPosition);
        }
        return Tripple.of(0.0F, 0.0F, 0.0F);
    });

    public final Cache<Pair<Float, Float>> renderOffset = new Cache<>(() -> {
        if (getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock block) {
            return block.getRenderOffset(level, getBlockState(), worldPosition);
        }
        return Pair.of(0.0F, 0.0F);
    });

    public final Cache<Pair<Float, Float>> renderZOffset = new Cache<>(() -> {
        if (getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock block) {
            return block.getRenderZOffset(level, getBlockState(), worldPosition);
        }
        return Pair.of(0.0F, 0.0F);
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
        reset();
    }

    public TrainDisplayData getTrainData() {
        return trainData;
    }

    public CarriageData getCarriageData() {
        return carriageData;
    }

    public long getLastRefreshedTime() {
        return lastRefreshedTime;
    }

    public byte getTrainNameWidth() {
        return trainNameWidth;
    }

    public ETimeDisplay getTimeDisplay() {
        return timeDisplay;
    }

    public byte getPlatformWidth() {
        return platformWidth;
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

    public DisplayTypeResourceKey getDisplayTypeKey() {
        return displayTypeKey;
    }
    
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

    public List<StationDisplayData> getStops() {
        return predictions;
    }

    public boolean isPlatformFixed() {
        return !stationNameFilter.contains("*");
    }

    public StationInfo getStationInfo2() {
        return stationInfo;
    }

    public String getStationNameFilter() {
        return stationNameFilter;
    }

	public boolean isSingleLine() {
		if (getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock block) {
			return block.isSingleLined() || AdvancedDisplaysRegistry.getInfo(displayTypeKey).singleLined();
		}
        return false;		
	}

    public DisplayTypeInfo getDisplayTypeInfo() {
        return AdvancedDisplaysRegistry.getInfo(displayTypeKey);
    }

    public void setColor(int color) {
		this.color = color;
        if (level.isClientSide) {
            getRenderer().update(level, worldPosition, getBlockState(), this, EUpdateReason.LAYOUT_CHANGED);
        }

    }

    public void setGlowing(boolean glowing) {
		this.glowing = glowing;
        if (level.isClientSide) {
            getRenderer().update(level, worldPosition, getBlockState(), this, EUpdateReason.LAYOUT_CHANGED);
        }

    }

    public void setDisplayTypeKey(DisplayTypeResourceKey key) {
        this.displayTypeKey = key;        
        if (level.isClientSide) {
            getRenderer().update(level, worldPosition, getBlockState(), this, EUpdateReason.LAYOUT_CHANGED);
        }
    }

    public void setDepartureData(List<StationDisplayData> predictions, String stationNameFilter, StationInfo staionInfo, long lastRefreshedTime, byte platformWidth, byte trainNameWidth, byte timeDisplayId) {
        this.dataOrderChanged = dataOrderChanged || !ListUtils.compareCollections(this.predictions, predictions, StationDisplayData::equals);
        
        this.predictions = predictions;
        this.stationNameFilter = stationNameFilter;
        this.stationInfo = staionInfo;
        this.lastRefreshedTime = lastRefreshedTime;
        this.platformWidth = platformWidth;
        this.trainNameWidth = trainNameWidth;
        this.timeDisplay = ETimeDisplay.getById(timeDisplayId);
        
    }
    
    @Override
    public boolean connectable(BlockGetter getter, BlockPos a, BlockPos b) {
        if (getter == null || a == null || b == null) {
            return false;
        }

        if (getter.getBlockEntity(a) instanceof AdvancedDisplayBlockEntity be1 && getter.getBlockEntity(b) instanceof AdvancedDisplayBlockEntity be2 && be1.getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock block1 && be2.getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock block2) {
            return block1 == block2 &&
                be1.getDisplayTypeKey().equals(be2.getDisplayTypeKey()) &&
                block1.canConnectWithBlock(getter, getter.getBlockState(a), getter.getBlockState(b)) && block2.canConnectWithBlock(getter, getter.getBlockState(b), getter.getBlockState(a)) && 
                (!a.above().equals(b) || (be1.getBlockState().getValue(AbstractAdvancedDisplayBlock.UP) && !be1.isSingleLine())) &&
                (!a.below().equals(b) || (be1.getBlockState().getValue(AbstractAdvancedDisplayBlock.DOWN) && !be1.isSingleLine()))
            ;
        }
        return false;
    }

    public AdvancedDisplayBlockEntity getController() {
		if (isController())
			return this;

		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof AbstractAdvancedDisplayBlock))
			return null;

		MutableBlockPos pos = getBlockPos().mutable();
		Direction side = blockState.getValue(AbstractAdvancedDisplayBlock.FACING).getClockWise();

        for (int i = 0; i < getMaxWidth(); i++) {
			if (connectable(level, pos, pos.relative(side))) {
				pos.move(side);
				continue;
			}

			BlockEntity found = level.getBlockEntity(pos);
			if (found instanceof AdvancedDisplayBlockEntity flap && flap.isController)
				return flap;

			break;
		}

		for (int i = 0; i < getMaxHeight(); i++) {
            if (connectable(level, pos, pos.relative(Direction.UP))) {
				pos.move(Direction.UP);
				continue;
			}

			BlockEntity found = level.getBlockEntity(pos);
			if (found instanceof AdvancedDisplayBlockEntity flap && flap.isController)
				return flap;

			break;
		}

		return null;
	}

    public void copyFrom(AdvancedDisplayBlockEntity other) {
        if (getColor() == other.getColor() &&
            getDisplayTypeKey().equals(other.getDisplayTypeKey()) &&
            isGlowing() == other.isGlowing()
        ) {
            return;
        }

        color = other.getColor();
        glowing = other.isGlowing();
        displayTypeKey = other.getDisplayTypeKey();
        notifyUpdate();
    }

    public void reset() {
        dataOrderChanged = true;

        predictions = List.of();
        stationNameFilter = "";
        platformWidth = -1;
        trainNameWidth = 14;
        xSize = 1;
        ySize = 1;
        isController = false;
        stationInfo = StationInfo.empty();
    }

    public void updateControllerStatus() {

        if (level.isClientSide) {
            return;
        }

		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof AbstractAdvancedDisplayBlock))
			return;

		Direction leftDirection = blockState.getValue(AbstractAdvancedDisplayBlock.FACING).getClockWise();
        boolean shouldBeController = !connectable(level, worldPosition, worldPosition.relative(leftDirection)) && !connectable(level, worldPosition, worldPosition.above());

		byte newXSize = 1;
		byte newYSize = 1;

		if (shouldBeController) {
			for (int xOffset = 1; xOffset < getMaxWidth(); xOffset++) {
                BlockPos relPos = worldPosition.relative(leftDirection.getOpposite(), xOffset);
				if (level.getBlockState(relPos) != blockState) {
                    break;
                }

				newXSize++;
			}

            if (!isSingleLine()) {
                for (int yOffset = 0; yOffset < getMaxHeight(); yOffset++) {
                    BlockPos downPos = worldPosition.relative(Direction.DOWN, yOffset);
                    
                    for (int i = 0; i < newXSize; i++) {
                        BlockPos relPos = downPos.relative(leftDirection.getOpposite(), i);
                        if (level.getBlockEntity(relPos) instanceof AdvancedDisplayBlockEntity be && be != this) {
                            be.copyFrom(this);
                        }
                    }

                    if (!connectable(level, downPos, downPos.below())) {
                        break;
                    }    
                    newYSize++;
                }
            }
		}

		if (isController == shouldBeController && newXSize == xSize && newYSize == ySize)
			return;
        
        isController = shouldBeController;
        xSize = newXSize;
        ySize = newYSize;

        if (!isController) {
            reset();
        }

        notifyUpdate();
	}    

    @Override
    public void tick() {
        if (level.isClientSide) {
            getRenderer().tick(level, getBlockPos(), getBlockState(), this);
        }

        super.tick();

        if (getDisplayTypeKey().category().getSource() != EDisplayTypeDataSource.PLATFORM) {
            return;
        }

        syncTicks++;
        if ((syncTicks %= REFRESH_FREQUENCY) == 0) {
            if (level.isClientSide) {
                boolean shouldUpdate = getStops().size() > 0 || dataOrderChanged;
                if (shouldUpdate) {
                    getRenderer().update(level, getBlockPos(), getBlockState(), this, dataOrderChanged ? EUpdateReason.LAYOUT_CHANGED : EUpdateReason.DATA_CHANGED);
                    dataOrderChanged = false;
                }
            }
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        updateControllerStatus();
    }

    @Override
    public void contraptionTick(Level level, BlockPos pos, BlockState state, CarriageContraption carriage) {
        getRenderer().tick(level, pos, state, this);

        if (!isController()) {
            return;
        }

        if (getDisplayTypeKey().category().getSource() != EDisplayTypeDataSource.TRAIN_INFORMATION) {
            return;
        }

        syncTicks++;       
        if ((syncTicks %= 100) == 0 && level.isClientSide) {
            DataAccessor.getFromServer(((CarriageContraptionEntity)carriage.entity).trainId, ModAccessorTypes.GET_TRAIN_DISPLAY_DATA, (data) -> { 
                if (data.isEmpty() && this.trainData.isEmpty()) {
                    return;
                }

                boolean shouldUpdate = false;
                if (this.trainData != null && this.trainData.getNextStop().isPresent() && data.getNextStop().isPresent()) {
                    TrainStopDisplayData prediction = this.trainData.getNextStop().get();

                    shouldUpdate = !this.trainData.getTrainData().getName().equals(data.getTrainData().getName()) ||
                        !prediction.getDestination().equals(data.getNextStop().get().getDestination()) ||
                        prediction.getStationEntryIndex() != data.getNextStop().get().getStationEntryIndex() ||
                        this.trainData.getNextStopExitSide() != data.getNextStopExitSide() ||
                        this.trainData.isWaitingAtStation() != data.isWaitingAtStation()
                        //(getInfoType() == EDisplayInfo.INFORMATIVE && getDisplayType() == EDisplayType.PASSENGER_INFORMATION && trainData.getNextStop().get().departureTicks() + lastRefreshedTime != data.getNextStop().get().departureTicks() + refreshTime) // It's not clean but it works ... for now
                    ;
                }
                boolean outOfService = this.trainData != null && !this.trainData.getTrainData().getId().equals(Constants.ZERO_UUID) && !data.getNextStop().isPresent();
                if (outOfService) {
                    shouldUpdate = true;
                }
                //this.lastRefreshedTime = refreshTime;
                this.trainData = outOfService ? TrainDisplayData.empty() : data;
                this.carriageData = new CarriageData(((CarriageContraptionEntity)carriage.entity).carriageIndex, carriage.getAssemblyDirection(), data.isOppositeDirection());
                this.relativeExitDirection.clear();
                
                if (shouldUpdate) {
                }
                getRenderer().update(level, pos, state, this, shouldUpdate ? EUpdateReason.LAYOUT_CHANGED : EUpdateReason.DATA_CHANGED);
            });
        }
    }    

    @Override
    protected void write(CompoundTag pTag, boolean clientPacket) {
        super.write(pTag, clientPacket);
        pTag.putByte(NBT_XSIZE, getXSize());
        pTag.putByte(NBT_YSIZE, getYSize());
        pTag.putInt(NBT_COLOR, getColor());
        pTag.putBoolean(NBT_CONTROLLER, isController());
        pTag.put(NBT_DISPLAY_TYPE_KEY, getDisplayTypeKey().toNbt());
        pTag.putString(NBT_FILTER, getStationNameFilter());
        pTag.putBoolean(NBT_GLOWING, isGlowing());
        pTag.putLong(NBT_LAST_REFRESH_TIME, getLastRefreshedTime());
        pTag.putByte(NBT_PLATFORM_WIDTH, getPlatformWidth());
        pTag.putByte(NBT_TRAIN_NAME_WIDTH, getTrainNameWidth());
        pTag.putByte(NBT_TIME_DISPLAY, getTimeDisplay().getId());

        getStationInfo2().writeNbt(pTag);

        if (getStops() != null && !getStops().isEmpty()) {            
            ListTag list = new ListTag();
            list.addAll(getStops().stream().map(x -> x.toNbt()).toList());
            pTag.put(NBT_TRAIN_STOPS, list);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void read(CompoundTag pTag, boolean clientPacket) {
        boolean updateClient = false;
        if (level != null && getBlockState() != null && level.isClientSide) {
            if (
                isController() != pTag.getBoolean(NBT_CONTROLLER) ||
                getXSize() != pTag.getByte(NBT_XSIZE) ||
                getYSize() != pTag.getByte(NBT_YSIZE) ||
                getPlatformWidth() != pTag.getByte(NBT_PLATFORM_WIDTH) ||
                getTrainNameWidth() != pTag.getByte(NBT_TRAIN_NAME_WIDTH) ||
                (getStops().isEmpty() ^ !pTag.contains(NBT_TRAIN_STOPS))
            ) {
                updateClient = true;
            }
        }

		super.read(pTag, clientPacket);

        StationInfo info = StationInfo.fromNbt(pTag);

        xSize = pTag.getByte(NBT_XSIZE);
        ySize = pTag.getByte(NBT_YSIZE);
        if (pTag.contains(NBT_COLOR)) {
            color = pTag.getInt(NBT_COLOR);
        }
        glowing = pTag.getBoolean(NBT_GLOWING);
        isController = pTag.getBoolean(NBT_CONTROLLER);
        if (pTag.contains(LEGACY_NBT_INFO_TYPE) && pTag.contains(LEGACY_NBT_DISPLAY_TYPE)) {
            displayTypeKey = ModDisplayTypes.legacy_getKeyForType(EDisplayType.getTypeById(pTag.getInt(LEGACY_NBT_DISPLAY_TYPE)), EDisplayInfo.getTypeById(pTag.getInt(LEGACY_NBT_INFO_TYPE)));
        } else {
            displayTypeKey = DisplayTypeResourceKey.fromNbt(pTag.getCompound(NBT_DISPLAY_TYPE_KEY));
        }
        setDepartureData(
            pTag.contains(NBT_TRAIN_STOPS) ? new ArrayList<>(pTag.getList(NBT_TRAIN_STOPS, Tag.TAG_COMPOUND).stream().map(x -> StationDisplayData.fromNbt((CompoundTag)x)).toList()) : new ArrayList<>(),
            pTag.getString(NBT_FILTER),
            info,
            pTag.getLong(NBT_LAST_REFRESH_TIME),
            pTag.getByte(NBT_PLATFORM_WIDTH),
            pTag.getByte(NBT_TRAIN_NAME_WIDTH),
            pTag.getByte(NBT_TIME_DISPLAY)
        );

        if (updateClient) {
            getRenderer().update(level, worldPosition, getBlockState(), this, EUpdateReason.LAYOUT_CHANGED);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB aabb = new AABB(worldPosition);
        if (!isController)
            return aabb;
        Vec3i normal = getDirection().getClockWise().getNormal();
        return aabb.expandTowards(normal.getX() * getXSize(), -getYSize(), normal.getZ() * getXSize());
    }

    public Direction getDirection() {
		return getBlockState().getOptionalValue(FlapDisplayBlock.HORIZONTAL_FACING)
			.orElse(Direction.SOUTH)
			.getOpposite();
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
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 512);
    }

    public static enum EUpdateReason {
        LAYOUT_CHANGED,
        DATA_CHANGED
    }

}