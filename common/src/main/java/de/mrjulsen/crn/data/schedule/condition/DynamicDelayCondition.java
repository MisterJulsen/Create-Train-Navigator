package de.mrjulsen.crn.data.schedule.condition;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.ResizableButton;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.crn.mixin.ModularGuiLineBuilderAccessor;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.ButtonState;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class DynamicDelayCondition extends ScheduledDelay {

    private static final String NBT_MIN = "Min";
    
    public DynamicDelayCondition() {
        super();
		data.putInt(NBT_MIN, 5);
    }

	@Override
	public Pair<ItemStack, Component> getSummary() {
		return Pair.of(new ItemStack(Items.COMPARATOR), TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.condition." + getId().getPath() + ".title", formatCustomTime(getMinValue(), true), formatTime(true)));
	}

    protected Component formatCustomTime(int time, boolean compact) {
		if (compact)
			return Components.literal(time + getUnit().suffix);
		return Components.literal(time + " ").append(Lang.translateDirect(getUnit().key));
	}

    @Override
	public List<Component> getTitleAs(String type) {
		return ImmutableList.of(
			TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath()),
			Lang.translateDirect("schedule.condition.for_x_time", formatTime(false)).withStyle(ChatFormatting.DARK_AQUA),
			TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".at_least", formatCustomTime(getMinValue(), false)).withStyle(ChatFormatting.DARK_AQUA)
        );
	}

	@Override
	public boolean tickCompletion(Level level, Train train, CompoundTag context) {
		int time = context.getInt("Time");

		long currentDelay = 0;
		long scheduledDepartureTime = 0;
		boolean initialized = false;

		if (TrainListener.data.containsKey(train.id)) {
			TrainData data = TrainListener.data.get(train.id);
			Optional<TrainPrediction> pred = data.getNextStopPrediction();
			if (pred.isPresent()) {
				currentDelay = pred.get().getArrivalTimeDeviation();
				initialized = data.isInitialized() && !data.isPreparing();
				scheduledDepartureTime = pred.get().getScheduledDepartureTime();
			} 
		}

		if (time >= (initialized ? Math.max(totalWaitTicks() - currentDelay, minWaitTicks()) : totalWaitTicks()) && (!initialized || DragonLib.getCurrentWorldTime() >= scheduledDepartureTime))
			return true;
		
		context.putInt("Time", time + 1);
		requestDisplayIfNecessary(context, time);
		return false;
	}

	@Override
	public ResourceLocation getId() {
		return new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "dynamic_delay");
	}
    
	public int getMinValue() {
		return intData(NBT_MIN);
	}
	
	public int minWaitTicks() {
		return getMinValue() * getUnit().ticksPer;
	}

    @Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addScrollInput(0, 26, (i, l) -> {
			i.titled(Lang.translateDirect("generic.duration"))
				.withShiftStep(15)
				.withRange(0, 121);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, "Value");

        builder.addScrollInput(26, 26, (i, l) -> {
			i.titled(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.condition." + getId().getPath() + ".min_duration"))
				.withShiftStep(15)
				.withRange(0, 121);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, NBT_MIN);

		builder.addSelectionScrollInput(52, 58, (i, l) -> {
			i.forOptions(TimeUnit.translatedOptions())
				.titled(Lang.translateDirect("generic.timeUnit"));
		}, "TimeUnit");

		
        ModularGuiLineBuilderAccessor accessor = (ModularGuiLineBuilderAccessor)builder;
        ResizableButton btn = new ResizableButton(accessor.crn$getX() + 110, accessor.crn$getY() - 4, 16, 16, TextUtils.empty(), 
        (b) -> {
			Util.getPlatform().openUri(Constants.HELP_PAGE_DYNAMIC_DELAYS);
        }) {
			@Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                Graphics graphics = new Graphics(poseStack);
				DynamicGuiRenderer.renderArea(graphics, x, y, width, height, AreaStyle.GRAY, isActive() ? (isFocused() || isMouseOver(mouseX, mouseY) ? ButtonState.SELECTED : ButtonState.BUTTON) : ButtonState.DISABLED);
				ModGuiIcons.HELP.render(graphics, x, y);
            }
        };
		accessor.crn$getTarget().add(Pair.of(btn, "help_btn"));
	}
}
