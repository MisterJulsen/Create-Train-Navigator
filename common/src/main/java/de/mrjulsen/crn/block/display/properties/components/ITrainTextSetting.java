package de.mrjulsen.crn.block.display.properties.components;

import java.util.Arrays;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringRepresentable;

public interface ITrainTextSetting {

    public static enum ETrainTextComponents implements StringRepresentable, ITranslatableEnum {
        ALL((byte)0, "all"),
        TRAIN_NAME((byte)1, "train_name"),
        DESTINATION((byte)2, "destination");
        
        final String name;
        final byte id;
        
        ETrainTextComponents(byte id, String name) {
            this.name = name;
            this.id = id;
        }

        public byte getId() {
            return this.id;
        }	

        public static ETrainTextComponents getById(int id) {
            return Arrays.stream(values()).filter(x -> x.getId() == (byte)id).findFirst().orElse(ETrainTextComponents.ALL);
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Override
        public String getEnumName() {
            return "train_text_components";
        }

        @Override
        public String getEnumValueName() {
            return this.name;
        }

        public boolean showTrainName() {
            return this == ALL || this == TRAIN_NAME;
        }

        public boolean showDestination() {
            return this == ALL || this == DESTINATION;
        }
    }

    public static final String GUI_LINE_SHOW_ARRIVAL_NAME = "train_text";

    public static final String NBT_TRAIN_TEXT = "TrainText";

    ETrainTextComponents getTrainTextComponents();
    void setTrainTextComponents(ETrainTextComponents v);

    @Environment(EnvType.CLIENT)
    default void buildTrainTextGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildTrainTextGui(this, context);
    }
    
    default void copyTrainTextSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ITrainTextSetting o) {
            setTrainTextComponents(o.getTrainTextComponents());
        }
    }
}
