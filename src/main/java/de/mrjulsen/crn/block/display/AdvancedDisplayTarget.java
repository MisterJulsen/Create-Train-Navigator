package de.mrjulsen.crn.block.display;

import java.util.List;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AdvancedDisplayTarget extends DisplayTarget {
    
    @Override
	public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {}

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

		Vec3i normal = controller.getDirection()
			.getClockWise()
			.getNormal();
		return baseShape.move(controller.getBlockPos()
			.subtract(pos))
			.expandTowards(normal.getX() * (controller.getXSize() - 1), 1 - controller.getYSize(), normal.getZ() * (controller.getXSize() - 1));
	}
}