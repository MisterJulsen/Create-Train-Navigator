package de.mrjulsen.crn.forge.client;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.AdvancedDisplaySlopedBlock;
import de.mrjulsen.crn.block.properties.ESide;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.List;

public class CopycatDisplayModel extends CopycatModel {

    private final ResourceLocation copycatTexture;

    public CopycatDisplayModel(BakedModel wrapped, ConnectedTextureBehaviour ctDisplay, ConnectedTextureBehaviour ctFrame, ResourceLocation copycatTexture) {
        super(new WrappedCTModel(new WrappedCTModel(wrapped, ctDisplay), ctFrame));
        this.copycatTexture = copycatTexture;
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
        ModelData.Builder b = super.gatherModelData(builder, world, pos, state, blockEntityData);
        for (ModelProperty<?> v : blockEntityData.getProperties()) {
            b.with((ModelProperty<? super Object>) v, (Object)blockEntityData.get(v));
        }
        return b;
    }

    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
        BlockState material = getMaterial(ModelData.EMPTY);
        if (material == null || material == AllBlocks.COPYCAT_BASE.getDefaultState()) {
            return originalModel.getQuads(state, side, rand);
        }
        return super.getQuads(state, side, rand);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        BlockState material = getMaterial(data);
        if (material == null || material == AllBlocks.COPYCAT_BASE.getDefaultState()) {
            return originalModel.getQuads(state, side, rand, data, renderType);
        }

        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        quads = quads.stream().filter(q -> !q.getSprite().contents().name().equals(copycatTexture)).toList();
        return quads;
    }

    protected List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand, BlockState material, ModelData wrappedData, RenderType renderType) {
        Direction facing = state == null ? Direction.NORTH : state.getOptionalValue(AbstractAdvancedDisplayBlock.FACING).orElse(Direction.NORTH);
        ESide doubleSided = ESide.FRONT;
        if (state != null) {
            if (state.getBlock() instanceof AdvancedDisplaySlopedBlock) {
                doubleSided = ESide.BOTH;
            } else {
                doubleSided = state.getOptionalValue(AbstractAdvancedSidedDisplayBlock.SIDE).orElse(ESide.FRONT);
            }
        }

        if (facing == side || (facing.getOpposite() == side && doubleSided != ESide.FRONT)) {
            return List.of();
        }

        List<BakedQuad> originalQuads = new ArrayList<>(originalModel.getQuads(material, side, rand, wrappedData, renderType));
        originalQuads.addAll(originalModel.getQuads(material, null, rand, wrappedData, renderType).stream().filter(x -> x.getDirection() == side).toList());
        List<BakedQuad> resultQuads = new ArrayList<>();

        for (BakedQuad quad : originalQuads) {
            resultQuads.addAll(cropQuads(side, rand, material, wrappedData, renderType, quad));
        }

        return resultQuads;
    }

    protected List<BakedQuad> cropQuads(Direction side, RandomSource rand, BlockState material, ModelData wrappedData, RenderType renderType, BakedQuad templateQuad) {
        BakedModel materialModel = getModelOf(material);
        List<BakedQuad> materialQuads = materialModel.getQuads(material, side, rand, wrappedData, renderType);
        List<BakedQuad> out = new ArrayList<>();
        Direction dir = templateQuad.getDirection();
        for (BakedQuad quad : materialQuads) {
            if (quad.getDirection() == dir) {
                out.addAll(splitIntoCornerQuadrants(templateQuad, quad));
            } else {
                out.add(quad);
            }
        }
        return out;
    }

    public static List<BakedQuad> splitIntoCornerQuadrants(BakedQuad geo, BakedQuad material) {
        List<BakedQuad> quadrants = new ArrayList<>();

        Vec3 p0 = BakedQuadHelper.getXYZ(geo.getVertices(), 0);
        Vec3 p1 = BakedQuadHelper.getXYZ(geo.getVertices(), 1);
        Vec3 p3 = BakedQuadHelper.getXYZ(geo.getVertices(), 3);

        double worldW = p0.distanceTo(p3);
        double worldH = p0.distanceTo(p1);

        double pixelsW = Math.round(worldW * 16.0);
        double pixelsH = Math.round(worldH * 16.0);

        double splitPixelW = Math.floor(pixelsW / 2.0);
        double splitPixelH = Math.floor(pixelsH / 2.0);
        double splitW = splitPixelW / 16.0;
        double splitH = splitPixelH / 16.0;
        double remainW = worldW - splitW;
        double remainH = worldH - splitH;
        double ratioW = (worldW > 1e-6) ? splitW / worldW : 0.5;
        double ratioH = (worldH > 1e-6) ? splitH / worldH : 0.5;

        // BL
        quadrants.add(createQuadrantScaled(geo, material, 0.0, 0.0, ratioW, ratioH, 0.0, 0.0, splitW, splitH));
        // BR
        quadrants.add(createQuadrantScaled(geo, material, ratioW, 0.0, 1.0, ratioH, 1.0 - remainW, 0.0, 1.0, splitH));
        // TL
        quadrants.add(createQuadrantScaled(geo, material, 0.0, ratioH, ratioW, 1.0, 0.0, 1.0 - remainH, splitW, 1.0));
        // TR
        quadrants.add(createQuadrantScaled(geo, material, ratioW, ratioH, 1.0, 1.0, 1.0 - remainW, 1.0 - remainH, 1.0, 1.0));
        return quadrants;
    }

    private static BakedQuad createQuadrantScaled(BakedQuad geo, BakedQuad material, double x0, double y0, double x1, double y1, double u0, double v0, double u1, double v1) {
        int[] gv = geo.getVertices();

        Vec3 newP0 = lerp2D(BakedQuadHelper.getXYZ(gv, 0), BakedQuadHelper.getXYZ(gv, 1), BakedQuadHelper.getXYZ(gv, 2), BakedQuadHelper.getXYZ(gv, 3), x0, y0);
        Vec3 newP1 = lerp2D(BakedQuadHelper.getXYZ(gv, 0), BakedQuadHelper.getXYZ(gv, 1), BakedQuadHelper.getXYZ(gv, 2), BakedQuadHelper.getXYZ(gv, 3), x0, y1);
        Vec3 newP2 = lerp2D(BakedQuadHelper.getXYZ(gv, 0), BakedQuadHelper.getXYZ(gv, 1), BakedQuadHelper.getXYZ(gv, 2), BakedQuadHelper.getXYZ(gv, 3), x1, y1);
        Vec3 newP3 = lerp2D(BakedQuadHelper.getXYZ(gv, 0), BakedQuadHelper.getXYZ(gv, 1), BakedQuadHelper.getXYZ(gv, 2), BakedQuadHelper.getXYZ(gv, 3), x1, y0);

        return remapQuadWithPreciseCrop(material, newP0, newP1, newP2, newP3, u0, v0, u1, v1);
    }

    public static BakedQuad remapQuadWithPreciseCrop(BakedQuad material, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double u0, double v0, double u1, double v1) {
        BakedQuad copy = BakedQuadHelper.clone(material);
        int[] v = copy.getVertices();
        TextureAtlasSprite sprite = copy.getSprite();

        BakedQuadHelper.setXYZ(v, 0, p0);
        BakedQuadHelper.setXYZ(v, 1, p1);
        BakedQuadHelper.setXYZ(v, 2, p2);
        BakedQuadHelper.setXYZ(v, 3, p3);

        double originU = SpriteShiftEntry.getUnInterpolatedU(sprite, BakedQuadHelper.getU(material.getVertices(), 0));
        double originV = SpriteShiftEntry.getUnInterpolatedV(sprite, BakedQuadHelper.getV(material.getVertices(), 0));

        double uVecX = SpriteShiftEntry.getUnInterpolatedU(sprite, BakedQuadHelper.getU(material.getVertices(), 3)) - originU;
        double uVecY = SpriteShiftEntry.getUnInterpolatedV(sprite, BakedQuadHelper.getV(material.getVertices(), 3)) - originV;
        double vVecX = SpriteShiftEntry.getUnInterpolatedU(sprite, BakedQuadHelper.getU(material.getVertices(), 1)) - originU;
        double vVecY = SpriteShiftEntry.getUnInterpolatedV(sprite, BakedQuadHelper.getV(material.getVertices(), 1)) - originV;

        double uvEps = 0.01;
        double[][] safeCorners = {
                {u0 + uvEps, v0 + uvEps}, // BL
                {u0 + uvEps, v1 - uvEps}, // TL
                {u1 - uvEps, v1 - uvEps}, // TR
                {u1 - uvEps, v0 + uvEps}  // BR
        };

        for (int i = 0; i < 4; i++) {
            double tu = safeCorners[i][0];
            double tv = safeCorners[i][1];

            double finalU = originU + (tu * uVecX) + (tv * vVecX);
            double finalV = originV + (tu * uVecY) + (tv * vVecY);

            BakedQuadHelper.setU(v, i, sprite.getU((float) finalU));
            BakedQuadHelper.setV(v, i, sprite.getV((float) finalV));
        }

        return copy;
    }

    private static Vec3 lerp2D(Vec3 p00, Vec3 p01, Vec3 p11, Vec3 p10, double x, double y) {
        Vec3 bottom = p00.lerp(p10, x);
        Vec3 top = p01.lerp(p11, x);
        return bottom.lerp(top, y);
    }
}