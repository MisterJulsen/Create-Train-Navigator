package de.mrjulsen.crn.block.display;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayBoardTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.data.DeparturePrediction;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainStop;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.data.SimpleTrainSchedule;
import de.mrjulsen.crn.data.SimulatedTrainSchedule;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public class AdvancedDisplayTarget extends DisplayBoardTarget {    
    
	@Override
	public void acceptFlapText(int line, List<List<MutableComponent>> text, DisplayLinkContext context) {

		CompoundTag nbt = context.sourceConfig();
		if (!nbt.contains(AdvancedDisplaySource.NBT_ADVANCED_DISPLAY)) {
			return;
		}

		String filter = context.sourceConfig().getString("Filter");

		if (context.getTargetBlockEntity() instanceof AdvancedDisplayBlockEntity blockEntity) {
			final AdvancedDisplayBlockEntity controller = blockEntity.getController();
			if (controller != null) {
				List<SimpleDeparturePrediction> preds = GlobalTrainDisplayData.prepare(filter, controller.getPlatformInfoLinesCount()).stream().map(x -> new DeparturePrediction(x).simplify()).sorted(Comparator.comparingInt(x -> x.departureTicks())).toList();
 				List<String> stopovers = new ArrayList<>();

				if (!preds.isEmpty()) {
					SimpleDeparturePrediction pred = preds.iterator().next();
					SimulatedTrainSchedule sched = SimpleTrainSchedule.of(TrainUtils.getTrainStopsSorted(pred.trainId(), context.blockEntity().getLevel())).simulate(TrainUtils.getTrain(pred.trainId()), 0, pred.stationName());
						
					List<TrainStop> stops = new ArrayList<>(sched.getAllStops());
					boolean foundStart = false;

					if (!stops.isEmpty()) {
						for (int i = 0; i < stops.size() - 1; i++) {
							TrainStop x = stops.get(i);
							if (foundStart) {
								stopovers.add(x.getStationAlias().getAliasName().get());
							}
							foundStart = foundStart || x.getPrediction().getStationName().equals(pred.stationName());
						}
					}
				}
				
				controller.setDepartureData(
					preds,
					stopovers,
					filter,
					GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(filter).getInfoForStation(filter),
					context.getTargetBlockEntity().getLevel().getDayTime(),
					(byte)context.sourceConfig().getInt(AdvancedDisplaySource.NBT_PLATFORM_WIDTH),
					(byte)context.sourceConfig().getInt(AdvancedDisplaySource.NBT_TRAIN_NAME_WIDTH)
				);
				controller.sendData();
			}
		}
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
		return new DisplayTargetStats(controller.getYSize() * 2, 150, this);
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