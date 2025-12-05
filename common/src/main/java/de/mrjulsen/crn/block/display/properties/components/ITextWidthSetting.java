package de.mrjulsen.crn.block.display.properties.components;

import java.util.Arrays;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

/**
 * For data conversion: Indicates that this class adopts the original
 * property {@code timeDisplay} from the Advanced Displays.
 * If the class should adopt this property, this interface must be
 * implemented or the value will not be converted!
 */
public interface ITextWidthSetting {

    public static final int USED_LINE_SPACE = 18 + 4;
    public static final String GUI_LINE_TEXT_MAX_WIDTH_NAME = "text_max_width";

    public static final float MIN_VALUE = 0;
    public static final float MAX_VALUE = AdvancedDisplayBlockEntity.MAX_XSIZE * 16 - 6;
    public static final float DEFAULT_TEXT_MAX_WIDTH = MAX_VALUE;
    public static final TextScaleBounds DEFAULT_BOUNDS_ACTION = TextScaleBounds.SCALE_SCROLL;

    public static final String NBT_TEXT_MAX_WIDTH = "TextMaxWidth";
    public static final String NBT_BOUNDS_ACTION = "BoundsAction";
    
    float getTextMaxWidth();
    void setTextMaxWidth(float s);
    TextScaleBounds getBoundsAction();
    void setBoundsAction(TextScaleBounds action);

    default void buildTextMaxWidthGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildTextMaxWidthGui(this, context);
    }
    
    default void copyTextMaxWidthSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ITextWidthSetting o) {
            setTextMaxWidth(o.getTextMaxWidth());
            setBoundsAction(o.getBoundsAction());

        }
    }

    default boolean isMaxTextWidth() {
        return getTextMaxWidth() >= MAX_VALUE;
    }


    public static enum TextScaleBounds implements ITranslatableEnum {
        CUT_OFF((byte)0, "cut_off", EScrollMode.NEVER),
        SCALE_SCROLL((byte)1, "scale_scroll", EScrollMode.WHEN_NEEDED),
        SCROLL((byte)2, "scroll", EScrollMode.ALWAYS);

        final byte index;
        final String name;
        final EScrollMode mode;

        private TextScaleBounds(byte index, String name,EScrollMode mode) {
            this.index = index;
            this.name = name;
            this.mode = mode;
        }

        public byte getIndex() {
            return index;
        }

        public EScrollMode getMode() {
            return mode;
        }

        public static TextScaleBounds getByIndex(int b) {
            return Arrays.stream(values()).filter(x -> x.getIndex() == b).findFirst().orElse(SCALE_SCROLL);
        }
        
        @Override
        public Data getTranslationData() {
            return new Data(CreateRailwaysNavigator.MOD_ID, "text_scale_bounds", name);
        }
    }
}
