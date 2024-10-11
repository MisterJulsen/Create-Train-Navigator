package de.mrjulsen.crn.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.web.WebsitePreparableReloadListener;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {
    
    public WebsitePreparableReloadListener websitemanager;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void onInit(RegistryAccess.Frozen frozen, Commands.CommandSelection commandSelection, int i, CallbackInfo ci) {
        this.websitemanager = new WebsitePreparableReloadListener();
        ModUtils.setWebsiteResourceManager(websitemanager);
    }

    @Inject(method = "listeners", at = @At("RETURN"), cancellable = true)
    private void addListener(CallbackInfoReturnable<List<PreparableReloadListener>> cir) {
        List<PreparableReloadListener> listeners = new ArrayList<>(cir.getReturnValue());
        listeners.add(websitemanager);
        cir.setReturnValue(listeners);
    }
}
