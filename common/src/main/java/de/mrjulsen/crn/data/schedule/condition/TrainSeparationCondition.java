package de.mrjulsen.crn.data.schedule.condition;

import java.util.List;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.IPredictableWaitCondition;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.ETimeSource;
import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.data.train.DepartureHistory;
import de.mrjulsen.crn.data.train.DepartureHistory.ETrainFilter;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.utils.GameInstance;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class TrainSeparationCondition extends ScheduledDelay implements IDelayedWaitCondition, IPredictableWaitCondition {

    @Deprecated public static final String NBT_TIME = "Value";
    @Deprecated public static final String NBT_TIME_UNIT = "TimeUnit";
    public static final String NBT_TICKS = "Ticks";
    public static final String NBT_TRAIN_FILTER = "TrainFilter";
    public static final String NBT_TIME_SOURCE = "TimeSource";
    
    public TrainSeparationCondition() {
        super();
		data.putByte(NBT_TRAIN_FILTER, ETrainFilter.ANY.getIndex());
		data.putInt(NBT_TICKS, 100);
		data.putByte(NBT_TIME_SOURCE, ETimeSource.REAL_LIFE.getIndex());
    }

	@Override
	public Pair<ItemStack, Component> getSummary() {
		return Pair.of(ItemStack.EMPTY, TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.condition." + getId().getPath() + ".title", formatTime(true)));
	}

	@Override
	public ItemStack getSecondLineIcon() {
		return new ItemStack(Items.OBSERVER);
	}

	@Override
	public int totalWaitTicks() {
		return 0;
	}

	private int getSeparationTime() {
		if (data.contains(NBT_TICKS)) {
			return data.getInt(NBT_TICKS);
		}
		return 0;
	}

	@Override
	protected Component formatTime(boolean compact) {
        int remainingTicks = getSeparationTime();

		switch (getTimeSource()) {
			case IN_GAME -> {
				int[] t = toInGameTime(remainingTicks);
				if (compact) {
					return TextUtils.text(String.format("%d:%02d:%02d", t[2], t[1], t[0]));
				}
				return TextUtils.text(String.format("%dd %dh %dm", t[2], t[1], t[0]));
			}
			default -> {
				int[] t = toRealLifeTime(remainingTicks);
				if (compact) {
					return TextUtils.text(String.format("%d:%02d,%02d", t[2], t[1], t[0]));
				}
				return TextUtils.text(String.format("%dm %ds %dt", t[2], t[1], t[0]));
			}
		}
	}

    @Override
	public List<Component> getTitleAs(String type) {
		return ImmutableList.of(
			TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath()),
			TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".description",
				formatTime(false),
				TextUtils.translate(getTimeSource().getValueTranslationKey(CreateRailwaysNavigator.MOD_ID)).getString()
			).withStyle(ChatFormatting.DARK_AQUA),
			TextUtils.translate(getTrainFilter().getValueTranslationKey(CreateRailwaysNavigator.MOD_ID)).withStyle(ChatFormatting.AQUA)
        );
	}

	@Override
	public boolean tickCompletion(Level level, Train train, CompoundTag context) {
		ScheduleEntry entry = train.runtime.getSchedule().entries.get(train.runtime.currentEntry);
		((INavigationExtension)(Object)train.navigation).addDelayedWaitCondition(de.mrjulsen.mcdragonlib.util.Pair.of(this, new DelayedWaitConditionContext(level, train, context, train.getCurrentStation(), entry)));
		return true;
	}


	@Override
	public boolean runDelayed(DelayedWaitConditionContext context) {
		int delayValue = getSeparationTime();
		long lastDepartureTimestamp = Long.MIN_VALUE;
		ScheduleEntry entry = context.scheduleEntry();
		if (entry.instruction instanceof PrioritizedDestinationInstruction instruction) {
			List<String> stationName = instruction.getFilters();
			lastDepartureTimestamp = stationName.stream().mapToLong(x -> DepartureHistory.getLatestDepartureFor(getTrainFilter(), context.train(), x)).max().orElse(0);
		} else if (entry.instruction instanceof DestinationInstruction instruction) {
			String stationName = instruction.getFilter();
			lastDepartureTimestamp = DepartureHistory.getLatestDepartureFor(getTrainFilter(), context.train(), stationName);
		}

		if (GameInstance.getServer() != null && lastDepartureTimestamp + delayValue < GameInstance.getServer().overworld().getGameTime()) {
			DepartureHistory.updateDepartures(context.station().name, context.train());
			return true;
		}
		return false;
	}

	@Override
	public ResourceLocation getId() {
		return new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "train_separation");
	}
    
	public ETrainFilter getTrainFilter() {
		return ETrainFilter.getByIndex(data.getByte(NBT_TRAIN_FILTER));
	}

	public ETimeSource getTimeSource() {
		return ETimeSource.getByIndex(data.getByte(NBT_TIME_SOURCE));
	}

    @Override
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		ClientWrapper.initTimingAdjustmentGui(this, builder);
	}

	@Override
	public long waitUntil(long worldTime) {
		return worldTime + totalWaitTicks();
	}


	public static int[] toRealLifeTime(int ticks) {
        int t = ticks;
        int m = t / 1200;
        t %= 1200;
        int s = t / 20;
        t %= 20;
        return new int[] { t, s, m };
    }

    public static int[] toInGameTime(int ticks) {
        int t = ticks;
        int d = (int)(t / DragonLib.ticksPerDay());
        t %= DragonLib.ticksPerDay();
        int h = (int)(t / DragonLib.ticksPerIngameHour());
        t %= DragonLib.ticksPerIngameHour();
        int m = (int)(t / (DragonLib.ticksPerIngameHour() / 60));
        return new int[] { m, h, d };
    }

    public static int toTicksFromRealLife(int[] t) {
        return t[2] * TimeUnit.MINUTES.ticksPer + t[1] * TimeUnit.SECONDS.ticksPer + t[0];
    }

    public static int toTicksFromInGame(int[] t) {
        return (int)(t[2] * DragonLib.ticksPerDay() + t[1] * DragonLib.ticksPerIngameHour() + t[0] * (DragonLib.ticksPerIngameHour() / 60));
    }

}
