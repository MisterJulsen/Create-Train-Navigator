package de.mrjulsen.crn.mixin;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.data.train.TrainListener;
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
        return (ScheduleRuntimeAccessor)this;
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

    @Inject(method = "startCurrentInstruction", remap = false, at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onStartCurrentInstructionRetFabric(CallbackInfoReturnable<DiscoveredPath> cir, ScheduleEntry entry, ScheduleInstruction instruction) {        
		if (CRNEventsManager.isRegistered(TrainDestinationChangedEvent.class) && cir.getReturnValue() != null && instruction instanceof DestinationInstruction) {
            CRNEventsManager.getEvent(TrainDestinationChangedEvent.class).run(accessor().crn$getTrain(), accessor().crn$getTrain().getCurrentStation(), cir.getReturnValue().destination, self().currentEntry);
        }        
    }
    
    @Inject(method = "startCurrentInstruction", remap = false, at = @At(value = "TAIL"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void onStartCurrentInstructionPost(CallbackInfoReturnable<Object> cir, ScheduleEntry entry, ScheduleInstruction instruction) {
        if (instruction instanceof ICustomSuggestionsInstruction custom) {
            TrainListener.getTrainData(accessor().crn$getTrain().id).ifPresent(x -> custom.run(self(), x, accessor().crn$getTrain(), self().currentEntry));
            self().state = ScheduleRuntime.State.PRE_TRANSIT;
            self().currentEntry++;
		}
        cir.setReturnValue(null);
    }

    @Inject(method = "startCurrentInstruction", remap = false, at = @At(value = "HEAD"), cancellable = true)
    public void startCurrentInstructionHeadForge(CallbackInfoReturnable<DiscoveredPath> cir) {        
		ScheduleEntry entry = self().getSchedule().entries.get(self().currentEntry);
		ScheduleInstruction instruction = entry.instruction;
        DiscoveredPath res = crn$customDestinationInstructions(self(), entry, instruction);
        if (res != null) {
            cir.setReturnValue(res);
        }
    }    

    @Unique
    private DiscoveredPath crn$customDestinationInstructions(ScheduleRuntime runtime, ScheduleEntry entry, ScheduleInstruction instruction) {
        if (instruction instanceof PrioritizedDestinationInstruction destination) {
            ScheduleRuntimeAccessor accessor = (ScheduleRuntimeAccessor)runtime;
            Train train = accessor.crn$getTrain();   
            List<String> filters = destination.getFilters();
            INavigationExtension ext = (INavigationExtension)train.navigation;

            DiscoveredPath selectedDestination = null;
            int selectedPainCount = Integer.MAX_VALUE;
            boolean anyMatch = false;


            MapCache<DiscoveredPath, GlobalStation, GlobalStation> navigationCache = new MapCache<>((station) -> {
                return train.navigation.findPathTo(station, Double.MAX_VALUE);
            }, GlobalStation::hashCode);

			if (!train.hasForwardConductor() && !train.hasBackwardConductor()) {
				train.status.missingConductor();
				accessor.crn$setCooldown(accessor.crn$getInterval());
				return null;
			}

            for (String regex : filters) {
                AtomicInteger painCount = new AtomicInteger(0);
                GlobalStation bestStation = null;
                DiscoveredPath bestPath = null;
                double bestCost = Double.MAX_VALUE;
                
                for (GlobalStation globalStation : train.graph.getPoints(EdgePointType.STATION)) {
                    try {
                        if (!globalStation.name.matches(regex))
                            continue;
                    } catch (PatternSyntaxException ignored) {
                        continue;
                    }
            
                    DiscoveredPath discoveredPath = navigationCache.get(globalStation, globalStation);

                    if (discoveredPath == null) {
                        continue;
                    }

                    if (discoveredPath.cost < 0)
                        continue;
                    if (discoveredPath.cost > bestCost)
                        continue;
                    bestStation = globalStation;
                    bestPath = discoveredPath;
                    bestCost = discoveredPath.cost;
                }

                if (bestStation == null) {
                    continue;
                }
                anyMatch = true;

                if (destination.shouldAvoidTrains() && (
                    (bestStation.getImminentTrain() != null && bestStation.getImminentTrain() != train) ||
                    (bestStation.getPresentTrain() != null && bestStation.getPresentTrain() != train) ||
                    (bestStation.getNearestTrain() != null && bestStation.getNearestTrain() != train)
                )) {
                    painCount.addAndGet(1);
                }

                ext.getPenaltiesByDirection().ifPresent(x -> {
                    for (PenaltyResult.Type type : x.getPenalties().keySet()) {
                        if (destination.shouldAvoidRedSignals() && type == Type.REDSTONE_RED_SIGNAL) {
                            painCount.addAndGet(1);
                        } else if (destination.shouldAvoidTrains() && (type.getCategory() == Category.TRAINS || type == Type.RED_SIGNAL)) {
                            painCount.addAndGet(1);
                        }
                    }
                });

                if (painCount.get() < selectedPainCount) {
                    selectedPainCount = painCount.get();
                    selectedDestination = bestPath;

                    if (painCount.get() <= 0)
                        break;
                }
            }

			if (selectedDestination == null) {
				if (anyMatch) {
					train.status.failedNavigation();
                } else {
					train.status.failedNavigationNoTarget(String.join(", ", filters));
                }
                accessor.crn$setCooldown(accessor.crn$getInterval());
				return null;
			}

			return selectedDestination;
		}
        return null;
    }
}
