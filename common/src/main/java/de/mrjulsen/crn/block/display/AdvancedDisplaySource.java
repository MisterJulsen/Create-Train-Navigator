package de.mrjulsen.crn.block.display;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Lang;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

public class AdvancedDisplaySource extends DisplaySource {

    public static final String NBT_ADVANCED_DISPLAY = "AdvancedDisplay";
    public static final String NBT_FILTER = "Filter";
    public static final String NBT_TRAIN_NAME_WIDTH = "TrainNameWidth";
    public static final String NBT_PLATFORM_WIDTH = "PlatformWidth";
    public static final String NBT_TIME_DISPLAY_TYPE = "TimeDisplay";

	@Override
	public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
		return EMPTY;
	}

	@Override
	public List<List<MutableComponent>> provideFlapDisplayText(DisplayLinkContext context, DisplayTargetStats stats) {
		context.sourceConfig().putBoolean(NBT_ADVANCED_DISPLAY, true);
        return List.of();
	}

	@Override
	protected String getTranslationKey() {
		return "advanced_display";
	}

	@Override
	public void populateData(DisplayLinkContext context) {
		CompoundTag conf = context.sourceConfig();

		if (!conf.contains(NBT_TRAIN_NAME_WIDTH))
			conf.putInt(NBT_TRAIN_NAME_WIDTH, 16);
		if (!conf.contains(NBT_PLATFORM_WIDTH))
			conf.putInt(NBT_PLATFORM_WIDTH, -1);
		if (!conf.contains(NBT_TIME_DISPLAY_TYPE))
			conf.putInt(NBT_TIME_DISPLAY_TYPE, ETimeDisplay.ABS.getId());

		if (conf.contains(NBT_FILTER))
			return;

		GlobalStation station = CRNPlatformSpecific.getStationFromBlockEntity(context.getSourceBlockEntity());
		if (station == null) {
			return;
		}

		conf.putString(NBT_FILTER, station.name);
	}

	@Override
	public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
		if (isFirstLine) {
			builder.addTextInput(0, 137, (e, t) -> {
				e.setValue("");
				t.withTooltip(ImmutableList.of(Lang.translateDirect("display_source.station_summary.filter")
					.withStyle(s -> s.withColor(0x5391E1)),
					Lang.translateDirect("gui.schedule.lmb_edit")
						.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
			}, NBT_FILTER);
			return;
		}

		builder.addScrollInput(0, 43, (si, l) -> {
			si.titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.train_name_width"))
				.addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.train_name_width.description"))
				.withRange(0, 65)
				.withShiftStep(4);
			si.setState(16);
			l.withSuffix("px");
		}, NBT_TRAIN_NAME_WIDTH);

		builder.addScrollInput(47, 43, (si, l) -> {
			si.titled(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.platform_width"))
				.addHint(TextUtils.translate("gui.createrailwaysnavigator.display_source.advanced_display.platform_width.description"))
				.withRange(-1, 65)
				.withShiftStep(4);				
			si.setState(16);
			si.format((val) -> {
				if (val >= 0) {
					return TextUtils.text(String.valueOf(val) + "px");
				}
				return TextUtils.translate("gui.createrailwaysnavigator.common.auto");
			});
		}, NBT_PLATFORM_WIDTH);

		builder.addSelectionScrollInput(47 * 2, 43, (si, l) -> {
			si
			.forOptions(Arrays.stream(ETimeDisplay.values()).map(x -> TextUtils.translate(x.getValueInfoTranslationKey(CreateRailwaysNavigator.MOD_ID))).toList())
			.titled(TextUtils.translate("enum.createrailwaysnavigator.time_display"))
			.addHint(TextUtils.translate("enum.createrailwaysnavigator.time_display.description"))
			.format((val) -> {
				return TextUtils.translate(ETimeDisplay.getById(val).getValueTranslationKey(CreateRailwaysNavigator.MOD_ID));
			})
			.setState(ETimeDisplay.ABS.getId());
		}, NBT_TIME_DISPLAY_TYPE);

	}

	public static enum ETimeDisplay implements StringRepresentable, ITranslatableEnum {
		ABS((byte)0, "abs"),
		ETA((byte)1, "eta");

		private byte id;
		private String name;

		private ETimeDisplay(byte id, String name) {
			this.id = id;
			this.name = name;
		}

		public byte getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public static ETimeDisplay getById(int id) {
			return Arrays.stream(values()).filter(x -> x.getId() == id).findFirst().orElse(ABS);
		}

		@Override
		public String getEnumName() {
			return "time_display";
		}

		@Override
		public String getEnumValueName() {
			return getName();
		}

		@Override
		public String getSerializedName() {
			return getName();
		}

	}

}
