package de.mrjulsen.crn.data.schedule.instruction;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.mixin.ScheduleRuntimeAccessor;
import de.mrjulsen.crn.util.PenaltyResult;
import de.mrjulsen.crn.util.PenaltyResult.Category;
import de.mrjulsen.crn.util.PenaltyResult.Type;
import de.mrjulsen.mcdragonlib.util.MapCache;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class PrioritizedDestinationInstruction extends DestinationInstruction {

	public static final String NBT_FILTERS = "Filters";
	public static final String NBT_AVOID_RED_SIGNAL = "AvoidRedSignal";
	public static final String NBT_AVOID_TRAINS = "AvoidTrains";
	public static final byte MAX_ENTRIES = 20;

	public final Component txtIsEmpty = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + getId().getPath() + ".empty").withStyle(ChatFormatting.GRAY);
	public final Component txtPrimary = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + getId().getPath() + ".primary").withStyle(ChatFormatting.AQUA);
    

	@Override
	public Pair<ItemStack, Component> getSummary() {
		return Pair.of(AllBlocks.TRACK_STATION.asStack(), TextUtils.text(getLabelText()));
	}	

	@Override
	protected void readAdditional(CompoundTag tag) {
		super.readAdditional(tag);
        if (!tag.contains(NBT_AVOID_RED_SIGNAL)) tag.putBoolean(NBT_AVOID_RED_SIGNAL, true);
        if (!tag.contains(NBT_AVOID_TRAINS)) tag.putBoolean(NBT_AVOID_TRAINS, true);
	}

	@Override
	public List<Component> getTitleAs(String type) {
		List<Component> components = new LinkedList<>();
		components.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + getId().getPath() + ".summary").withStyle(ChatFormatting.GOLD));
		if (data.contains(NBT_FILTERS)) {
			ListTag list = data.getList(NBT_FILTERS, Tag.TAG_STRING);
			if (list.isEmpty()) {
				components.add(txtIsEmpty);
			}
			for (int i = 0; i < list.size(); i++) {
				String entry = list.get(i).getAsString();
				components.add(TextUtils.empty()
					.append(TextUtils.text(String.format("%02d  ", i + 1)).withStyle(ChatFormatting.WHITE))
					.append(TextUtils.text(entry).withStyle(i <= 0 ? ChatFormatting.WHITE : ChatFormatting.GRAY))
					.append(i <= 0 ? TextUtils.text("  ").append(txtPrimary) : TextUtils.empty())
				);
			}
		} else {
			components.add(txtIsEmpty);
		}
		return components;
	}

	@Override
	protected String getLabelText() {
		if (data.contains(NBT_FILTERS)) {
			return data.getList(NBT_FILTERS, Tag.TAG_STRING).stream().findFirst().map(Tag::getAsString).orElse("");
		}
		return "";
	}

	@Override
	public boolean supportsConditions() {
		return true;
	}

	@Override
	public ResourceLocation getId() {
		return new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "prioritized_destination_instruction");
	}

	@Override
	public ItemStack getSecondLineIcon() {
		return AllBlocks.TRACK_STATION.asStack();
	}


	public boolean shouldAvoidRedSignals() {
		return data.getBoolean(NBT_AVOID_RED_SIGNAL);
	}

	public boolean shouldAvoidTrains() {
		return data.getBoolean(NBT_AVOID_TRAINS);
	}

	public List<String> getFilters() {
		if (data.contains(NBT_FILTERS)) {
			return data.getList(NBT_FILTERS, Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
		}
		return List.of();
	}

	@Override
	public String getFilter() {
		if (data.contains(NBT_FILTERS)) {
			return data.getList(NBT_FILTERS, Tag.TAG_STRING).stream().map(Tag::getAsString).findFirst().orElse("");
		}
		return "";
	}
	
	@Override
	protected void modifyEditBox(EditBox box) {
	}

    @Override
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {   
        ClientWrapper.initPrioritizedDestinationInstruction(this, builder);
	}

	@Override
	public @Nullable DiscoveredPath start(ScheduleRuntime runtime, Level level) {
		ScheduleRuntimeAccessor accessor = (ScheduleRuntimeAccessor) runtime;
		Train train = accessor.crn$getTrain();
		List<String> filters = getFilters();
		List<Pattern> patterns = filters.stream()
				.map(Pattern::compile)
				.collect(Collectors.toList());
		INavigationExtension ext = (INavigationExtension)train.navigation;

		DiscoveredPath selectedDestination = null;
		int selectedPainCount = Integer.MAX_VALUE;
		boolean anyMatch = false;


		MapCache<DiscoveredPath, GlobalStation, GlobalStation> navigationCache = new MapCache<>((station) -> {
			return train.navigation.findPathTo(station, Double.MAX_VALUE);
		}, GlobalStation::hashCode);

		if (!train.hasForwardConductor() && !train.hasBackwardConductor()) {
			train.status.missingConductor();
			accessor.crn$setCooldown(accessor.crn$getInterval());
			return null;
		}

		for (Pattern regex : patterns) {
			AtomicInteger painCount = new AtomicInteger(0);
			GlobalStation bestStation = null;
			DiscoveredPath bestPath = null;
			double bestCost = Double.MAX_VALUE;

			for (GlobalStation globalStation : train.graph.getPoints(EdgePointType.STATION)) {
				if (!regex.matcher(globalStation.name).matches()) {
					continue;
				}
				DiscoveredPath discoveredPath = navigationCache.get(globalStation, globalStation);

				if (discoveredPath == null) {
					continue;
				}

				if (discoveredPath.cost < 0)
					continue;
				if (discoveredPath.cost > bestCost)
					continue;
				bestStation = globalStation;
				bestPath = discoveredPath;
				bestCost = discoveredPath.cost;
			}

			if (bestStation == null) {
				continue;
			}
			anyMatch = true;			

			if (shouldAvoidTrains() && (
				(bestStation.getImminentTrain() != null && bestStation.getImminentTrain() != train) ||
				(bestStation.getPresentTrain() != null && bestStation.getPresentTrain() != train) ||
				(bestStation.getNearestTrain() != null && bestStation.getNearestTrain() != train)
			)) {
				painCount.addAndGet(1);
			}

			ext.getPenaltiesByDirection().ifPresent(x -> {
				for (PenaltyResult.Type type : x.getPenalties().keySet()) {
					if (shouldAvoidRedSignals() && type == Type.REDSTONE_RED_SIGNAL) {
						painCount.addAndGet(1);
					} else if (shouldAvoidTrains() && (type.getCategory() == Category.TRAINS || type == Type.RED_SIGNAL)) {
						painCount.addAndGet(1);
					}
				}
			});

			if (painCount.get() < selectedPainCount) {
				selectedPainCount = painCount.get();
				selectedDestination = bestPath;

				if (painCount.get() <= 0)
					break;
			}
		}

		if (selectedDestination == null) {
			if (anyMatch) {
				train.status.failedNavigation();
			} else {
				train.status.failedNavigationNoTarget(String.join(", ", filters));
			}
			accessor.crn$setCooldown(accessor.crn$getInterval());
			return null;
		}

		return selectedDestination;
	}
}
