package de.mrjulsen.crn.data.schedule.instruction;

import java.util.LinkedList;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Pair;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class PrioritizedDestinationInstruction extends DestinationInstruction {

	public static final String NBT_FILTERS = "Filters";
	public static final String NBT_AVOID_RED_SIGNAL = "AvoidRedSignal";
	public static final String NBT_AVOID_TRAINS = "AvoidTrains";
	public static final byte MAX_ENTRIES = 20;

	public final Component txtIsEmpty = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + getId().getPath() + ".empty").withStyle(ChatFormatting.GRAY);
	public final Component txtPrimary = TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + getId().getPath() + ".primary").withStyle(ChatFormatting.AQUA);
    

	@Override
	public Pair<ItemStack, Component> getSummary() {
		return Pair.of(AllBlocks.TRACK_STATION.asStack(), Components.literal(getLabelText()));
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
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {   
        ClientWrapper.initPrioritizedDestinationInstruction(this, builder);
	}
}
