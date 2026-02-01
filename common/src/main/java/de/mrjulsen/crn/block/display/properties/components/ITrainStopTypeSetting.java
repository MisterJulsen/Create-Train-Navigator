package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.crn.data.train.ETrainStopState;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

import java.util.Arrays;

public interface ITrainStopTypeSetting {

    public static enum ETrainStopType implements ITranslatableEnum {
        ALL((byte)0, "all"),
        ARRIVALS_ONLY((byte)1, "arrivals_only"),
        ARRIVALS_PREFERRED((byte)2, "arrivals_preferred"),
        DEPARTURES_ONLY((byte)3, "departures_only"),
        DEPARTURES_PREFERRED((byte)4, "departures_preferred");

        final String name;
        final byte id;

        ETrainStopType(byte id, String name) {
            this.name = name;
            this.id = id;
        }

        public byte getId() {
            return this.id;
        }	

        public static ETrainStopType getById(int id) {
            return Arrays.stream(values()).filter(x -> x.getId() == (byte)id).findFirst().orElse(ETrainStopType.ALL);
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public boolean showArrivals(boolean isTerminus) {
            return this == ALL || this == ARRIVALS_ONLY || this == ARRIVALS_PREFERRED || (isTerminus && this == DEPARTURES_PREFERRED);
        }

        public boolean showDepartures(boolean isStart) {
            return this == ALL || this == DEPARTURES_ONLY || this == DEPARTURES_PREFERRED || (isStart && this == ARRIVALS_PREFERRED);
        }

        @Override
        public Data getTranslationData() {
            return new Data(CreateRailwaysNavigator.MOD_ID, "train_stop_type", name);
        }
    }

    public static final String GUI_LINE_TRAIN_STOP_TYPE_NAME = "train_text";

    public static final String NBT_TRAIN_STOP_TYPE = "TrainText";
    public static final String LEGACY_NBT_SHOW_ARRIVAL = "ShowArrival";

    ETrainStopType getTrainStopType();
    void setTrainStopType(ETrainStopType v);

    default void buildTrainStopTypeGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildTrainStopTypeGui(this, context);
    }
    
    default void copyTrainStopTypeSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ITrainStopTypeSetting o) {
            setTrainStopType(o.getTrainStopType());
        }
    }


    public static ETrainStopState resolveStopState(StationDisplayData stop, ITrainStopTypeSetting settings) {
        ITrainStopTypeSetting.ETrainStopType stopType = settings.getTrainStopType();
        boolean start = stop.isFirstStop();
        boolean terminus = stop.isLastStop();
        return resolveStopState(stop, stopType.showDepartures(start), stopType.showArrivals(terminus));
    }

    public static ETrainStopState resolveStopState(StationDisplayData stop, boolean allowDepartures, boolean allowArrivals) {
        StationDisplayData.State state = stop.getState();
        boolean showDeparture = allowDepartures && !stop.isNextSectionExcluded();
        boolean showArrival = allowArrivals && !stop.isPrevSectionExcluded();
        boolean showAsArrival = showArrival && (!showDeparture || !state.isWaiting());
        return showAsArrival ? ETrainStopState.ARRIVAL : ETrainStopState.DEPARTURE;
    }
}
