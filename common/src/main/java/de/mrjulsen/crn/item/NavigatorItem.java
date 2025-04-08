package de.mrjulsen.crn.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.RenderGraphics;
import de.mrjulsen.mcdragonlib.client.render.ICustomItemRenderer;
import de.mrjulsen.mcdragonlib.client.util.BERUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NavigatorItem extends Item implements ICustomItemRenderer {

    private static final String NBT_BACKGROUND_ID = "BackgroundId";

    @Environment(EnvType.CLIENT)
    public static final ModelResourceLocation WORLD_MODEL = new ModelResourceLocation(CreateRailwaysNavigator.MOD_ID, "navigator_world", "inventory");

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
        if (transformType != TransformType.FIRST_PERSON_LEFT_HAND && transformType != TransformType.FIRST_PERSON_RIGHT_HAND) {
            return;
        }

        int backgroundId = itemStack.getOrCreateTag().getInt(NBT_BACKGROUND_ID);
        
        Font font = Minecraft.getInstance().font;
        poseStack.mulPose(Vector3f.XP.rotationDegrees(90F));
        poseStack.translate(4, 2, -1.26f);
        BERUtils.renderTexture(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, String.format("textures/item/navigator_backgrounds/%s.png", backgroundId)), graphics, false, 0, 0, 0, 8, 12, 0, 0, 1F / 12F * 8, 1F, Direction.UP, 0xFFFFFFFF, LightTexture.FULL_BRIGHT);
        
        poseStack.translate(0, 0, -0.01f);
        poseStack.pushPose();
        poseStack.translate(4, 0.8f, 0);
        poseStack.scale(0.075f, 0.075f, 0.075f);
        BERUtils.drawString(graphics, font, 0, 0, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".journey_info.date", (DragonLib.getCurrentWorldTime() + DragonLib.daytimeShift()) / DragonLib.ticksPerDay()), 0xFFFFFFFF, EAlignment.CENTER, false, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        
        poseStack.pushPose();
        poseStack.translate(4, 2, 0);
        poseStack.scale(0.2f, 0.2f, 0.2f);
        BERUtils.drawString(graphics, font, 0, 0, TimeUtils.formatTime(DragonLib.getCurrentWorldTime(), ModClientConfig.TIME_FORMAT.get()), 0xFFFFFFFF, EAlignment.CENTER, false, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
