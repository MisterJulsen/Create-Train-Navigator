package de.mrjulsen.crn.item;

import com.mojang.blaze3d.vertex.PoseStack;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.mcdragonlib.client.ber.RenderGraphics;
import de.mrjulsen.mcdragonlib.client.render.ICustomItemRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NavigatorItem extends Item implements ICustomItemRenderer {

    public static final String NBT_BACKGROUND_ID = "BackgroundId";

    public NavigatorItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pLevel.isClientSide) {
            ClientWrapper.showNavigatorGui();
            return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
        }        
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void renderAdditional(RenderGraphics graphics, ItemStack itemStack, TransformType transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        ClientWrapper.renderNavigatorItem(graphics, itemStack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
    }
}
