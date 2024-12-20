package de.mrjulsen.crn.mixin;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.entity.Navigation;

import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition;
import de.mrjulsen.crn.data.schedule.condition.IDelayedWaitCondition.DelayedWaitConditionContext;
import de.mrjulsen.mcdragonlib.data.Pair;
import net.minecraft.world.level.Level;

@Mixin(Navigation.class)
public abstract class NavigationMixin implements INavigationExtension {

    public Queue<Pair<IDelayedWaitCondition, DelayedWaitConditionContext>> delayedWaitConditions = new ConcurrentLinkedQueue<>();

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

    //@Inject(method = "startNavigation", at = @At(value = "HEAD"), remap = false)
    //public void resetOnStart(DiscoveredPath path, CallbackInfoReturnable<Double> cir) {
    //    delayedWaitConditions.clear();
    //}
}
