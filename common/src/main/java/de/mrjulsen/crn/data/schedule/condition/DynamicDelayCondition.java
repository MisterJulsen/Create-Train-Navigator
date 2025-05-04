package de.mrjulsen.crn.data.schedule.condition;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import com.simibubi.create.foundation.utility.CreateLang;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.IPredictableWaitCondition;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class DynamicDelayCondition extends ScheduledDelay implements IPredictableWaitCondition {

    public static final String NBT_MIN = "Min";
    
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
			return TextUtils.text(time + getUnit().suffix);
		return TextUtils.text(time + " ").append(CreateLang.translateDirect(getUnit().key));
	}

    @Override
	public List<Component> getTitleAs(String type) {
		return ImmutableList.of(
			TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath()),
				CreateLang.translateDirect("schedule.condition.for_x_time", formatTime(false)).withStyle(ChatFormatting.DARK_AQUA),
			TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".at_least", formatCustomTime(getMinValue(), false)).withStyle(ChatFormatting.DARK_AQUA)
        );
	}

	@Override
	public boolean tickCompletion(Level level, Train train, CompoundTag context) {
		int time = context.getInt("Time");

		AtomicLong currentDelay = new AtomicLong(0);
		AtomicLong scheduledDepartureTime = new AtomicLong(0);
		AtomicBoolean initialized = new AtomicBoolean(false);

		TrainListener.getTrainData(train.id).ifPresent(data -> {
			Optional<TrainPrediction> pred = data.getNextStopPrediction();
			if (pred.isPresent()) {
				currentDelay.set(pred.get().getArrivalTimeDeviation());
				initialized.set(data.isInitialized() && !data.isPreInitializationPhase());
				scheduledDepartureTime.set(pred.get().scheduled().departureTime());
			}
		});

		long totalTicks = initialized.get() ? Math.max(totalWaitTicks() - currentDelay.get(), minWaitTicks()) : totalWaitTicks();

		if (time >= (initialized.get() ? Math.max(totalWaitTicks() - currentDelay.get(), minWaitTicks()) : totalWaitTicks()) && (!initialized.get() || DragonLib.getCurrentWorldTime() >= scheduledDepartureTime.get()))
			return true;
		
		context.putInt("Time", time + 1);
		context.putLong("TotalTicks", Math.max(totalTicks, scheduledDepartureTime.get() - DragonLib.getCurrentWorldTime() + time));
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
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		ClientWrapper.initDynamicDelayCondition(this, builder);
	}

	@Override
	public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag tag) {
		int time = tag.getInt("Time");
		long totalTime = tag.getInt("TotalTicks");
		long ticksUntilDeparture = Math.max(totalTime - time, 0);
		boolean showInMinutes = ticksUntilDeparture >= 20 * 60;
		int num = (int) (showInMinutes ? Math.floor(ticksUntilDeparture / (20 * 60f)) : Math.ceil(ticksUntilDeparture / 100f) * 5);
		String key = "generic." + (showInMinutes ? num == 1 ? "daytime.minute" : "unit.minutes"
			: num == 1 ? "daytime.second" : "unit.seconds");
			
		return CreateLang.translateDirect("schedule.condition." + getId().getPath() + ".status", TextUtils.text(num + " ").append(CreateLang.translateDirect(key)));
	}

	@Override
	public long waitUntil(long worldTime) {
		return worldTime + totalWaitTicks();
	}

	@Override
	public long waitMinUntil(long worldTime) {
		return worldTime + minWaitTicks();
	}
}
