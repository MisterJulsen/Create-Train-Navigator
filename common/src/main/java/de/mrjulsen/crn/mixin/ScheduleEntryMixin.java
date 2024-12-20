package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import net.minecraft.nbt.CompoundTag;

/**
 * Fixes a crash in Create when a condition got removed from the game
 */
@Mixin(ScheduleEntry.class)
public class ScheduleEntryMixin {

    @Inject(method = "fromTag", remap = false, at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void onFromTag(CompoundTag tag, CallbackInfoReturnable<ScheduleEntry> cir, ScheduleEntry entry) {
        entry.conditions.forEach(x -> x.removeIf(y -> y == null));
	}
}
