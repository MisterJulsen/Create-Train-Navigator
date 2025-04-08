package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.item.NavigatorItem;
import de.mrjulsen.crn.registry.ModItems;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Shadow
    ItemModelShaper itemModelShaper;

    @ModifyVariable(method = "render", at = @At(value = "HEAD"), argsOnly = true)
    public BakedModel onRender(BakedModel value, ItemStack itemStack, ItemTransforms.TransformType transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        if (itemStack.is(ModItems.NAVIGATOR.get()) && (transformType != TransformType.FIRST_PERSON_RIGHT_HAND && transformType != TransformType.FIRST_PERSON_LEFT_HAND)) {
            return itemModelShaper.getModelManager().getModel(NavigatorItem.WORLD_MODEL);
        }
        return value;
    }
}
