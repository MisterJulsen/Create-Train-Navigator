package de.mrjulsen.crn.data.schedule.condition;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.IPredictableWaitCondition;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.core.history.DepartureLog;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.ETimeSource;
import de.mrjulsen.crn.data.schedule.INavigationExtension;
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
		long lastDepartureTimestamp = DepartureLog.NEVER;
		ScheduleEntry entry = context.scheduleEntry();
		Train train = context.train();
		String customStationFilter = getCustomStationFilter();
		if (customStationFilter != null && !customStationFilter.isBlank()) {
			lastDepartureTimestamp = lastDepartureAt(customStationFilter, train);
		} else if (entry.instruction instanceof PrioritizedDestinationInstruction instruction) {
			lastDepartureTimestamp = instruction.getFilters().stream().mapToLong(x -> lastDepartureAt(x, train)).max().orElse(DepartureLog.NEVER);
		} else if (entry.instruction instanceof DestinationInstruction instruction) {
			lastDepartureTimestamp = lastDepartureAt(instruction.getFilter(), train);
		}

		Long now = ModCommonEvents.getCurrentServer().map(server -> server.overworld().getGameTime()).orElse(null);
		return now != null && lastDepartureTimestamp + delayValue < now;
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

	@Override
	public long waitUntil(long worldTime) {
		return worldTime + totalWaitTicks();
	}

    public static DLTime toTime(long ticks) {
		return new DLTime(ticks, DLTime.defaultTimeSystem());
    }

}
