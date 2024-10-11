package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.events.TrainArrivalAndDepartureEvent;

@Mixin(Train.class)
public class TrainMixin {
    
    public Train self() {
        return (Train)(Object)this;
    }
    
    @Inject(method = "arriveAt", remap = false, at = @At(value = "TAIL"))
    public void onArriveAt(GlobalStation station, CallbackInfo ci) {
        if (CRNEventsManager.isRegistered(TrainArrivalAndDepartureEvent.class)) {
            CRNEventsManager.getEvent(TrainArrivalAndDepartureEvent.class).run(self(), station, true);
        }
    }

    @Inject(method = "leaveStation", remap = false, at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onLeaveStation(CallbackInfo ci, GlobalStation currentStation) {
        if (CRNEventsManager.isRegistered(TrainArrivalAndDepartureEvent.class)) {
            CRNEventsManager.getEvent(TrainArrivalAndDepartureEvent.class).run(self(), currentStation, false);
        }
    }
}
