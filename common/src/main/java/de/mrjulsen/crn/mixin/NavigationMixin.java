package de.mrjulsen.crn.mixin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.simibubi.create.content.trains.graph.DiscoveredPath;
import net.createmod.catnip.data.Couple;
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

import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition;
import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition.DelayedWaitConditionContext;
import de.mrjulsen.crn.data.schedule.instruction.PrioritizedDestinationInstruction;
import de.mrjulsen.crn.util.IFrontierEntry;
import de.mrjulsen.crn.util.PenaltyResult;
import de.mrjulsen.mcdragonlib.util.Pair;
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

    @Override
    public Optional<PenaltyResult> getPenaltiesByDirection() {
        if (this.finalReasonByDirection == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.finalReasonByDirection.get(isForwardSelected));
    }

    @Inject(method = "findPathTo", remap = false, at = @At(value = "HEAD"))
    public void onStartNavigation(@Coerce Object a, double maxCost, CallbackInfoReturnable<?> cir) {
        if (train == null || train.runtime == null || train.runtime.getSchedule() == null) {
            return;
        }

        if ((train.runtime.currentEntry < 0 || train.runtime.currentEntry > train.runtime.getSchedule().entries.size()) || !(this.shouldCheckPenalties = train.runtime.getSchedule().entries.get(train.runtime.currentEntry).instruction instanceof PrioritizedDestinationInstruction)) {
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
    public void onEndNavigation(@Coerce Object a, double maxCost, CallbackInfoReturnable<?> cir) {
        this.shouldCheckPenalties = false;
        this.currentReasons = null;
    }

    @Inject(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "HEAD"))
    public void onStartSearch(double maxDistance, double maxCosts, boolean forward, ArrayList<GlobalStation> destinations, StationTest stationTest, CallbackInfo ci) {
        if (!this.shouldCheckPenalties) return;
        this.currentReasons = new PenaltyResult();
        this.forward = forward;
    }

    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;test(DDLjava/util/Map;Lnet/createmod/catnip/data/Pair;Lcom/simibubi/create/content/trains/station/GlobalStation;)Z", remap = false))
    public boolean onTestStation(StationTest test, double distance, double cost, Map<TrackEdge, net.createmod.catnip.data.Pair<Boolean, Couple<TrackNode>>> reachedVia, net.createmod.catnip.data.Pair<Couple<TrackNode>, TrackEdge> current, GlobalStation station) {
        boolean b = test.test(distance, cost, reachedVia, current, station);        
        if (this.shouldCheckPenalties && b) {
            this.finalReasonByDirection.put(forward, new PenaltyResult(currentReasons));            
        }
        return b;
    }

    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Ljava/util/PriorityQueue;add(Ljava/lang/Object;)Z", remap = false))
    public boolean onReadFrontierEntry(PriorityQueue<Object> queue, @Coerce Object obj) {
        IFrontierEntry entry = (IFrontierEntry)obj;
        if (this.shouldCheckPenalties) {
            entry.setPenaltyReasons(new PenaltyResult(currentReasons));            
        }
        return queue.add(obj);
    }

    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains/entity/Navigation$FrontierEntry;penalty:I", remap = false, opcode = Opcodes.GETFIELD))
    public int onCreateFrontierEntry(@Coerce Object obj) {
        IFrontierEntry entry = (IFrontierEntry)obj;        
        if (this.shouldCheckPenalties) {
            this.currentReasons = new PenaltyResult(entry.getPenaltyReasons());            
        }
        return entry.getPenalty();
    }

    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/SignalBoundary;isForcedRed(Lcom/simibubi/create/content/trains/graph/TrackNode;)Z", remap = false))
    public boolean onForceRed(SignalBoundary signal, TrackNode node) {
        boolean b = signal.isForcedRed(node);        
        if (this.shouldCheckPenalties && b) {
            this.currentReasons.add(PenaltyResult.Type.REDSTONE_RED_SIGNAL);            
        }
        return b;
    }

    @Redirect(
        method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            remap = false
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Ljava/util/PriorityQueue;<init>()V",
                remap = false
            )
        )
    )
    public Object onGetPenaltyByEdge(Map<TrackEdge, Integer> map, Object edge, Object defaultValue) {
        int val = map.getOrDefault((TrackEdge)edge, (Integer)defaultValue);
        if (this.shouldCheckPenalties && val > 0) {
            PenaltyResult.Type.getTypeByPenalty(PenaltyResult.Category.TRAINS, val).ifPresent(currentReasons::add);
        }
        return val;
    }

    @Redirect(method = "search(DDZLjava/util/ArrayList;Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V", remap = false, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/signal/SignalEdgeGroup;isOccupiedUnless(Lcom/simibubi/create/content/trains/signal/SignalBoundary;)Z", remap = false))
    public boolean onCheckOccupiedRedSignal(SignalEdgeGroup group, SignalBoundary signal) {
        boolean b = group.isOccupiedUnless(signal);
        if (this.shouldCheckPenalties && b) {
            this.currentReasons.add(PenaltyResult.Type.RED_SIGNAL);
        }
        return b;
    }

    @Inject(
        method = "findPathTo(Ljava/util/ArrayList;D)Lcom/simibubi/create/content/trains/graph/DiscoveredPath;",
        remap = false,
        at = @At(
            value = "RETURN"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/createmod/catnip/data/Couple;create(Ljava/lang/Object;Ljava/lang/Object;)Lnet/createmod/catnip/data/Couple;",
                remap = false
            )
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void selectDirection(ArrayList<GlobalStation> destinations, double maxCost, CallbackInfoReturnable<DiscoveredPath> cir, TrackGraph graph, Couple<DiscoveredPath> results) {
        if (this.shouldCheckPenalties) {
            Object selected = cir.getReturnValue();
            this.isForwardSelected = results.getFirst() == selected;
        }
    }
}