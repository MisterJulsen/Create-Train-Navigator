package de.mrjulsen.crn.block.display;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayBoardTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.data.DeparturePrediction;
import de.mrjulsen.crn.data.TrainStop;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AdvancedDisplayTarget extends DisplayBoardTarget {    
    
	@Override
	public void acceptFlapText(int line, List<List<MutableComponent>> text, DisplayLinkContext context) {		
		String filter = context.sourceConfig().getString("Filter");
		boolean fixedPlatform = !filter.contains("*");

		if (context.getTargetBlockEntity() instanceof AdvancedDisplayBlockEntity blockEntity) {
			AdvancedDisplayBlockEntity controller = blockEntity.getController();
			if (controller != null) {
				List<SimpleDeparturePrediction> preds = GlobalTrainDisplayData.prepare(filter, controller.getPlatformInfoLinesCount()).stream().map(x -> new DeparturePrediction(x).simplify()).sorted(Comparator.comparingInt(x -> x.departureTicks())).toList();
 				Set<String> stopovers = new HashSet<>();

				if (!preds.isEmpty()) {
					SimpleDeparturePrediction pred = preds.iterator().next();
					List<TrainStop> stops = new ArrayList<>(TrainUtils.getTrainStopsSorted(pred.trainId(), context.blockEntity().getLevel()).stream().skip(1).filter(x -> !GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(x.getStationAlias())).toList());
					for (int i = 0; i < stops.size() - 1; i++) {
						stopovers.add(stops.get(i).getStationAlias().getAliasName().get());
					}
				}
				
				controller.setDepartureData(preds, stopovers, fixedPlatform, context.getTargetBlockEntity().getLevel().getDayTime());
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
	@OnlyIn(Dist.CLIENT)
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