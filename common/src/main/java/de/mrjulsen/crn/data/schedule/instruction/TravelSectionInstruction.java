package de.mrjulsen.crn.data.schedule.instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Pair;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.ScheduleSection;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class TravelSectionInstruction extends ScheduleInstruction implements IStationTagInstruction, IPredictableInstruction {
    
    public static final String NBT_TRAIN_GROUP = "TrainGroup";
    public static final String NBT_TRAIN_LINE = "TrainLine";
    public static final String NBT_INCLUDE_PREVIOUS_STATION = "IncludePreviousStation";
    public static final String NBT_USABLE = "Usable";

    private final MutableComponent txtNone = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".section_settings.none").withStyle(ChatFormatting.GRAY);
    private final MutableComponent txtLoading = TextUtils.empty().append(Constants.TEXT_LOADING).withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC);

    private UUID lastGroupId = null;
    private UUID lastLineId = null;
    private TrainGroup group;
    private TrainLine line;

    public TravelSectionInstruction() {
    }

    @Override
    protected void readAdditional(CompoundTag tag) {
        super.readAdditional(tag);        
        //if (!tag.contains(NBT_TRAIN_GROUP)) tag.putUUID(NBT_TRAIN_GROUP, "");
        //if (!tag.contains(NBT_TRAIN_LINE)) tag.putString(NBT_TRAIN_LINE, "");
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

    private void requestGroup(UUID groupId) {
        this.lastGroupId = null;
        this.group = null;
        if (groupId == null) return;
        DataAccessor.getFromServer(groupId, ModAccessorTypes.GET_TRAIN_GROUP, group -> {
            this.lastGroupId = groupId;
            this.group = group.orElse(null);
        });
    }

    private void requestLine(UUID lineId) {
        this.lastLineId = null;
        this.line = null;
        if (lineId == null) return;
        DataAccessor.getFromServer(lineId, ModAccessorTypes.GET_TRAIN_LINE, line -> {
            this.lastLineId = lineId;
            this.line = line.orElse(null);            
        });
    }

    @Override
	public List<Component> getTitleAs(String type) {

        UUID groupId = null;
        UUID lineId = null;

        if (data.contains(NBT_TRAIN_GROUP)) {
            if (data.getTagType(NBT_TRAIN_GROUP) == Tag.TAG_STRING) {
                groupId = TrainGroup.genMD5Uuid(data.getString(NBT_TRAIN_GROUP));
            } else {
                groupId = data.getUUID(NBT_TRAIN_GROUP);
            }
        }
        if (data.contains(NBT_TRAIN_LINE)) {
            if (data.getTagType(NBT_TRAIN_LINE) == Tag.TAG_STRING) {
                lineId = TrainLine.genMD5Uuid(data.getString(NBT_TRAIN_LINE));
            } else {
                lineId = data.getUUID(NBT_TRAIN_LINE);
            }
        }
        if (lastGroupId == null || groupId == null || !lastGroupId.equals(groupId)) {
            requestGroup(groupId);
        }
        if (lastLineId == null || lineId == null || !lastLineId.equals(lineId)) {
            requestLine(lineId);
        }

		List<Component> lines = new ArrayList<>();
        lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath()).withStyle(ChatFormatting.GOLD));
        lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".description").withStyle(ChatFormatting.GRAY));

        lines.add(
            TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".train_group").withStyle(ChatFormatting.DARK_AQUA)
                .append(lastGroupId == null && group != null ? txtLoading : (group == null ? txtNone : TextUtils.text(group.getGroupName()).withStyle(ChatFormatting.WHITE))));
        lines.add(
            TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".train_line").withStyle(ChatFormatting.DARK_AQUA)
                .append(lastLineId == null && line != null ? txtLoading : (line == null ? txtNone : TextUtils.text(line.getLineName()).withStyle(ChatFormatting.WHITE))));
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
            x.addScheduleSection(getSectionData(x, index));
            x.changeCurrentSection(index);
        });
    }

    private ScheduleSection getSectionData(TrainData data, int index) {
        return new ScheduleSection(
            data,
            index,
            GlobalSettings.getInstance().getTrainGroup(this.data.getTagType(NBT_TRAIN_GROUP) == Tag.TAG_STRING ? TrainGroup.genMD5Uuid(this.data.getString(NBT_TRAIN_GROUP)) : this.data.getUUID(NBT_TRAIN_GROUP)).orElse(null),
            GlobalSettings.getInstance().getTrainLine(this.data.getTagType(NBT_TRAIN_LINE) == Tag.TAG_STRING ? TrainGroup.genMD5Uuid(this.data.getString(NBT_TRAIN_LINE)) : this.data.getUUID(NBT_TRAIN_LINE)).orElse(null),
            this.data.getBoolean(NBT_INCLUDE_PREVIOUS_STATION),
            this.data.getBoolean(NBT_USABLE)
        );
    }

    @Override
    public void predict(TrainData data, ScheduleRuntime runtime, int indexInSchedule, Train train) {
        DLUtils.doIfNotNull(data, x -> {            
            x.addScheduleSection(getSectionData(x, indexInSchedule));
        });
    }
}