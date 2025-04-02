package de.mrjulsen.crn.mixin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.Navigation.StationTest;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.utility.Couple;

import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition;
import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition.DelayedWaitConditionContext;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.util.IFrontierEntry;
import de.mrjulsen.crn.util.PenaltyResult;
import de.mrjulsen.mcdragonlib.data.Pair;
import dev.architectury.injectables.annotations.PlatformOnly;
import net.minecraft.world.level.Level;

@Mixin(Navigation.class)
public abstract class NavigationMixin implements INavigationExtension {

    public Queue<Pair<IDelayedWaitCondition, DelayedWaitConditionContext>> delayedWaitConditions = new ConcurrentLinkedQueue<>();
    
    public PenaltyResult currentReasons;
    public boolean forward;
    public Map<Boolean, PenaltyResult> finalReasonByDirection;

    @Shadow(remap = false)
    public List<Couple<TrackNode>> currentPath;
    @Shadow(remap = false)
    public Train train;



    @Override
    public void addDelayedWaitCondition(Pair<IDelayedWaitCondition, DelayedWaitConditionContext> pair) {
        delayedWaitConditions.add(pair);
    }

    @Override
    public boolean isDelayedWaitConditionPending() {
        return !delayedWaitConditions.isEmpty();
    }
    
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Train;leaveStation()V", shift = Shift.BEFORE), remap = false, cancellable = true)
    public void onTick(Level level, CallbackInfo ci) {
        if (!delayedWaitConditions.isEmpty()) {
            Pair<IDelayedWaitCondition, DelayedWaitConditionContext> p = delayedWaitConditions.peek();
            if (!p.getSecond().nbt().contains(IDelayedWaitCondition.NBT_DELAY)) {
                p.getSecond().nbt().putInt(IDelayedWaitCondition.NBT_DELAY, 0);
            }
            if (p.getFirst().runDelayed(p.getSecond())) {
                delayedWaitConditions.poll().getSecond().nbt().remove(IDelayedWaitCondition.NBT_DELAY);
            } else {                
                p.getSecond().nbt().putInt(IDelayedWaitCondition.NBT_DELAY, p.getSecond().nbt().getInt(IDelayedWaitCondition.NBT_DELAY) + 1);
            }
            ci.cancel();
        }
    }

    @Inject(method = "cancelNavigation", at = @At(value = "HEAD"), remap = false)
    public void resetOnCancel(CallbackInfo ci) {
        delayedWaitConditions.clear();
    }
    
    private boolean shouldCheckPenalties = false;
    private boolean isForwardSelected = false;

    /*
    @Inject(method = "startNavigation", remap = false, at = @At(value = "HEAD"))
    public void onStartNavigation(GlobalStation destination, double maxCost, boolean simulate, CallbackInfoReturnable<?> cir) {
        if (!(this.shouldCheckPenalties = train.runtime.getSchedule().entries.get(train.runtime.currentEntry).instruction instanceof PrioritizedDestinationInstruction)) {
            return;
        }
        
        if (this.finalReasonByDirection == null) {
            this.finalReasonByDirection = new IdentityHashMap<>(2);
        } else {
            this.finalReasonByDirection.clear();
        }
        this.finalReasonByDirection.put(true, new PenaltyResult());
        this.finalReasonByDirection.put(false, new PenaltyResult());
        this.currentReasons = null;
    }

    @Inject(method = "startNavigation", remap = false, at = @At(value = "RETURN"))
    public void onEndNavigation(GlobalStation destination, double maxCost, boolean simulate, CallbackInfoReturnable<?> cir) {
        this.shouldCheckPenalties = false;
        this.currentReasons = null;
    }
        */

    @Override
    public Optional<PenaltyResult> getPenaltiesByDirection() {
        if (this.finalReasonByDirection == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.finalReasonByDirection.get(isForwardSelected));
    }

    @Inject(method = "findPathTo", remap = false, at = @At(value = "HEAD"))
    public void onStartNavigationFor(@Coerce Object a, double maxCost, CallbackInfoReturnable<?> cir) {
        if (!(this.shouldCheckPenalties = train.runtime.getSchedule().entries.get(train.runtime.currentEntry).instruction instanceof PrioritizedDestinationInstruction)) {
            return;
        }
        
        if (this.finalReasonByDirection == null) {
            this.finalReasonByDirection = new IdentityHashMap<>(2);
        } else {
            this.finalReasonByDirection.clear();
        }
        this.finalReasonByDirection.put(true, new PenaltyResult());
        this.finalReasonByDirection.put(false, new PenaltyResult());
        this.currentReasons = null;
    }
        
    @Inject(method = "findPathTo", remap = false, at = @At(value = "TAIL"))
    public void onEndNavigationFor(@Coerce Object a, double maxCost, CallbackInfoReturnable<?> cir) {
        this.shouldCheckPenalties = false;
        this.currentReasons = null;
    }




    @PlatformOnly(value = PlatformOnly.FORGE)
    @Inject(method = "search(DDZLcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "HEAD"))
    public void onStartSearchFor(double maxDistance, double maxCosts, boolean forward, StationTest stationTest, CallbackInfo ci) {
        if (!this.shouldCheckPenalties) return;
        this.currentReasons = new PenaltyResult();
        this.forward = forward;
    }
    
    @PlatformOnly(value = PlatformOnly.FORGE)
    @Redirect(method = "search(DDZLcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;test", remap = false))
    public boolean onTestStationFor(StationTest test, double distance, double cost, Map<TrackEdge, com.simibubi.create.foundation.utility.Pair<Boolean, Couple<TrackNode>>> reachedVia, com.simibubi.create.foundation.utility.Pair<Couple<TrackNode>, TrackEdge> current, GlobalStation station) {
        boolean b = test.test(distance, cost, reachedVia, current, station);        
        if (this.shouldCheckPenalties && b) {
            this.finalReasonByDirection.put(forward, new PenaltyResult(currentReasons));            
        }
        return b;
    }
    
    @PlatformOnly(value = PlatformOnly.FORGE)
    @Redirect(method = "search(DDZLcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Ljava/util/PriorityQueue;add", remap = false))
    public boolean onReadFrontierEntryFor(PriorityQueue<Object> queue, @Coerce Object obj) {
        IFrontierEntry entry = (IFrontierEntry)obj;
        if (this.shouldCheckPenalties) {
            entry.setPenaltyReasons(new PenaltyResult(currentReasons));            
        }
        return queue.add(obj);
    }

    @PlatformOnly(value = PlatformOnly.FORGE)
    @Redirect(method = "search(DDZLcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains/entity/Navigation$FrontierEntry;penalty:I", remap = false, opcode = Opcodes.GETFIELD))
    public int onCreateFrontierEntryFor(@Coerce Object obj) {
        IFrontierEntry entry = (IFrontierEntry)obj;        
        if (this.shouldCheckPenalties) {
            this.currentReasons = new PenaltyResult(entry.getPenaltyReasons());            
        }
        return entry.getPenalty();
    }

    @PlatformOnly(value = PlatformOnly.FORGE)
    @Redirect(method = "search(DDZLcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/SignalBoundary;isForcedRed(Lcom/simibubi/create/content/trains/graph/TrackNode;)Z", remap = false))
    public boolean onForceRedFor(SignalBoundary signal, TrackNode node) {
        boolean b = signal.isForcedRed(node);        
        if (this.shouldCheckPenalties && b) {
            this.currentReasons.add(PenaltyResult.Type.REDSTONE_RED_SIGNAL);            
        }
        return b;
    }

    @PlatformOnly(value = PlatformOnly.FORGE)
    @Redirect(
        method = "search(DDZLcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;getOrDefault",
            remap = false
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Ljava/util/PriorityQueue;<init>",
                remap = false
            )
        )
    )
    public Object onGetPenaltyByEdgeFor(Map<TrackEdge, Integer> map, Object edge, Object defaultValue) {
        int val = map.getOrDefault((TrackEdge)edge, (Integer)defaultValue);
        if (this.shouldCheckPenalties && val > 0) {
            PenaltyResult.Type.getTypeByPenalty(PenaltyResult.Category.TRAINS, val).ifPresent(currentReasons::add);
        }
        return val;
    }

    @PlatformOnly(value = PlatformOnly.FORGE)
    @Redirect(method = "search(DDZLcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/SignalEdgeGroup;isOccupiedUnless", remap = false))
    public boolean onCheckOccupiedRedSignalFor(SignalEdgeGroup group, SignalBoundary signal) {
        boolean b = group.isOccupiedUnless(signal);
        if (this.shouldCheckPenalties && b) {
            this.currentReasons.add(PenaltyResult.Type.RED_SIGNAL);
        }
        return b;
    }


    @PlatformOnly(value = PlatformOnly.FORGE)
    @Inject(
        method = "findPathTo",
        remap = false,
        at = @At(
            value = "RETURN"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lcom/simibubi/create/foundation/utility/Couple;create",
                remap = false
            )
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void selectDirectionForge(@Coerce Object a, double maxCost, CallbackInfoReturnable<Object> cir, TrackGraph graph, Couple<Object> results) {
        if (this.shouldCheckPenalties) {
            Object selected = cir.getReturnValue();
            this.isForwardSelected = results.getFirst() == selected;
        }
    }





    

    @PlatformOnly(value = PlatformOnly.FABRIC)
    @Inject(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "HEAD"))
    public void onStartSearchFab(double maxDistance, double maxCosts, boolean forward, ArrayList<GlobalStation> destinations, StationTest stationTest, CallbackInfo ci) {
        if (!this.shouldCheckPenalties) return;
        this.currentReasons = new PenaltyResult();
        this.forward = forward;
    }

    
    @PlatformOnly(value = PlatformOnly.FABRIC)
    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;test", remap = false))
    public boolean onTestStationFab(StationTest test, double distance, double cost, Map<TrackEdge, com.simibubi.create.foundation.utility.Pair<Boolean, Couple<TrackNode>>> reachedVia, com.simibubi.create.foundation.utility.Pair<Couple<TrackNode>, TrackEdge> current, GlobalStation station) {
        boolean b = test.test(distance, cost, reachedVia, current, station);        
        if (this.shouldCheckPenalties && b) {
            this.finalReasonByDirection.put(forward, new PenaltyResult(currentReasons));            
        }
        return b;
    }
    
    @PlatformOnly(value = PlatformOnly.FABRIC)
    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Ljava/util/PriorityQueue;add", remap = false))
    public boolean onReadFrontierEntryFab(PriorityQueue<Object> queue, @Coerce Object obj) {
        IFrontierEntry entry = (IFrontierEntry)obj;
        if (this.shouldCheckPenalties) {
            entry.setPenaltyReasons(new PenaltyResult(currentReasons));            
        }
        return queue.add(obj);
    }

    @PlatformOnly(value = PlatformOnly.FABRIC)
    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains/entity/Navigation$FrontierEntry;penalty:I", remap = false, opcode = Opcodes.GETFIELD))
    public int onCreateFrontierEntryFab(@Coerce Object obj) {
        IFrontierEntry entry = (IFrontierEntry)obj;        
        if (this.shouldCheckPenalties) {
            this.currentReasons = new PenaltyResult(entry.getPenaltyReasons());            
        }
        return entry.getPenalty();
    }

    @PlatformOnly(value = PlatformOnly.FABRIC)
    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/SignalBoundary;isForcedRed(Lcom/simibubi/create/content/trains/graph/TrackNode;)Z", remap = false))
    public boolean onForceRedFab(SignalBoundary signal, TrackNode node) {
        boolean b = signal.isForcedRed(node);        
        if (this.shouldCheckPenalties && b) {
            this.currentReasons.add(PenaltyResult.Type.REDSTONE_RED_SIGNAL);            
        }
        return b;
    }

    @PlatformOnly(value = PlatformOnly.FABRIC)
    @Redirect(
        method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;getOrDefault",
            remap = false
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Ljava/util/PriorityQueue;<init>",
                remap = false
            )
        )
    )
    public Object onGetPenaltyByEdgeFab(Map<TrackEdge, Integer> map, Object edge, Object defaultValue) {
        int val = map.getOrDefault((TrackEdge)edge, (Integer)defaultValue);
        if (this.shouldCheckPenalties && val > 0) {
            PenaltyResult.Type.getTypeByPenalty(PenaltyResult.Category.TRAINS, val).ifPresent(currentReasons::add);
        }
        return val;
    }

    @PlatformOnly(value = PlatformOnly.FABRIC)
    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/SignalEdgeGroup;isOccupiedUnless", remap = false))
    public boolean onCheckOccupiedRedSignalFab(SignalEdgeGroup group, SignalBoundary signal) {
        boolean b = group.isOccupiedUnless(signal);
        if (this.shouldCheckPenalties && b) {
            this.currentReasons.add(PenaltyResult.Type.RED_SIGNAL);
        }
        return b;
    }

    @PlatformOnly(value = PlatformOnly.FABRIC)
    @Inject(
        method = "findPathTo(Ljava/util/ArrayList;D)Lcom/simibubi/create/content/trains/graph/DiscoveredPath;",
        remap = false,
        at = @At(
            value = "RETURN"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lcom/simibubi/create/foundation/utility/Couple;create",
                remap = false
            )
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void selectDirectionFabric(@Coerce Object a, double maxCost, CallbackInfoReturnable<Object> cir, TrackGraph graph, Couple<Object> results) {
        if (this.shouldCheckPenalties) {
            Object selected = cir.getReturnValue();
            this.isForwardSelected = results.getFirst() == selected;
        }
    }
}