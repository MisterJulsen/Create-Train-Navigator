package de.mrjulsen.crn.data.schedule.instruction;

import java.util.LinkedList;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.core.debug.BackendDiagnosticsRecorder;
import de.mrjulsen.crn.mixin.ScheduleRuntimeAccessor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
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
	public static final String NBT_WAIT_INSTEAD = "WaitInstead";
	public static final String NBT_DETOUR_ALLOWANCE = "DetourAllowance";

	public static final byte MAX_ENTRIES = 20;
	public static final int DEFAULT_DETOUR_ALLOWANCE = 500;
	public static final int DETOUR_CHECK_OFF = -1;

	public final Component txtIsEmpty = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + getId().getPath() + ".empty").withStyle(ChatFormatting.GRAY);
	public final Component txtPrimary = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + getId().getPath() + ".primary").withStyle(ChatFormatting.AQUA);
    

	@Override
	public Pair<ItemStack, Component> getSummary() {
		return Pair.of(AllBlocks.TRACK_STATION.asStack(), TextUtils.text(getLabelText()));
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
		return !data.contains(NBT_AVOID_RED_SIGNAL) || data.getBoolean(NBT_AVOID_RED_SIGNAL);
	}

	public boolean shouldAvoidTrains() {
		return !data.contains(NBT_AVOID_TRAINS) || data.getBoolean(NBT_AVOID_TRAINS);
	}

	public boolean shouldWaitInstead() {
		return data.getBoolean(NBT_WAIT_INSTEAD);
	}

	public int getDetourAllowance() {
		return data.contains(NBT_DETOUR_ALLOWANCE) ? data.getInt(NBT_DETOUR_ALLOWANCE) : DEFAULT_DETOUR_ALLOWANCE;
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

		if (!train.hasForwardConductor() && !train.hasBackwardConductor()) {
			train.status.missingConductor();
			accessor.crn$setCooldown(accessor.crn$getInterval());
			return null;
		}

		boolean explain = shouldExplain();
		PriorityChoice.Result result = PriorityChoice.select(train, this, explain);
		if (explain) {
			reportChoice(train, result);
		}

		if (result.hasPath()) {
			return result.path();
		}

		if (result.waiting()) {
			RailwayBackendApi.getTrackedTrain(train.id).ifPresent(x -> x.markWaitingForPlatform(blockingTrainName(result)));
		} else if (result.passed().stream().anyMatch(x -> x.reason() != PriorityChoice.Skip.NO_STATION)) {
			train.status.failedNavigation();
		} else {
			train.status.failedNavigationNoTarget(String.join(", ", getFilters()));
		}
		accessor.crn$setCooldown(accessor.crn$getInterval());
		return null;
	}

	private static String blockingTrainName(PriorityChoice.Result result) {
		return result.passed().stream()
			.map(PriorityChoice.Passed::blockedBy)
			.filter(x -> x != null && !x.isBlank())
			.findFirst()
			.orElse("");
	}

	private static boolean shouldExplain() {
		return ModCommonConfig.ADVANCED_LOGGING.get()
			|| CreateRailwaysNavigator.isDebug()
			|| BackendDiagnosticsRecorder.isActive();
	}

	private void reportChoice(Train train, PriorityChoice.Result result) {
		boolean logging = ModCommonConfig.ADVANCED_LOGGING.get() || CreateRailwaysNavigator.isDebug();
		List<String> filters = getFilters();
		List<String> passed = result.passed().stream()
			.map(x -> String.format("%d (%s): %s", x.index() + 1, x.filter(), x.reason()))
			.toList();
		String chosen = result.hasPath() ? result.path().destination.name : "";

		BackendDiagnosticsRecorder.recordRouteChoice(train.id, train.name.getString(), chosen, result.index(), result.waiting(), passed, result.notes());

		if (logging) {
			CreateRailwaysNavigator.LOGGER.info("[Route] '{}' -> {} | passed over: {} | {}",
				train.name.getString(),
				result.hasPath()
					? (result.index() < 0 ? "fallback " + chosen : "priority " + (result.index() + 1) + " " + chosen)
					: result.waiting() ? "waiting, nothing free of " + filters.size() + " entries" : "no route",
				passed.isEmpty() ? "-" : String.join("; ", passed),
				String.join(" | ", result.notes()));
		}
	}
}
