package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;

public interface AbstractAdvancedDisplayRenderer<T extends IDisplaySettings> extends IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    public static final int DARK_FONT_COLOR = 0xFF111111;
    public static final int LIGHT_FONT_COLOR = 0xFFEEEEEE;

    @SuppressWarnings("unchecked")
    default T getDisplaySettings(AdvancedDisplayBlockEntity blockEntity) {
        try {
            return (T)blockEntity.getSettings();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Could not get display data of display at " + blockEntity.getBlockPos(), e);
        }
    }
}
