package de.mrjulsen.crn.block.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.IBlockGetter;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.StaticTextDisplaySettings;
import de.mrjulsen.crn.block.properties.EDisplayType;
import de.mrjulsen.crn.block.properties.EDisplayType.EDisplayTypeDataSource;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.registry.ModDisplayTypes;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public class AdvancedDisplayTarget extends DisplayTarget {
	
	private static boolean running = false;
	private static boolean threadRunning = false;
	private static final Queue<Runnable> workerTasks = new ConcurrentLinkedQueue<>();

	public static void start() {
		if (running) stop();		
		while (running && threadRunning) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException ignored) {}
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
						CreateRailwaysNavigator.LOGGER.info("Error while process Advanced Display Data. " + e.getMessage(), e);
					}
				}
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException ignored) {}
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
	public void acceptText(final int line, List<MutableComponent> text, DisplayLinkContext context) {
		CompoundTag nbt = context.sourceConfig();

		if (context.getTargetBlockEntity() instanceof AdvancedDisplayBlockEntity blockEntity && ModCommonEvents.hasServer()) {
			final AdvancedDisplayBlockEntity controller = blockEntity.getController(new IBlockGetter.WorldBlockGetter(blockEntity.getLevel()));
			if (controller == null) {
				return;
			}

			long dayTime = context.getTargetBlockEntity().getLevel().getDayTime();
			boolean advancedDisplaySource = context.blockEntity().activeSource instanceof AdvancedDisplaySource;//nbt.contains(AdvancedDisplaySource.NBT_ADVANCED_DISPLAY);

			queueAdvancedDisplayWorkerTask(() -> {
				if (advancedDisplaySource) {
					String filter = context.sourceConfig().getString("Filter");

					if (controller.getDisplayType().category().getSource() != EDisplayTypeDataSource.PLATFORM) {
						if (!ModCommonConfig.AUTO_UPDATE_DISPLAY_TYPE.get()) return;
						if (controller.getDisplayType().category() != EDisplayType.PLATFORM) {
							AdvancedDisplaysRegistry.DisplayTypeResourceKey displayType;
							if (filter.contains("*")) {
								displayType = ModDisplayTypes.DEPARTURE_BOARD_TABLE;
							} else if (controller.getDisplayProperties().singleLined()) {
								displayType = ModDisplayTypes.PLATFORM_RUNNING_TEXT;
							} else {
								displayType = ModDisplayTypes.PLATFORM_TABLE;
							}
							ModCommonEvents.getCurrentServer().ifPresent(server -> server.executeIfPossible(() -> {
								controller.applyToAll(x -> {
									x.setDisplayType(displayType, null);
									x.notifyUpdate();
								});
							}));
						}
					}

					List<StationDisplayData> preds = prepare(filter, controller.getDisplayProperties().platformDisplayTrainsCount().apply(controller));
					controller.setData(
							preds,
							filter,
							GlobalSettings.getInstance().getOrCreateStationTagFor(filter).getInfoForStation(filter),
							dayTime
					);
					ModCommonEvents.getCurrentServer().ifPresent(x -> x.executeIfPossible(controller::notifyUpdate));
				} else {
					if (controller.getDisplayType() != ModDisplayTypes.RICH_TEXT) {
						if (!ModCommonConfig.AUTO_UPDATE_DISPLAY_TYPE.get()) return;
					}

					StaticTextDisplaySettings settings = controller.getSettingsAs(StaticTextDisplaySettings.class).orElse(new StaticTextDisplaySettings());
					int currentLine = line;
					for (MutableComponent comp : text) {
						StaticTextDisplaySettings.TextComponent component = new StaticTextDisplaySettings.TextComponent(Component.Serializer.toJson(comp, RegistryAccess.EMPTY));
						component.setTextAlignment(EAlignment.LEFT);
						component.setXScale(0.4f);
						component.setMinXScale(0.4f);
						component.setYScale(0.4f);
						component.setY(currentLine * 5.5f);
						if (currentLine >= settings.getComponentsCount()) {
							settings.addComponent(component);
						} else {
							settings.setComponent(currentLine, component);
						}
						currentLine++;
						if (currentLine >= controller.getYSize() * 3 - 1) {
							break;
						}
					}
					ModCommonEvents.getCurrentServer().ifPresent(x -> x.executeIfPossible(() -> controller.applyToAll(a -> {
						a.setDisplayType(ModDisplayTypes.RICH_TEXT, settings);
						a.notifyUpdate();
					})));
				}
			});

		}
	}

	public static List<StationDisplayData> prepare(String filter, int maxLines) {
		List<StationDisplayData> result = new ArrayList<>(maxLines);

		int i = 0;
		for (TrainStop stop : TrainUtils.getDeparturesAtStationName(filter, null, false)) {
			i++;
			result.add(StationDisplayData.of(stop));
			if (i >= maxLines) {
				break;
			}
		}
		return result;
	}

	@Override
	public boolean isReserved(int line, BlockEntity target, DisplayLinkContext context) {
		return super.isReserved(line, target, context) || target instanceof AdvancedDisplayBlockEntity;
	}

	@Override
	public DisplayTargetStats provideStats(DisplayLinkContext context) {
		AdvancedDisplayBlockEntity controller = getController(context);
		if (controller == null)
			return new DisplayTargetStats(1, 1024, this);

		return new DisplayTargetStats(context.blockEntity().activeSource instanceof AdvancedDisplaySource ? 1 : 50, 1024, this);
	}

	@Override
	public Component getLineOptionText(int line) {
		return TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".display_target.advanced_display.component", line + 1);
	}

	private AdvancedDisplayBlockEntity getController(DisplayLinkContext context) {
		BlockEntity teIn = context.getTargetBlockEntity();
		if (!(teIn instanceof AdvancedDisplayBlockEntity be))
			return null;
		return be.getController(new IBlockGetter.WorldBlockGetter(be.getLevel()));
	}

	@Override
	public AABB getMultiblockBounds(LevelAccessor level, BlockPos pos) {
		AABB baseShape = super.getMultiblockBounds(level, pos);
		BlockEntity be = level.getBlockEntity(pos);

		if (!(be instanceof AdvancedDisplayBlockEntity fdbe))
			return baseShape;

		AdvancedDisplayBlockEntity controller = fdbe.getController(new IBlockGetter.WorldBlockGetter(fdbe.getLevel()));
		if (controller == null)
			return baseShape;

		Vec3i normal = controller.getDirection().getClockWise().getNormal();
		return baseShape.move(controller.getBlockPos().subtract(pos)).expandTowards(normal.getX() * (controller.getXSize() - 1), 1 - controller.getYSize(), normal.getZ() * (controller.getXSize() - 1));
	}
}