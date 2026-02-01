package de.mrjulsen.crn.block.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.IBlockGetter;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.SimpleStaticTextDisplaySettings;
import de.mrjulsen.crn.block.display.properties.StaticTextDisplaySettings;
import de.mrjulsen.crn.block.display.properties.components.IShowTrainMultipleTimes;
import de.mrjulsen.crn.block.display.properties.components.ITrainStopTypeSetting;
import de.mrjulsen.crn.block.properties.EDisplayType;
import de.mrjulsen.crn.block.properties.EDisplayType.EDisplayTypeDataSource;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.registry.ModDisplayTypes;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.core.BlockPos;
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

					List<StationDisplayData> preds = prepare(filter, controller.getDisplayProperties().platformDisplayTrainsCount().apply(controller), controller);
					controller.setData(
							preds,
							filter,
							GlobalSettings.getInstance().getOrCreateStationTagFor(filter).getInfoForStation(filter),
							dayTime
					);
					ModCommonEvents.getCurrentServer().ifPresent(x -> x.executeIfPossible(controller::notifyUpdate));
				} else if (controller.getDisplayType().equals(ModDisplayTypes.SIMPLE_TEXT)) {
					SimpleStaticTextDisplaySettings settings = controller
							.getSettingsAs(SimpleStaticTextDisplaySettings.class)
							.orElse(new SimpleStaticTextDisplaySettings());
					settings.setStaticText(Component.Serializer.toJson((text.get(0))));
					CreateRailwaysNavigator.LOGGER.debug(settings.getStaticText());
					ModCommonEvents.getCurrentServer()
							.ifPresent(x -> x.executeIfPossible(() -> controller.applyToAll(a -> {
								a.setDisplayType(ModDisplayTypes.SIMPLE_TEXT, settings);
								a.notifyUpdate();
							}, new IBlockGetter.WorldBlockGetter(blockEntity.getLevel()))));
				} else {
					if (!controller.getDisplayType().equals(ModDisplayTypes.RICH_TEXT)) {
						if (!ModCommonConfig.AUTO_UPDATE_DISPLAY_TYPE.get())
							return;
					}

					StaticTextDisplaySettings settings = controller.getSettingsAs(StaticTextDisplaySettings.class)
							.orElse(new StaticTextDisplaySettings());
					// Loop through the entire available space to make sure any stragglers are taken
					// care of.
					for (int i = 0; i < controller.getYSize() * 3 - line - 1; i++) {
						final int componentIndex = i + line;
						if (i == 0)
							reserve(componentIndex, controller, context);
						if (i > 0 && isReserved(componentIndex, controller, context))
							break;

						while (componentIndex >= settings.getComponentsCount()) {
							settings.addComponent(new StaticTextDisplaySettings.TextComponent("{\"text\":\"\"}"));
						}
						StaticTextDisplaySettings.TextComponent component = settings.getComponents()
								.get(componentIndex);
						if (i >= text.size()) {
							if (context.blockEntity().activeSource instanceof SingleLineDisplaySource)
								break;
							component.setStaticText("{\"text\":\"\"}");
						} else
							component.setStaticText(Component.Serializer.toJson(text.get(i)));
						component.setTextAlignment(ETextAlignment.LEFT);
						component.setXScale(0.4f);
						component.setMinXScale(0.4f);
						component.setYScale(0.4f);
						component.setY((componentIndex) * 5.5f);
						settings.setComponent(componentIndex, component);
					}
					ModCommonEvents.getCurrentServer()
							.ifPresent(x -> x.executeIfPossible(() -> controller.applyToAll(a -> {
								a.setDisplayType(ModDisplayTypes.RICH_TEXT, settings);
								a.notifyUpdate();
							})));
				}
			});

		}
	}

	public static List<StationDisplayData> prepare(String filter, int maxLines, AdvancedDisplayBlockEntity controller) {
		List<StationDisplayData> result = new ArrayList<>(maxLines);

		int i = 0;
		for (TrainStop stop : TrainUtils.getDeparturesAtStationName(filter, null, false, controller.getSettingsAs(IShowTrainMultipleTimes.class).map(IShowTrainMultipleTimes::showTrainMultipleTimes).orElse(false))) {
			StationDisplayData data = StationDisplayData.of(stop);
			boolean cancelled = data.getTrainData().isCancelled();
			boolean isStillValid = DragonLib.getCurrentWorldTime() < data.getStationData().getScheduledDepartureTime() + ModClientConfig.DISPLAY_LEAD_TIME.get();
			boolean terminus = data.isNextSectionExcluded();
			boolean start = data.isPrevSectionExcluded();

			ITrainStopTypeSetting.ETrainStopType type = controller.getSettingsAs(ITrainStopTypeSetting.class).map(ITrainStopTypeSetting::getTrainStopType).orElse(ITrainStopTypeSetting.ETrainStopType.ALL);
			boolean showArrival = type.showArrivals(terminus) && !start;
			boolean showDeparture = type.showDepartures(start) && !terminus;

			boolean allowed = showArrival || showDeparture;
			if (!allowed && (!cancelled || isStillValid)) {
				continue;
			}

			result.add(StationDisplayData.of(stop));
			if ((i++) >= maxLines) {
				break;
			}
		}
		return result;
	}

	@Override
	public boolean isReserved(int line, BlockEntity target, DisplayLinkContext context) {
		if (target instanceof AdvancedDisplayBlockEntity) {
			AdvancedDisplayBlockEntity controller = (AdvancedDisplayBlockEntity) target;
			if (controller.getDisplayType().equals(ModDisplayTypes.SIMPLE_TEXT)
					|| controller.getDisplayType().equals(ModDisplayTypes.RICH_TEXT))
				return super.isReserved(line, target, context);
			else
				return true;
		} else
			return super.isReserved(line, target, context);
	}

	@Override
	public DisplayTargetStats provideStats(DisplayLinkContext context) {
		AdvancedDisplayBlockEntity controller = getController(context);
		if (controller == null)
			return new DisplayTargetStats(1, 1024, this);
		float textScale = 0.4f;
		int maxRows = 50;
		int maxColumns = 1024;
		if (controller.getDisplayType().equals(ModDisplayTypes.SIMPLE_TEXT)){
			textScale = 0.75f;
			maxRows = 1;
		} else if (controller.getDisplayType().equals(ModDisplayTypes.RICH_TEXT)){
			maxColumns = controller.getYSize() * 3 - 1;
		}
		if (context.blockEntity().activeSource instanceof PercentOrProgressBarDisplaySource)
			maxColumns = (int) ((controller.getXSizeScaled() * 16 - 3) / 9.0f / textScale);
		if(context.blockEntity().activeSource instanceof AdvancedDisplaySource)
			maxRows = 1;

		maxRows = Math.min(maxRows, 50);

		return new DisplayTargetStats(maxRows, maxColumns, this);
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