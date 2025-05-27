package de.mrjulsen.crn.client.ber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import de.mrjulsen.crn.block.NavigatorLecternBlock;
import de.mrjulsen.crn.block.blockentity.NavigatorLecternBlockEntity;
import de.mrjulsen.crn.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class NavigatorLecternBlockEntityRenderer extends SafeBlockEntityRenderer<NavigatorLecternBlockEntity> {

    public NavigatorLecternBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(NavigatorLecternBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ItemStack stack = ModItems.NAVIGATOR.asStack();
        Direction facing = be.getBlockState().getValue(NavigatorLecternBlock.FACING);

        ms.pushPose();
        ms.translate(0.5, 0.894f, 0.5);
        ms.pushPose();
        ms.scale(0.75f, 0.75f, 0.75f);
        ms.mulPose(Axis.YP.rotationDegrees((facing.getAxis() == Direction.Axis.Z ? facing.getOpposite() : facing).toYRot()));
        ms.mulPose(Axis.XP.rotationDegrees(-22.5f + 90));
        ms.translate(0, 0, -0.3);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
        ms.popPose();
        ms.popPose();
    }

}