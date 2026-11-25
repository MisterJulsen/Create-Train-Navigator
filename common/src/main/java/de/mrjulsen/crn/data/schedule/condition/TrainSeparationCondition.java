package de.mrjulsen.crn.data.schedule.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.core.TrainManager;
import de.mrjulsen.crn.core.history.DepartureLog;
import de.mrjulsen.crn.core.schedule.JourneySection;
import de.mrjulsen.crn.core.timing.StopTimings;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.ETimeSource;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.util.TrainUtils;
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
		AtomicLong l = new AtomicLong(0);
		forEachIn(entry, condition -> {
			l.set(Math.max(l.get(), condition.remainingHold(train, entry)));
			return false;
		});
		return l.get();
	}

	public static ScheduleEntry readyEntry(Train train) {
		ScheduleRuntime runtime = train.runtime;
		Schedule schedule = runtime.schedule;
		if (schedule == null || runtime.paused || train.derailed || runtime.state != ScheduleRuntime.State.POST_TRANSIT || train.navigation.destination != null || runtime.conditionProgress == null || runtime.currentEntry < 0 || runtime.currentEntry >= schedule.entries.size()) {
			return null;
		}
		ScheduleEntry entry = schedule.entries.get(runtime.currentEntry);
		if (!entry.instruction.supportsConditions() || !isPresentIn(entry)) {
			return null;
		}
		int groups = Math.min(entry.conditions.size(), runtime.conditionProgress.size());
		for (int i = 0; i < groups; i++) {
			if (runtime.conditionProgress.get(i) >= entry.conditions.get(i).size()) {
				return entry;
			}
		}
		return null;
	}

	public static boolean mustGiveWay(Train train, ScheduleEntry entry) {
		long now = ModUtils.getTransformedWorldTime();
		long own = arrivalTime(train, now);
		for (Train other : Create.RAILWAYS.trains.values()) {
			if (other == train || !isAffectedBy(train, entry, other)) {
				continue;
			}
			ScheduleEntry otherEntry = readyEntry(other);
			if (otherEntry == null || remainingHoldTicks(other, otherEntry) > 0) {
				continue;
			}
			long theirs = arrivalTime(other, now);
			if (theirs < own || (theirs == own && other.id.compareTo(train.id) < 0)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isAffectedBy(Train train, ScheduleEntry entry, Train other) {
		GlobalStation station = other.getCurrentStation();
		return station != null && forEachIn(entry, condition -> condition.getSeparationTime() > 0
				&& condition.stationFilters(entry).stream().anyMatch(filter -> TrainUtils.stationMatches(station.name, filter))
				&& condition.sharesTrainFilter(train, other));
	}

	private boolean sharesTrainFilter(Train train, Train other) {
		return switch (getTrainFilter()) {
			case SAME_NAME -> train.name.getString().equals(other.name.getString());
			case SAME_LINE -> sameSectionId(train, other, JourneySection::getTrainLineId);
			case SAME_CATEGORY -> sameSectionId(train, other, JourneySection::getTrainCategoryId);
			default -> true;
		};
	}

	private static boolean sameSectionId(Train train, Train other, Function<JourneySection, UUID> id) {
		UUID own = sectionOf(train).map(id).orElse(null);
		return own != null && own.equals(sectionOf(other).map(id).orElse(null));
	}

	private static Optional<JourneySection> sectionOf(Train train) {
		return TrainManager.getInstance().getTrain(train.id).flatMap(TrackedTrain::getCurrentSection);
	}

	private static long arrivalTime(Train train, long now) {
		return TrainManager.getInstance().getTrain(train.id)
			.flatMap(tracked -> tracked.getCurrentStop().map(tracked::getTimings))
			.map(StopTimings::getLastActualArrival)
			.filter(time -> time >= 0)
			.orElse(now);
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
		return stationFilters(entry).stream().mapToLong(x -> lastDepartureAt(x, train)).max().orElse(DepartureLog.NEVER);
	}

	private List<String> stationFilters(ScheduleEntry entry) {
		String customStationFilter = getCustomStationFilter();
		if (customStationFilter != null && !customStationFilter.isBlank()) {
			return List.of(customStationFilter);
		}
		if (entry.instruction instanceof PrioritizedDestinationInstruction instruction) {
			return instruction.getFilters();
		}
		if (entry.instruction instanceof DestinationInstruction instruction) {
			return List.of(instruction.getFilter());
		}
		return List.of();
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
