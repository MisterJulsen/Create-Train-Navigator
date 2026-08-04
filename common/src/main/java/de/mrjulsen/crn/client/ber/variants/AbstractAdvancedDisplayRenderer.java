package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.core.snapshot.BoardEntry;
import de.mrjulsen.crn.api.core.CallDirection;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.block.display.properties.components.ITimeDisplaySetting;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface AbstractAdvancedDisplayRenderer<T extends IDisplaySettings> extends IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    public static final DLColor DARK_FONT_COLOR = DLColor.fromInt(0xFF111111);
    public static final DLColor LIGHT_FONT_COLOR = DLColor.fromInt(0xFFEEEEEE);
    public static final float SCROLLING_SPEED = 12f;

    @SuppressWarnings("unchecked")
    default T getDisplaySettings(AdvancedDisplayBlockEntity blockEntity) {
        try {
            return (T)blockEntity.getSettings();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Could not get display data of display at " + blockEntity.getBlockPos(), e);
        }
    }

    default List<Component> announcements(AdvancedDisplayBlockEntity blockEntity, BoardEntry entry, CallDirection direction) {
        List<Component> content = new ArrayList<>();
        if (entry.isCancelled()) {
            content.add(CustomLanguage.translate(key("cancelled")));
            return content;
        }

        if (entry.terminus()) {
            content.add(CustomLanguage.translate(key("train_terminates")));
        }

        if (entry.isDelayed(direction)) {
            boolean eta = blockEntity.getSettingsAs(ITimeDisplaySetting.class)
                .map(x -> x.getTimeDisplay() == ETimeDisplay.ETA).orElse(false);
            long deviation = entry.deviation(direction);
            String delay = eta
                ? ModUtils.timeRemainingString(deviation)
                : String.valueOf((long)DLTime.fromGameTicks(deviation, DLTime.defaultTimeSystem()).toGameMinutes(DLTime.defaultTimeSystem()));
            MutableComponent delayComponent = CustomLanguage.translate(key("delayed"), delay);
            if (!eta) {
                delayComponent.append(" ").append(CustomLanguage.translate(key("delay_abs_suffix")));
            }
            content.add(delayComponent);
        }

        if (entry.isDiverted() && !blockEntity.isAllowedOnDisplay(entry.station())) {
            content.add(entry.hasChangedTag()
                ? CustomLanguage.translate(key("platform_and_station_changed"), entry.station().displayName(), entry.station().platform())
                : CustomLanguage.translate(key("platform_changed"), entry.station().platform()));
        }

        for (DelayInstance cause : entry.delays()) {
            content.add(CustomLanguage.translate(cause.translationKey()));
        }
        return content;
    }

    private static String key(String name) {
        return "block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber." + name;
    }
}
