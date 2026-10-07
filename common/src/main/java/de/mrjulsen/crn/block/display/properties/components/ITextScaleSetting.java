package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;

public interface ITextScaleSetting {

    public static final String GUI_LINE_TEXT_SIZE_NAME = "text_scale";
    public static final int USED_LINE_SPACE = 22 + 4;

    public static final float DEFAULT_SCALE = 0.75f;

    public static final String NBT_X_SCALE = "XScale";
    public static final String NBT_Y_SCALE = "YScale";
    public static final String NBT_X_MIN_SCALE = "MinXScale";

    float getXScale();
    void setXScale(float f);
    float getYScale();
    void setYScale(float f);
    float getMinXScale();
    void setMinXScale(float f);

    default void buildTextScaleGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildTextScaleGui(this, context);
    }


    default void copyTextScaleSettings(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ITextScaleSetting o) {
            setXScale(o.getXScale());
            setYScale(o.getYScale());
            setMinXScale(o.getMinXScale());
        }
    }
}
