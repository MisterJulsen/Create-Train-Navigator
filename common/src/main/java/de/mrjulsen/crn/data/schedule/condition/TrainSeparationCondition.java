package de.mrjulsen.crn.data.schedule.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.condition.TimedWaitCondition.TimeUnit;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.core.history.DepartureLog;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.ETimeSource;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class TrainSeparationCondition extends ScheduledDelay {

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
		if (data.contains(NBT_TIME)) {
			TimeUnit[] units = TimeUnit.values();
			int unitIndex = data.getInt(NBT_TIME_UNIT);
			TimeUnit unit = unitIndex >= 0 && unitIndex < units.length ? units[unitIndex] : TimeUnit.TICKS;
			return data.getInt(NBT_TIME) * unit.ticksPer;
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
		return true;
	}

	/**
	 * Whether the given schedule entry carries at least one separation condition, in any of its
	 * condition groups.
	 */
	public static boolean isPresentIn(ScheduleEntry entry) {
		return forEachIn(entry, x -> true);
	}

	/**
	 * How much longer the train has to be held at the platform to satisfy every separation condition
	 * of the given entry, in ticks. Zero once none of them holds it any longer.
	 * <p>
	 * The result is bounded by the longest separation time configured on the entry, because the
	 * departure a condition measures against always lies in the past.
	 */
	public static long remainingHoldTicks(Train train, ScheduleEntry entry) {
		if (train == null) {
			return 0;
		}
		long[] remaining = { 0 };
		forEachIn(entry, condition -> {
			remaining[0] = Math.max(remaining[0], condition.remainingHold(train, entry));
			return false;
		});
		return remaining[0];
	}

	private static boolean forEachIn(ScheduleEntry entry, Predicate<TrainSeparationCondition> action) {
		if (entry == null || entry.conditions == null) {
			return false;
		}
		for (List<ScheduleWaitCondition> group : entry.conditions) {
			for (ScheduleWaitCondition condition : group) {
				if (condition instanceof TrainSeparationCondition separation && action.test(separation)) {
					return true;
				}
			}
		}
		return false;
	}

	private long remainingHold(Train train, ScheduleEntry entry) {
		int separationTime = getSeparationTime();
		if (separationTime <= 0) {
			return 0;
		}

		long lastDepartureTimestamp = lastRelevantDeparture(train, entry);
		if (lastDepartureTimestamp == DepartureLog.NEVER) {
			return 0;
		}

		Long now = ModCommonEvents.getCurrentServer().map(server -> server.overworld().getGameTime()).orElse(null);
		return now == null ? 0 : Math.max(0, lastDepartureTimestamp + separationTime - now);
	}

	private long lastRelevantDeparture(Train train, ScheduleEntry entry) {
		String customStationFilter = getCustomStationFilter();
		if (customStationFilter != null && !customStationFilter.isBlank()) {
			return lastDepartureAt(customStationFilter, train);
		}
		if (entry.instruction instanceof PrioritizedDestinationInstruction instruction) {
			return instruction.getFilters().stream().mapToLong(x -> lastDepartureAt(x, train)).max().orElse(DepartureLog.NEVER);
		}
		if (entry.instruction instanceof DestinationInstruction instruction) {
			return lastDepartureAt(instruction.getFilter(), train);
		}
		return DepartureLog.NEVER;
	}

	private long lastDepartureAt(String stationFilter, Train train) {
		return RailwayBackendApi.getLastDepartureTime(stationFilter, getTrainFilter(), train.id, train.name.getString());
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

    public static DLTime toTime(long ticks) {
		return new DLTime(ticks, DLTime.defaultTimeSystem());
    }

}
