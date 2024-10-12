package de.mrjulsen.crn.registry;

import java.util.Optional;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainStatus;
import de.mrjulsen.crn.data.train.TrainStatus.Registry;
import de.mrjulsen.crn.data.train.TrainStatus.TrainStatusCategory;
import de.mrjulsen.crn.data.train.TrainStatus.TrainStatusType;
import de.mrjulsen.crn.mixin.TrainStatusAccessor;

public final class ModTrainStatusInfos {
    
    public static final Registry REGISTRY = Registry.create(CreateRailwaysNavigator.MOD_ID);

    // Custom
    public static final TrainStatus RED_SIGNAL = REGISTRY.register("red_signal", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_status.red_signal"), (data) -> {
        return data.isCurrentSectionDelayed() &&
            data.getTrain().navigation.waitingForSignal != null &&
            data.getTrain().navigation.waitingForSignal.getSecond() &&
            data.occupyingTrains.isEmpty()
        ;
    }));

    public static final TrainStatus PRIORITY_OTHER_TRAIN = REGISTRY.register("priority_other_train", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_status.priority_other_train"), (data) -> {
        if (!data.isCurrentSectionDelayed() || data.getTrain().navigation.waitingForSignal == null) {
            return false;
        }
        
        Optional<Train> occupyingTrain = data.occupyingTrains.stream().findFirst();
        if (!occupyingTrain.isPresent()) {
            return false;
        }

        return TrainListener.data.containsKey(occupyingTrain.get().id) ? !TrainListener.data.get(occupyingTrain.get().id).isDelayed() : false;
    }));

    public static final TrainStatus PERVIOUS_TRAIN_DELAYED = REGISTRY.register("previous_train_delayed", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_status.delay_other_train"), (data) -> {
        if (!data.isCurrentSectionDelayed() || data.getTrain().navigation.waitingForSignal == null) {
            return false;
        }
        
        Optional<Train> occupyingTrain = data.occupyingTrains.stream().findFirst();
        if (!occupyingTrain.isPresent()) {
            return false;
        }

        return TrainListener.data.containsKey(occupyingTrain.get().id) ? TrainListener.data.get(occupyingTrain.get().id).isDelayed() : false;
    }));

    public static final TrainStatus TRACK_CLOSED = REGISTRY.register("track_closed", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_status.track_closed"), (data) -> {
        return data.isCurrentSectionDelayed() && ((TrainStatusAccessor)data.getTrain().status).crn$track();
    }));

    public static final TrainStatus STAFF_SHORTAGE = REGISTRY.register("staff_shortage", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_status.staff_shortage"), (data) -> {
        return data.isCurrentSectionDelayed() && ((TrainStatusAccessor)data.getTrain().status).crn$conductor();
    }));

    public static final TrainStatus OPERATIONAL_DISRUPTION = REGISTRY.register("operational_disruption", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_status.operational_disruption"), (data) -> {
        return data.isCurrentSectionDelayed() && ((TrainStatusAccessor)data.getTrain().status).crn$navigation();
    }));

    public static final TrainStatus SPECIAL_JOURNEY = REGISTRY.register("special_journey", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".train_status.special_trip"), (data) -> {
        return false;
    }));

    public static final TrainStatus OUT_OF_SERVICE = REGISTRY.register("out_of_service", new TrainStatus(TrainStatusCategory.TRAIN, TrainStatusType.DELAY, (data) -> ELanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.not_in_service"), (data) -> {
        return data.isCurrentSectionDelayed() && data.getTrain().runtime.paused;
    }));

    public static void init() {}
}
