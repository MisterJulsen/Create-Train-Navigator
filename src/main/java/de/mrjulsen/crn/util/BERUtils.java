package de.mrjulsen.crn.util;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;

public class BERUtils {

    public void initRenderEngine() {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    public void setTint(int r, int g, int b, int a) {
        RenderSystem.setShaderColor(r, g, b, a);
    }

    public void renderTexture(ResourceLocation texture, PoseStack poseStack, float x, float y, float z, float w, float h, float u0, float v0, float u1, float v1, Direction facing, int tint, int light) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        Tesselator t = Tesselator.getInstance();
        BufferBuilder builder = t.getBuilder();

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        Vec3i normal = facing.getNormal();
        builder.vertex(poseStack.last().pose(), x + w, y + h, z).uv(u1, v1).color(tint).uv2(light).normal(poseStack.last().normal(), normal.getX(), normal.getY(), normal.getZ()).endVertex();
        builder.vertex(poseStack.last().pose(), x + w, y,  z).uv(u1, v0).color(tint).uv2(light).normal(poseStack.last().normal(), normal.getX(), normal.getY(), normal.getZ()).endVertex();
        builder.vertex(poseStack.last().pose(), x, y,  z).uv(u0, v0).color(tint).uv2(light).normal(poseStack.last().normal(), normal.getX(), normal.getY(), normal.getZ()).endVertex();
        builder.vertex(poseStack.last().pose(), x, y + h,  z).uv(u0, v1).color(tint).uv2(light).normal(poseStack.last().normal(), normal.getX(), normal.getY(), normal.getZ()).endVertex();
        t.end();
    }

    public void fillColor(MultiBufferSource pBufferSource, int color, PoseStack poseStack, float x, float y, float z, float w, float h, Direction facing, int light) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator t = Tesselator.getInstance();
        BufferBuilder builder = t.getBuilder();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vec3i normal = facing.getNormal();
        builder.vertex(poseStack.last().pose(), x + w, y + h, z).uv(1, 1).color(color).uv2(light).overlayCoords(OverlayTexture.NO_OVERLAY).normal(poseStack.last().normal(), normal.getX(), normal.getY(), normal.getZ()).endVertex();
        builder.vertex(poseStack.last().pose(), x + w, y,  z).uv(1, 0).color(color).uv2(light).overlayCoords(OverlayTexture.NO_OVERLAY).normal(poseStack.last().normal(), normal.getX(), normal.getY(), normal.getZ()).endVertex();
        builder.vertex(poseStack.last().pose(), x, y,  z).uv(0, 0).color(color).uv2(light).overlayCoords(OverlayTexture.NO_OVERLAY).normal(poseStack.last().normal(), normal.getX(), normal.getY(), normal.getZ()).endVertex();
        builder.vertex(poseStack.last().pose(), x, y + h,  z).uv(0, 1).color(color).uv2(light).overlayCoords(OverlayTexture.NO_OVERLAY).normal(poseStack.last().normal(), normal.getX(), normal.getY(), normal.getZ()).endVertex();
        t.end();
    }
}
