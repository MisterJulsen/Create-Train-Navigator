package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.api.BoardEntry;
import de.mrjulsen.crn.backend.api.CallDirection;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

import java.util.Arrays;

public interface ITrainStopTypeSetting {

    public static enum ETrainStopType implements ITranslatableEnum {
        ALL((byte)0, "all"),
        ARRIVALS_ONLY((byte)1, "arrivals_only"),
        ARRIVALS_PREFERRED((byte)2, "arrivals_preferred"),
        DEPARTURES_ONLY((byte)3, "departures_only"),
        DEPARTURES_PREFERRED((byte)4, "departures_preferred");

        public static final ETrainStopType DEF_VALUE = DEPARTURES_PREFERRED;

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
            return Arrays.stream(values()).filter(x -> x.getId() == (byte)id).findFirst().orElse(DEF_VALUE);
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


    /**
     * Which half of a call a board with these settings shows: the train coming in, or going out.
     * <p>
     * Where both would do, the arrival is shown while the train is still on its way and the departure
     * once it is standing at the platform - at that point what a traveller wants to know is when it
     * leaves again. A call nobody can board is only ever an arrival, and one nobody can have arrived
     * on is only ever a departure.
     */
    public static CallDirection resolveDirection(BoardEntry entry, ITrainStopTypeSetting settings) {
        ETrainStopType type = settings.getTrainStopType();
        return resolveDirection(entry, type.showDepartures(entry.originating()), type.showArrivals(entry.terminus()));
    }

    public static CallDirection resolveDirection(BoardEntry entry, boolean allowDepartures, boolean allowArrivals) {
        boolean showDeparture = allowDepartures && !entry.terminus();
        boolean showArrival = allowArrivals && !entry.originating();
        return showArrival && (!showDeparture || !entry.isWaiting()) ? CallDirection.ARRIVAL : CallDirection.DEPARTURE;
    }

    /** Whether a board with these settings lists the given call at all. */
    public static boolean shows(BoardEntry entry, ETrainStopType type) {
        boolean showDeparture = type.showDepartures(entry.originating()) && !entry.terminus();
        boolean showArrival = type.showArrivals(entry.terminus()) && !entry.originating();
        return showArrival || showDeparture;
    }

    /**
     * Whether the given call is worth a line on a board with these settings, at the given time.
     * <p>
     * A cancelled train keeps its line for a while so travellers waiting for it find out that it is
     * not coming, but only until it would have gone anyway. This is the one and only place that
     * decides it: the server builds a board with it and the display re-checks it as time moves on, so
     * the two must be the same question rather than two similar ones.
     */
    public static boolean accepts(BoardEntry entry, ETrainStopType type, long now) {
        if (!shows(entry, type)) {
            return false;
        }
        return !entry.isCancelled()
            || now < entry.scheduled().departure() + ModCommonConfig.DISPLAY_LEAD_TIME.get();
    }
}
