package de.mrjulsen.crn.block.display;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayBoardTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.properties.EDisplayType.EDisplayTypeDataSource;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public class AdvancedDisplayTarget extends DisplayBoardTarget {
	
	private static boolean running = false;
	private static boolean threadRunning = false;
	private static final Queue<Runnable> workerTasks = new ConcurrentLinkedQueue<>();

	public static void start() {
		if (running) stop();		
		while (running && threadRunning) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {}
		}

		workerTasks.clear();
		running = true;
		new Thread(() -> {
			threadRunning = true;
			CreateRailwaysNavigator.LOGGER.info("Advanced Display Data Manager has been started.");
						
			while (running) {
				while (!workerTasks.isEmpty()) {
					try {	
						workerTasks.poll().run();						
					} catch (Exception e) {
						CreateRailwaysNavigator.LOGGER.info("Error while process Advanced Display Data.", e);
					}
				}
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {}
			}
			workerTasks.clear();
			CreateRailwaysNavigator.LOGGER.info("Advanced Display Data Manager has been stopped.");
			threadRunning = false;
		}, "Advanced Display Data Manager").start();
	}

	public static void stop() {
		CreateRailwaysNavigator.LOGGER.info("Stopping Advanced Display Data Manager...");
		running = false;
	}

	private static void queueAdvancedDisplayWorkerTask(Runnable task) {
		workerTasks.add(task);
	}
    
	@Override
	public void acceptFlapText(int line, List<List<MutableComponent>> text, DisplayLinkContext context) {

		CompoundTag nbt = context.sourceConfig();
		if (!nbt.contains(AdvancedDisplaySource.NBT_ADVANCED_DISPLAY)) {
			return;
		}

		String filter = context.sourceConfig().getString("Filter");

		if (context.getTargetBlockEntity() instanceof AdvancedDisplayBlockEntity blockEntity) {
			final AdvancedDisplayBlockEntity controller = blockEntity.getController();
			long dayTime = context.getTargetBlockEntity().getLevel().getDayTime();

			queueAdvancedDisplayWorkerTask(() -> {
				if (controller != null & controller.getDisplayTypeKey().category().getSource() == EDisplayTypeDataSource.PLATFORM) {
					List<StationDisplayData> preds = prepare(filter, controller.getDisplayTypeInfo().platformDisplayTrainsCount().apply(controller));
					
					controller.setDepartureData(
						preds,
						filter,
						GlobalSettings.getInstance().getOrCreateStationTagFor(filter).getInfoForStation(filter),
						dayTime,
						(byte)context.sourceConfig().getInt(AdvancedDisplaySource.NBT_PLATFORM_WIDTH),
						(byte)context.sourceConfig().getInt(AdvancedDisplaySource.NBT_TRAIN_NAME_WIDTH),
						context.sourceConfig().getByte(AdvancedDisplaySource.NBT_TIME_DISPLAY_TYPE)
					);
					if (ModCommonEvents.hasServer()) {
						ModCommonEvents.getCurrentServer().get().executeIfPossible(controller::sendData);
					}
				}
			});
		}
	}

	public static List<StationDisplayData> prepare(String filter, int maxLines) {
		return TrainUtils.getDeparturesAtStationName(filter, null).stream().limit(maxLines).map(x -> StationDisplayData.of(x)).toList();
	}

	@Override
	public boolean isReserved(int line, BlockEntity target, DisplayLinkContext context) {
		return super.isReserved(line, target, context) || target instanceof AdvancedDisplayBlockEntity;
	}

	@Override
	public DisplayTargetStats provideStats(DisplayLinkContext context) {
		AdvancedDisplayBlockEntity controller = getController(context);
		if (controller == null)
			return new DisplayTargetStats(1, 1, this);
		return new DisplayTargetStats(1, 1, this);
	}

	private AdvancedDisplayBlockEntity getController(DisplayLinkContext context) {
		BlockEntity teIn = context.getTargetBlockEntity();
		if (!(teIn instanceof AdvancedDisplayBlockEntity be))
			return null;
		return be.getController();
	}

	@Override
	public AABB getMultiblockBounds(LevelAccessor level, BlockPos pos) {
		AABB baseShape = super.getMultiblockBounds(level, pos);
		BlockEntity be = level.getBlockEntity(pos);

		if (!(be instanceof AdvancedDisplayBlockEntity fdbe))
			return baseShape;

		AdvancedDisplayBlockEntity controller = fdbe.getController();
		if (controller == null)
			return baseShape;

		Vec3i normal = controller.getDirection().getClockWise().getNormal();
		return baseShape.move(controller.getBlockPos().subtract(pos)).expandTowards(normal.getX() * (controller.getXSize() - 1), 1 - controller.getYSize(), normal.getZ() * (controller.getXSize() - 1));
	}
}