package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;

/**
 * For data conversion: Indicates that this class adopts the original
 * property {@code timeDisplay} from the Advanced Displays.
 * If the class should adopt this property, this interface must be
 * implemented or the value will not be converted!
 */
public interface ITextPosSetting {

    public static final String GUI_LINE_TEXT_POS_NAME = "text_pos";

    public static final float MAX_X = AdvancedDisplayBlockEntity.MAX_XSIZE * 16 - 6;
    public static final float MAX_Y = AdvancedDisplayBlockEntity.MAX_YSIZE * 16 - 6;

    public static final float DEFAULT_X = 0;
    public static final float DEFAULT_Y = 2.5f;
    public static final ETextAlignment DEFAULT_TEXT_ALIGNMENT = ETextAlignment.CENTER;
    
    public static final String NBT_POS_X = "PosX";
    public static final String NBT_POS_Y = "PosY";
    public static final String NBT_TEXT_ALIGNMENT = "TextAlignment";
    
    float getX();
    void setX(float x);
    float getY();
    void setY(float y);
    ETextAlignment getTextAlignment();
    void setTextAlignment(ETextAlignment align);

    default void buildTextPosGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildTextPosGui(this, context);
    }

    
    default void copyTextPosSettings(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ITextPosSetting o) {
            setX(o.getX());
            setY(o.getY());
            setTextAlignment(o.getTextAlignment());
        }
    }
}
