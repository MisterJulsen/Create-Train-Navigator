package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.mrjulsen.crn.item.NavigatorItem;
import dev.architectury.injectables.annotations.PlatformOnly;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

@Mixin(ModelBakery.class)
public class ModelBakeryMixin {
    @Shadow
    private void loadTopLevel(ModelResourceLocation location) { throw new AssertionError(); }

    @PlatformOnly(value = PlatformOnly.FABRIC)
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelBakery;loadTopLevel(Lnet/minecraft/client/resources/model/ModelResourceLocation;)V", ordinal = 0, shift = At.Shift.AFTER))
    public void registerCustomModels(ResourceManager resourceManager, BlockColors blockColors, ProfilerFiller profiler, int maxMipmapLevel, CallbackInfo ci) {
        this.loadTopLevel(NavigatorItem.WORLD_MODEL);
    }
    
    @PlatformOnly(value = PlatformOnly.FORGE)
    @Inject(method = "processLoading", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelBakery;loadTopLevel(Lnet/minecraft/client/resources/model/ModelResourceLocation;)V", ordinal = 0, shift = At.Shift.AFTER))
    public void registerCustomModels(ProfilerFiller profiler, int b, CallbackInfo ci) {
        this.loadTopLevel(NavigatorItem.WORLD_MODEL);
    }
}