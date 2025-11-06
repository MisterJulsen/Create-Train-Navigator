package de.mrjulsen.crn.mixin;

import java.util.Collection;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.events.ScheduleResetEvent;
import de.mrjulsen.crn.event.events.SubmitTrainPredictionsEvent;
import de.mrjulsen.crn.event.events.TrainDestinationChangedEvent;
import de.mrjulsen.crn.util.PenaltyResult;
import de.mrjulsen.crn.util.PenaltyResult.Category;
import de.mrjulsen.crn.util.PenaltyResult.Type;
import de.mrjulsen.mcdragonlib.util.MapCache;
import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.data.schedule.instruction.ICustomSuggestionsInstruction;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;

@Mixin(ScheduleRuntime.class)
public class ScheduleRuntimeMixin {

    public ScheduleRuntime self() {
        return (ScheduleRuntime)(Object)this;
    }

    public ScheduleRuntimeAccessor accessor() {
        return (ScheduleRuntimeAccessor)(Object)this;
    }


    @Inject(method = "submitPredictions", remap = false, at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onSubmitPredictions(CallbackInfoReturnable<Collection<TrainDeparturePrediction>> cir, Collection<TrainDeparturePrediction> predictions, int entryCount, int accumulatedTime, int current) {
        if (CRNEventsManager.isRegistered(SubmitTrainPredictionsEvent.class)) {
            CRNEventsManager.getEvent(SubmitTrainPredictionsEvent.class).run(accessor().crn$getTrain(), predictions, entryCount, accumulatedTime, current);
        }        
    }
    
    @Inject(method = "<init>", remap = false, at = @At(value = "TAIL"))
    public void onResetWhileInit(CallbackInfo ci) {
        if (CRNEventsManager.isRegistered(ScheduleResetEvent.class)) {
            CRNEventsManager.getEvent(ScheduleResetEvent.class).run(accessor().crn$getTrain(), true);
        }
    }
    
    @Inject(method = {"setSchedule", "discardSchedule"}, remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/schedule/ScheduleRuntime;reset()V"))
    public void onReset(CallbackInfo ci) {
        if (CRNEventsManager.isRegistered(ScheduleResetEvent.class)) {
            CRNEventsManager.getEvent(ScheduleResetEvent.class).run(accessor().crn$getTrain(), false);
        }
    }

    @Inject(method = "startCurrentInstruction", remap = false, at = @At(value = "RETURN", ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onStartCurrentInstructionRetForge(Level level, CallbackInfoReturnable<DiscoveredPath> cir, ScheduleEntry entry, ScheduleInstruction instruction) {
		if (CRNEventsManager.isRegistered(TrainDestinationChangedEvent.class) && cir.getReturnValue() != null && instruction instanceof DestinationInstruction) {
            CRNEventsManager.getEvent(TrainDestinationChangedEvent.class).run(accessor().crn$getTrain(), accessor().crn$getTrain().getCurrentStation(), cir.getReturnValue().destination, self().currentEntry);
        }
    }
   
}
