package de.mrjulsen.crn.item;

import de.mrjulsen.crn.client.ClientWrapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NavigatorItem extends Item {

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

    /*
    @Environment(EnvType.CLIENT)
    @SuppressWarnings("resource")
    @Override
    public void renderAdditional(RenderGraphics graphics, ItemStack itemStack, TransformType transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        Font font = Minecraft.getInstance().font;
        poseStack.mulPose(Vector3f.XP.rotationDegrees(90F));
        poseStack.translate(4, 2, -0.76);
        
        poseStack.pushPose();
        poseStack.translate(4, 0.8f, 0);
        poseStack.scale(0.075f, 0.075f, 0.075f);
        BERUtils.drawString(graphics, font, 0, 0, "Day " + (DragonLib.getCurrentWorldTime() / DragonLib.ticksPerDay()), 0xFFFFFFFF, EAlignment.CENTER, false, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        
        poseStack.pushPose();
        poseStack.translate(4, 2, 0);
        poseStack.scale(0.2f, 0.2f, 0.2f);
        BERUtils.drawString(graphics, font, 0, 0, TimeUtils.formatTime(DragonLib.getCurrentWorldTime(), ModClientConfig.TIME_FORMAT.get()), 0xFFFFFFFF, EAlignment.CENTER, false, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
    */
}
