package de.mrjulsen.crn.data.schedule.instruction;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Pair;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainTravelSection;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class TravelSectionInstruction extends ScheduleInstruction implements IStationTagInstruction, IPredictableInstruction {
    
    public static final String NBT_TRAIN_GROUP = "TrainGroup";
    public static final String NBT_TRAIN_LINE = "TrainLine";
    public static final String NBT_INCLUDE_PREVIOUS_STATION = "IncludePreviousStation";
    public static final String NBT_USABLE = "Usable";

    public TravelSectionInstruction() {
    }

    @Override
    protected void readAdditional(CompoundTag tag) {
        super.readAdditional(tag);        
        if (!tag.contains(NBT_TRAIN_GROUP)) tag.putString(NBT_TRAIN_GROUP, "");
        if (!tag.contains(NBT_TRAIN_LINE)) tag.putString(NBT_TRAIN_LINE, "");
        if (!tag.contains(NBT_INCLUDE_PREVIOUS_STATION)) tag.putBoolean(NBT_INCLUDE_PREVIOUS_STATION, false);
        if (!tag.contains(NBT_USABLE)) tag.putBoolean(NBT_USABLE, true);
    }

    @Override
    public Pair<ItemStack, Component> getSummary() {
        return Pair.of(new ItemStack(ModBlocks.ADVANCED_DISPLAY.get()), TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + getId().getPath()).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "travel_section");
    }

    @Override
    public boolean supportsConditions() {
        return false;
    }

    @Override
	public List<Component> getTitleAs(String type) {
        String noneText = TextUtils.translate("gui.createrailwaysnavigator.section_settings.none").getString();
		List<Component> lines = new ArrayList<>();
        lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath()).withStyle(ChatFormatting.GOLD));
        lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".description").withStyle(ChatFormatting.GRAY));
        if (data.contains(NBT_TRAIN_GROUP)) lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".train_group").withStyle(ChatFormatting.DARK_AQUA).append(TextUtils.text(data.getString(NBT_TRAIN_GROUP).isBlank() ? noneText : data.getString(NBT_TRAIN_GROUP).toString()).withStyle(ChatFormatting.WHITE)));
        if (data.contains(NBT_TRAIN_LINE)) lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".train_line").withStyle(ChatFormatting.DARK_AQUA).append(TextUtils.text(data.getString(NBT_TRAIN_LINE).isBlank() ? noneText : data.getString(NBT_TRAIN_LINE).toString()).withStyle(ChatFormatting.WHITE)));
        if (data.contains(NBT_INCLUDE_PREVIOUS_STATION)) lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".include_previous_station").withStyle(ChatFormatting.DARK_AQUA).append((data.getBoolean(NBT_INCLUDE_PREVIOUS_STATION) ? CommonComponents.GUI_YES : CommonComponents.GUI_NO)));
        if (data.contains(NBT_USABLE)) lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".usable").withStyle(ChatFormatting.DARK_AQUA).append((data.getBoolean(NBT_USABLE) ? CommonComponents.GUI_YES : CommonComponents.GUI_NO)));
        return lines;
	}

    /** HERE BE DRAGONS! This code is very illegal, but it works... */
	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {   
        ClientWrapper.initScheduleSectionInstruction(this, builder);
	}

    @Override
    public void run(ScheduleRuntime runtime, TrainData data, Train train, int index) {
        DLUtils.doIfNotNull(data, x -> {
            x.addTravelSection(getSectionData(x, index));
            x.changeCurrentSection(index);
        });
    }

    private TrainTravelSection getSectionData(TrainData data, int index) {
        return new TrainTravelSection(
            data,
            index,
            GlobalSettings.getInstance().getTrainGroup(this.data.getString(NBT_TRAIN_GROUP)).orElse(null),
            GlobalSettings.getInstance().getTrainLine(this.data.getString(NBT_TRAIN_LINE)).orElse(null),
            this.data.getBoolean(NBT_INCLUDE_PREVIOUS_STATION),
            this.data.getBoolean(NBT_USABLE)
        );
    }

    @Override
    public void predict(TrainData data, ScheduleRuntime runtime, int indexInSchedule, Train train) {
        DLUtils.doIfNotNull(data, x -> {            
            x.addTravelSection(getSectionData(x, indexInSchedule));
        });
    }
}