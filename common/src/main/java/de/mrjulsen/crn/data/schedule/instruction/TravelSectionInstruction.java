package de.mrjulsen.crn.data.schedule.instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.network.packets.pain.GetTrainCategoryPacketData;
import de.mrjulsen.crn.network.packets.pain.GetTrainLinePacketData;
import de.mrjulsen.crn.data.train.ScheduleSection;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TravelSectionInstruction extends ScheduleInstruction implements IPredictableInstruction {
    
    @Deprecated
    public static final String LEGACY_NBT_TRAIN_CATEGORY = "TrainGroup";

    public static final String NBT_TRAIN_CATEGORY = "TrainCategory";
    public static final String NBT_TRAIN_LINE = "TrainLine";
    public static final String NBT_INCLUDE_PREVIOUS_STATION = "IncludePreviousStation";
    public static final String NBT_USABLE = "Usable";

    private final MutableComponent txtNone = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".section_settings.none").withStyle(ChatFormatting.GRAY);
    private final MutableComponent txtLoading = TextUtils.empty().append(Constants.TEXT_LOADING).withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC);

    private UUID lastCategoryId = null;
    private UUID lastLineId = null;
    private TrainCategory category;
    private TrainLine line;

    public TravelSectionInstruction() {
    }

    @Override
    protected void readAdditional(CompoundTag tag) {
        super.readAdditional(tag);        
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
    public DiscoveredPath start(ScheduleRuntime runtime, Level level) {
        TrainListener.getTrainData(runtime.train.id).ifPresent(x -> {
            x.addScheduleSection(getSectionData(x, runtime.currentEntry));
            x.changeCurrentSection(runtime.currentEntry);
        });
        runtime.state = ScheduleRuntime.State.PRE_TRANSIT;
        runtime.currentEntry++;
        return null;
    }

    private void requestCategory(UUID categoryId) {
        this.lastCategoryId = null;
        this.category = null;
        if (categoryId == null) return;
        
        ModNetworkManager.GET_TRAIN_CATEGORY.send(NetworkDirection.toServer(), new GetTrainCategoryPacketData.Request(categoryId), (response) -> {
            this.lastCategoryId = categoryId;
            this.category = response.getCategory().orElse(null);
        }, () -> {});
    }

    private void requestLine(UUID lineId) {
        this.lastLineId = null;
        this.line = null;
        if (lineId == null) return;
        
        ModNetworkManager.GET_TRAIN_LINE.send(NetworkDirection.toServer(), new GetTrainLinePacketData.Request(lineId), (response) -> {
            this.lastLineId = lineId;
            this.line = response.getLine().orElse(null);
        }, () -> {});
    }

    @Override
	public List<Component> getTitleAs(String type) {

        UUID categoryId = null;
        UUID lineId = null;

        if (data.contains(LEGACY_NBT_TRAIN_CATEGORY)) {
            if (data.getTagType(LEGACY_NBT_TRAIN_CATEGORY) == Tag.TAG_STRING) {
                categoryId = TrainCategory.genMD5Uuid(data.getString(LEGACY_NBT_TRAIN_CATEGORY));
            } else {
                categoryId = data.getUUID(LEGACY_NBT_TRAIN_CATEGORY);
            }
        } else if (data.contains(NBT_TRAIN_CATEGORY)) {
            categoryId = data.getUUID(NBT_TRAIN_CATEGORY);
        }
        if (data.contains(NBT_TRAIN_LINE)) {
            if (data.getTagType(NBT_TRAIN_LINE) == Tag.TAG_STRING) {
                lineId = TrainLine.genMD5Uuid(data.getString(NBT_TRAIN_LINE));
            } else {
                lineId = data.getUUID(NBT_TRAIN_LINE);
            }
        }
        if (lastCategoryId == null || categoryId == null || !lastCategoryId.equals(categoryId)) {
            requestCategory(categoryId);
        }
        if (lastLineId == null || lineId == null || !lastLineId.equals(lineId)) {
            requestLine(lineId);
        }

		List<Component> lines = new ArrayList<>();
        lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath()).withStyle(ChatFormatting.GOLD));
        lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".description").withStyle(ChatFormatting.GRAY));

        lines.add(
            TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".train_category").withStyle(ChatFormatting.DARK_AQUA)
                .append(lastCategoryId == null && category != null ? txtLoading : (category == null ? txtNone : TextUtils.text(category.getCategoryName()).withStyle(ChatFormatting.WHITE))));
        lines.add(
            TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".train_line").withStyle(ChatFormatting.DARK_AQUA)
                .append(lastLineId == null && line != null ? txtLoading : (line == null ? txtNone : TextUtils.text(line.getLineName()).withStyle(ChatFormatting.WHITE))));
        if (data.contains(NBT_INCLUDE_PREVIOUS_STATION)) lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".include_previous_station").withStyle(ChatFormatting.DARK_AQUA).append((data.getBoolean(NBT_INCLUDE_PREVIOUS_STATION) ? CommonComponents.GUI_YES : CommonComponents.GUI_NO)));
        if (data.contains(NBT_USABLE)) lines.add(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath() + ".usable").withStyle(ChatFormatting.DARK_AQUA).append((data.getBoolean(NBT_USABLE) ? CommonComponents.GUI_YES : CommonComponents.GUI_NO)));
        return lines;
	}

    /** HERE BE DRAGONS! This code is very illegal, but it works... */
	@Override
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {   
        ClientWrapper.initScheduleSectionInstruction(this, builder);
	}

    private ScheduleSection getSectionData(TrainData data, int index) {
        String categoryNbtKey = null;
        if (this.data.contains(LEGACY_NBT_TRAIN_CATEGORY))
            categoryNbtKey = LEGACY_NBT_TRAIN_CATEGORY;
        else 
            categoryNbtKey = NBT_TRAIN_CATEGORY;


        return new ScheduleSection(
            data,
            index,
            !this.data.contains(categoryNbtKey) || (this.data.getTagType(categoryNbtKey) != Tag.TAG_STRING && this.data.getTagType(categoryNbtKey) != Tag.TAG_INT_ARRAY)
                ? null
                : GlobalSettings.getInstance().getTrainCategory(
                    this.data.getTagType(categoryNbtKey) == Tag.TAG_STRING
                        ? TrainCategory.genMD5Uuid(this.data.getString(categoryNbtKey))
                        : this.data.getUUID(categoryNbtKey)
                    ).orElse(null),

            !this.data.contains(NBT_TRAIN_LINE) || (this.data.getTagType(NBT_TRAIN_LINE) != Tag.TAG_STRING && this.data.getTagType(NBT_TRAIN_LINE) != Tag.TAG_INT_ARRAY)
                ? null
                : GlobalSettings.getInstance().getTrainLine(
                    this.data.getTagType(NBT_TRAIN_LINE) == Tag.TAG_STRING
                        ? TrainCategory.genMD5Uuid(this.data.getString(NBT_TRAIN_LINE))
                        : this.data.getUUID(NBT_TRAIN_LINE)
                    ).orElse(null),
                    
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