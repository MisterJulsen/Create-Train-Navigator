package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;
import de.mrjulsen.mcdragonlib.util.DLColor;

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
}
