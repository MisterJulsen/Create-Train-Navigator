package de.mrjulsen.crn.data.schedule.condition;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.IPredictableWaitCondition;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.ETimeSource;
import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.data.train.DepartureHistory;
import de.mrjulsen.crn.data.train.DepartureHistory.ETrainFilter;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
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
    public static final String NBT_STATION_FILTER = "StationFilter";

    public TrainSeparationCondition() {
        super();
		data.putByte(NBT_TRAIN_FILTER, ETrainFilter.ANY.getIndex());
		data.putInt(NBT_TICKS, 100);
		data.putByte(NBT_TIME_SOURCE, ETimeSource.REAL_LIFE.getIndex());
		data.putString(NBT_STATION_FILTER, "");
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

	private String getCustomStationFilter() {
		if (data.contains(NBT_STATION_FILTER)) {
			return data.getString(NBT_STATION_FILTER);
		}
		return "";
	}

	@Override
	protected Component formatTime(boolean compact) {
        int remainingTicks = getSeparationTime();

		switch (getTimeSource()) {
			case IN_GAME -> {
				return TextUtils.text(toTime(remainingTicks).format(compact ? Constants.DEFAULT_GAME_DURATION_FORMAT : Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME, DLTime.defaultTimeSystem()));
			}
			default -> {
				return TextUtils.text(toTime(remainingTicks).format(compact ? Constants.DEFAULT_REAL_DURATION_FORMAT : Constants.DEFAULT_VERBOSE_REAL_DURATION_FORMAT, TimeContext.REAL, DLTime.defaultTimeSystem()));
			}
		}
	}

    @Override
	public List<Component> getTitleAs(String type) {
		List<Component> components = new ArrayList<>();
		components.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath()));
		components.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".description",
				formatTime(false),
				getTimeSource().getValueTranslation().getString()
		).withStyle(ChatFormatting.DARK_AQUA));
		components.add(getTrainFilter().getValueTranslation().withStyle(ChatFormatting.AQUA));

		String customStationFilter = getCustomStationFilter();
		if (customStationFilter != null && !customStationFilter.isBlank()) {
			components.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".custom_filter").withStyle(ChatFormatting.DARK_AQUA));
			components.add(TextUtils.text(customStationFilter).withStyle(ChatFormatting.AQUA));
		}
		return components;
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
		String customStationFilter = getCustomStationFilter();
		if (customStationFilter != null && !customStationFilter.isBlank()) {
			lastDepartureTimestamp = DepartureHistory.getLatestDepartureFor(getTrainFilter(), context.train(), customStationFilter);
		} else if (entry.instruction instanceof PrioritizedDestinationInstruction instruction) {
			List<String> stationName = instruction.getFilters();
			lastDepartureTimestamp = stationName.stream().mapToLong(x -> DepartureHistory.getLatestDepartureFor(getTrainFilter(), context.train(), x)).max().orElse(0);
		} else if (entry.instruction instanceof DestinationInstruction instruction) {
			String stationName = instruction.getFilter();
			lastDepartureTimestamp = DepartureHistory.getLatestDepartureFor(getTrainFilter(), context.train(), stationName);
		}

		if (GameInstance.getServer() != null && lastDepartureTimestamp + delayValue < GameInstance.getServer().overworld().getGameTime()) {
			if (context.station() != null && context.station().name != null && context.train() != null) { // TODO what's going on here? Why can station().name be null???
				DepartureHistory.updateDepartures(context.station().name, context.train());
			}
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

    public static DLTime toTime(long ticks) {
		return new DLTime(ticks, DLTime.defaultTimeSystem());
    }

}
