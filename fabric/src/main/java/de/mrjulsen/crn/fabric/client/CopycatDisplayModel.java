package de.mrjulsen.crn.fabric.client;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.AdvancedDisplaySlopedBlock;
import de.mrjulsen.crn.block.properties.ESide;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CopycatDisplayModel extends CopycatModel {

    public CopycatDisplayModel(BakedModel originalModel, ConnectedTextureBehaviour ctDisplay, ConnectedTextureBehaviour ctFrame) {
        super(new WrappedCTModel(new WrappedCTModel(originalModel, ctDisplay), ctFrame));
    }

    private static Vec3 lerp2D(Vec3 p00, Vec3 p01, Vec3 p11, Vec3 p10, double x, double y) {
        Vec3 bottom = p00.lerp(p10, x);
        Vec3 top = p01.lerp(p11, x);
        return bottom.lerp(top, y);
    }

    @Override
    protected void emitBlockQuadsInner(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context, BlockState material, CullFaceRemovalData cullFaceRemovalData, OcclusionData occlusionData) {
        if (material == null || material.is(AllBlocks.COPYCAT_BASE.get())) {
            ((FabricBakedModel) wrapped).emitBlockQuads(blockView, state, pos, randomSupplier, context);
            return;
        }

        Direction facing = state.getOptionalValue(AbstractAdvancedDisplayBlock.FACING).orElse(Direction.NORTH);
        ESide doubleSided;
        if (state.getBlock() instanceof AdvancedDisplaySlopedBlock) {
            doubleSided = ESide.BOTH;
        } else {
            doubleSided = state.getOptionalValue(AbstractAdvancedSidedDisplayBlock.SIDE).orElse(ESide.FRONT);
        }
        ESide isDoubleSided = doubleSided;

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel materialModel = blockRenderer.getBlockModel(material);

        Map<Direction, List<MutableQuadView>> materialLayerMap = new HashMap<>();
        context.pushTransform(quad -> {
            MutableQuadView copy = IntermediateMutableQuadView.create();
            copy.copyFrom(quad);
            materialLayerMap.computeIfAbsent(quad.lightFace(), d -> new ArrayList<>()).add(copy);
            return false;
        });
        ((FabricBakedModel) materialModel).emitBlockQuads(blockView, material, pos, randomSupplier, context);
        context.popTransform();

        MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
        QuadEmitter emitter = meshBuilder.getEmitter();
        SpriteFinder spriteFinder = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS));
        RenderMaterial cutoutMaterial = RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(BlendMode.CUTOUT).find();

        context.pushTransform(formQuad -> {
            Direction side = formQuad.lightFace();

            if (side == facing || (side == facing.getOpposite() && isDoubleSided != ESide.FRONT)) {
                formQuad.material(cutoutMaterial);
                return true;
            }

            if (cullFaceRemovalData.shouldRemove(formQuad.cullFace())) return false;
            if (occlusionData.isOccluded(side)) {
                emitter.copyFrom(formQuad).emit();
                return false;
            }

            List<MutableQuadView> layers = materialLayerMap.get(side);
            if (layers == null || layers.isEmpty()) {
                layers = materialLayerMap.values().stream().findFirst().orElse(null);
            }

            if (layers != null) {
                for (MutableQuadView sourceQuad : layers) {
                    TextureAtlasSprite materialSprite = spriteFinder.find(sourceQuad);
                    emitSplitQuads(emitter, formQuad, sourceQuad, materialSprite);
                }
            }

            return false;
        });

        ((FabricBakedModel) wrapped).emitBlockQuads(blockView, state, pos, randomSupplier, context);
        context.popTransform();
        meshBuilder.build().outputTo(context.getEmitter());
    }

    private void emitSplitQuads(QuadEmitter emitter, MutableQuadView geoQuad, MutableQuadView sourceMaterial, TextureAtlasSprite materialSprite) {
        Vec3 p0 = getVec(geoQuad, 0);
        Vec3 p1 = getVec(geoQuad, 1);
        Vec3 p3 = getVec(geoQuad, 3);

        double worldW = p0.distanceTo(p3);
        double worldH = p0.distanceTo(p1);
        double pixelsW = Math.round(worldW * 16.0);
        double pixelsH = Math.round(worldH * 16.0);

        if (pixelsW <= 0 || pixelsH <= 0) {
            emitFullQuad(emitter, geoQuad, sourceMaterial, materialSprite);
            return;
        }

        double splitW = Math.floor(pixelsW / 2.0) / 16.0;
        double splitH = Math.floor(pixelsH / 2.0) / 16.0;
        double ratioW = (worldW > 1e-6) ? splitW / worldW : 0.5;
        double ratioH = (worldH > 1e-6) ? splitH / worldH : 0.5;
        double remW = worldW - splitW;
        double remH = worldH - splitH;

        // BL
        emitQuadrant(emitter, geoQuad, sourceMaterial, materialSprite, 0.0, 0.0, ratioW, ratioH, 0.0, 0.0, splitW, splitH);
        // BR
        emitQuadrant(emitter, geoQuad, sourceMaterial, materialSprite, ratioW, 0.0, 1.0, ratioH, 1.0 - remW, 0.0, 1.0, splitH);
        // TL
        emitQuadrant(emitter, geoQuad, sourceMaterial, materialSprite, 0.0, ratioH, ratioW, 1.0, 0.0, 1.0 - remH, splitW, 1.0);
        // TR
        emitQuadrant(emitter, geoQuad, sourceMaterial, materialSprite, ratioW, ratioH, 1.0, 1.0, 1.0 - remW, 1.0 - remH, 1.0, 1.0);
    }

    private void emitFullQuad(QuadEmitter emitter, MutableQuadView geoQuad, MutableQuadView sourceMaterial, TextureAtlasSprite sprite) {
        emitter.copyFrom(geoQuad);
        emitter.material(sourceMaterial.material());
        emitter.colorIndex(sourceMaterial.colorIndex());
        emitter.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);

        for (int i = 0; i < 4; i++) {
            emitter.sprite(i, 0, sourceMaterial.spriteU(i, 0), sourceMaterial.spriteV(i, 0));
        }
        emitter.emit();
    }

    private void emitQuadrant(QuadEmitter emitter, MutableQuadView geo, MutableQuadView sourceMaterial, TextureAtlasSprite materialSprite, double x0, double y0, double x1, double y1, double u0, double v0, double u1, double v1) {
        emitter.copyFrom(geo);

        setVec(emitter, 0, lerp2D(getVec(geo, 0), getVec(geo, 1), getVec(geo, 2), getVec(geo, 3), x0, y0));
        setVec(emitter, 1, lerp2D(getVec(geo, 0), getVec(geo, 1), getVec(geo, 2), getVec(geo, 3), x0, y1));
        setVec(emitter, 2, lerp2D(getVec(geo, 0), getVec(geo, 1), getVec(geo, 2), getVec(geo, 3), x1, y1));
        setVec(emitter, 3, lerp2D(getVec(geo, 0), getVec(geo, 1), getVec(geo, 2), getVec(geo, 3), x1, y0));

        emitter.material(sourceMaterial.material());
        emitter.colorIndex(sourceMaterial.colorIndex());
        emitter.spriteBake(materialSprite, MutableQuadView.BAKE_LOCK_UV);

        float mU0 = sourceMaterial.spriteU(0, 0);
        float mV0 = sourceMaterial.spriteV(0, 0);
        float mU1 = sourceMaterial.spriteU(1, 0);
        float mV1 = sourceMaterial.spriteV(1, 0);
        float mU2 = sourceMaterial.spriteU(2, 0);
        float mV2 = sourceMaterial.spriteV(2, 0);
        float mU3 = sourceMaterial.spriteU(3, 0);
        float mV3 = sourceMaterial.spriteV(3, 0);

        double eps = 0.001;
        Vec2 uv0 = lerp2DUV(mU0, mV0, mU1, mV1, mU2, mV2, mU3, mV3, u0 + eps, v0 + eps);
        Vec2 uv1 = lerp2DUV(mU0, mV0, mU1, mV1, mU2, mV2, mU3, mV3, u0 + eps, v1 - eps);
        Vec2 uv2 = lerp2DUV(mU0, mV0, mU1, mV1, mU2, mV2, mU3, mV3, u1 - eps, v1 - eps);
        Vec2 uv3 = lerp2DUV(mU0, mV0, mU1, mV1, mU2, mV2, mU3, mV3, u1 - eps, v0 + eps);

        emitter.sprite(0, 0, uv0.x, uv0.y);
        emitter.sprite(1, 0, uv1.x, uv1.y);
        emitter.sprite(2, 0, uv2.x, uv2.y);
        emitter.sprite(3, 0, uv3.x, uv3.y);

        emitter.emit();
    }

    private Vec2 lerp2DUV(float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3, double x, double y) {
        double uvEps = 1e-3;
        double safeX = Math.max(uvEps, Math.min(1.0 - uvEps, x));
        double safeY = Math.max(uvEps, Math.min(1.0 - uvEps, y));

        double bottomU = u0 + (u3 - u0) * safeX;
        double bottomV = v0 + (v3 - v0) * safeX;
        double topU = u1 + (u2 - u1) * safeX;
        double topV = v1 + (v2 - v1) * safeX;

        return new Vec2((float) (bottomU + (topU - bottomU) * safeY), (float) (bottomV + (topV - bottomV) * safeY));
    }

    private Vec3 getVec(MutableQuadView q, int index) {
        return new Vec3(q.x(index), q.y(index), q.z(index));
    }

    private void setVec(MutableQuadView q, int index, Vec3 v) {
        q.pos(index, (float) v.x, (float) v.y, (float) v.z);
    }
}