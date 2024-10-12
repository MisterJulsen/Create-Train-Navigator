package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData;

import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.events.GlobalTrainDisplayDataRefreshEventPost;
import de.mrjulsen.crn.event.events.GlobalTrainDisplayDataRefreshEventPre;

@Mixin(GlobalTrainDisplayData.class)
public class GlobalTrainDisplayDataMixin {
    
    /*
     * Called every ~5 seconds, when create refreshes its display data.
     */

    @Inject(method = "refresh", remap = false, at = @At(value = "HEAD"))
    private static void onRefreshPre(CallbackInfo ci) {
        if (CRNEventsManager.isRegistered(GlobalTrainDisplayDataRefreshEventPre.class)) {
            CRNEventsManager.getEvent(GlobalTrainDisplayDataRefreshEventPre.class).run();
        }
    }

    @Inject(method = "refresh", remap = false, at = @At(value = "TAIL"))
    private static void onRefreshPost(CallbackInfo ci) {
        if (CRNEventsManager.isRegistered(GlobalTrainDisplayDataRefreshEventPost.class)) {
            CRNEventsManager.getEvent(GlobalTrainDisplayDataRefreshEventPost.class).run();
        }
    }
}
