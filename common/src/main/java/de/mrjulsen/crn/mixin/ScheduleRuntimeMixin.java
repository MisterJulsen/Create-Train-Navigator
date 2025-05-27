package de.mrjulsen.crn.mixin;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.List;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.events.CreateTrainPredictionEvent;
import de.mrjulsen.crn.event.events.ScheduleResetEvent;
import de.mrjulsen.crn.event.events.SubmitTrainPredictionsEvent;
import de.mrjulsen.crn.event.events.TrainDestinationChangedEvent;
import de.mrjulsen.crn.util.PenaltyResult;
import de.mrjulsen.crn.util.PenaltyResult.Category;
import de.mrjulsen.crn.util.PenaltyResult.Type;
import de.mrjulsen.mcdragonlib.data.MapCache;
import dev.architectury.injectables.annotations.PlatformOnly;
import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.data.schedule.condition.DynamicDelayCondition;
import de.mrjulsen.crn.data.schedule.instruction.ICustomSuggestionsInstruction;
import de.mrjulsen.crn.data.schedule.instruction.IPredictableInstruction;
import de.mrjulsen.crn.data.schedule.instruction.IStationPredictableInstruction;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;

@Mixin(ScheduleRuntime.class)
public class ScheduleRuntimeMixin {

    public final Map<Class<? extends IStationPredictableInstruction>, IStationPredictableInstruction> customData = new LinkedHashMap<>();

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

    @Inject(method = "predictForEntry", remap = false, at = @At(value = "HEAD"))
    public void onPredictForEntryPre(int index, String currentTitle, int accumulatedTime, Collection<TrainDeparturePrediction> predictions, CallbackInfoReturnable<Integer> cir) {
        ScheduleInstruction instruction = self().getSchedule().entries.get(index).instruction;
        if (instruction instanceof IStationPredictableInstruction predictable) {
            customData.put(predictable.getClass(), predictable::predictForStation);
        }
        if (instruction instanceof IPredictableInstruction predictable) {
            TrainListener.getTrainData(accessor().crn$getTrain().id).ifPresent(x -> predictable.predict(x, accessor().crn$getTrain().runtime, index, accessor().crn$getTrain()));
        }
    }

    @Inject(method = "createPrediction", remap = false, at = @At(value = "RETURN"))
    public void onCreatePrediction(int index, String destination, String currentTitle, int time, CallbackInfoReturnable<TrainDeparturePrediction> cir) {
        if (CRNEventsManager.isRegistered(CreateTrainPredictionEvent.class) && cir.getReturnValue() != null) {
            int stayDuration = accessor().crn$runEstimateStayDuration(index);
            int minStayDuration = estimateMinStayDuration(accessor().crn$getTrain(), index);
            CRNEventsManager.getEvent(CreateTrainPredictionEvent.class).run(accessor().crn$getTrain(), self(), new LinkedHashMap<>(customData), index, stayDuration, minStayDuration, cir.getReturnValue());
        }
    }

    

    private static int estimateMinStayDuration(Train train, int index) {
        Schedule schedule = train.runtime.getSchedule();
		if (index >= schedule.entries.size()) {
			if (!schedule.cyclic)
				return -1;
			index = 0;
		}

		ScheduleEntry scheduleEntry = schedule.entries.get(index);
		Columns: for (List<ScheduleWaitCondition> list : scheduleEntry.conditions) {
			int total = 0;
			for (ScheduleWaitCondition condition : list) {
				if (condition instanceof DynamicDelayCondition wait)
				    total += wait.minWaitTicks();
                else if (condition instanceof ScheduledDelay wait)
                    total += wait.totalWaitTicks();
                else
                    continue Columns;
			}
			return total;
		}

		return -1;
	}
}
