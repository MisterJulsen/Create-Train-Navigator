package de.mrjulsen.crn.client.ber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ber.base.BERText;
import de.mrjulsen.crn.client.ber.variants.BERPassengerInfoDetailed;
import de.mrjulsen.crn.client.ber.variants.BERPassengerInfoInformative;
import de.mrjulsen.crn.client.ber.variants.BERPassengerInfoSimple;
import de.mrjulsen.crn.client.ber.variants.BERPlatformDetailed;
import de.mrjulsen.crn.client.ber.variants.BERPlatformInformative;
import de.mrjulsen.crn.client.ber.variants.BERPlatformSimple;
import de.mrjulsen.crn.client.ber.variants.BERRenderSubtypeBase;
import de.mrjulsen.crn.client.ber.variants.BERTrainDestinationDetailed;
import de.mrjulsen.crn.client.ber.variants.BERTrainDestinationInformative;
import de.mrjulsen.crn.client.ber.variants.BERTrainDestinationSimple;
import de.mrjulsen.crn.client.ber.variants.IBERRenderSubtype;
import de.mrjulsen.crn.data.EDisplayInfo;
import de.mrjulsen.crn.data.EDisplayType;
import de.mrjulsen.crn.data.ESide;
import de.mrjulsen.crn.data.EDisplayType.EDisplayTypeDataSource;
import de.mrjulsen.mcdragonlib.client.ber.AbstractBlockEntityRenderInstance;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplayRenderInstance extends AbstractBlockEntityRenderInstance<AdvancedDisplayBlockEntity> {

    private Map<EDisplayType, Map<EDisplayInfo, Supplier<IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean>>>> renderSubtypes;

    public Collection<BERText> labels;
    public BERText carriageIndexLabel;
    public IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> renderSubtype;

    private int lastXSize = 0;
    private EDisplayType lastType;
    private EDisplayInfo lastInfo;

    public AdvancedDisplayRenderInstance(AdvancedDisplayBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    protected void preinit(AdvancedDisplayBlockEntity blockEntity) {
        this.labels = new ArrayList<>();
        this.renderSubtypes = Map.of(
            EDisplayType.TRAIN_DESTINATION, Map.of(
                EDisplayInfo.SIMPLE, () -> new BERTrainDestinationSimple(),
                EDisplayInfo.DETAILED, () -> new BERTrainDestinationDetailed(),
                EDisplayInfo.INFORMATIVE, () -> new BERTrainDestinationInformative()
            ),
            EDisplayType.PASSENGER_INFORMATION, Map.of(
                EDisplayInfo.SIMPLE, () -> new BERPassengerInfoSimple(),
                EDisplayInfo.DETAILED, () -> new BERPassengerInfoDetailed(),
                EDisplayInfo.INFORMATIVE, () -> new BERPassengerInfoInformative()
            ),
            EDisplayType.PLATFORM, Map.of(
                EDisplayInfo.SIMPLE, () -> new BERPlatformSimple(),
                EDisplayInfo.DETAILED, () -> new BERPlatformDetailed(),
                EDisplayInfo.INFORMATIVE, () -> new BERPlatformInformative() 
            )
        );
    }

    public MutableComponent getStopoversString(AdvancedDisplayBlockEntity blockEntity) {
        MutableComponent line = TextUtils.empty();

        List<String> stopovers = blockEntity.getDisplayType().getSource() == EDisplayTypeDataSource.TRAIN_INFORMATION ?
            blockEntity.getTrainData().stopovers().stream().map(x -> x.stationTagName()).toList() :
            blockEntity.getNextDepartureStopovers();

        Iterator<String> i = stopovers.iterator();
        boolean isFirst = true;
        while (i.hasNext()) {
            if (!isFirst) {
                line = line.append(TextUtils.text(" ● "));
            }
            line = line.append(TextUtils.text(i.next()));
            isFirst = false;
        }
        return line;
    }

    @Override
    public void render(BlockEntityRendererContext context, AdvancedDisplayBlockEntity pBlockEntity, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay) {
        
        if (!pBlockEntity.isController()) {
            return;
        }
        
        final int light = pBlockEntity.isGlowing() ? LightTexture.FULL_BRIGHT : pPackedLight;

        if (pBlockEntity.getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock) {
            
            Tripple<Float, Float, Float> rotation = pBlockEntity.renderRotation.get();
            Pair<Float, Float> offset = pBlockEntity.renderOffset.get();
            Pair<Float, Float> zOffset = pBlockEntity.renderZOffset.get();
            float scale = pBlockEntity.renderScale.get();

            pPoseStack.pushPose();
            pPoseStack.translate(offset.getFirst(), offset.getSecond(), zOffset.getFirst());
            pPoseStack.mulPose(Axis.XP.rotationDegrees(rotation.getFirst()));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(rotation.getSecond()));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(rotation.getThird()));
            pPoseStack.scale(scale, scale, 1);   
            renderSubtype.renderAdditional(context, pBlockEntity, this, pPartialTicks, pPoseStack, pBufferSource, light, pOverlay, false);
            labels.forEach(x -> x.render(pPoseStack, pBufferSource, light)); 
            pPoseStack.popPose();

            if (!(pBlockEntity.getBlockState().getBlock() instanceof AbstractAdvancedSidedDisplayBlock) || pBlockEntity.getBlockState().getValue(AbstractAdvancedSidedDisplayBlock.SIDE) == ESide.BOTH) {
                pPoseStack.pushPose();
                pPoseStack.mulPose(Axis.YP.rotationDegrees(180));
                pPoseStack.translate(-pBlockEntity.getXSize() * 16, 0, -16);
                pPoseStack.translate(offset.getFirst(), offset.getSecond(), zOffset.getSecond());
                pPoseStack.mulPose(Axis.XP.rotationDegrees(rotation.getFirst()));
                pPoseStack.mulPose(Axis.YP.rotationDegrees(rotation.getSecond()));
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(rotation.getThird()));
                pPoseStack.scale(scale, scale, 1);
                renderSubtype.renderAdditional(context, pBlockEntity, this, pPartialTicks, pPoseStack, pBufferSource, light, pOverlay, true);
                labels.forEach(x -> x.render(pPoseStack, pBufferSource, light));
                pPoseStack.popPose();
            }
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity) {
        renderSubtype.tick(level, pos, state, blockEntity, this);
        labels.forEach(x -> x.tick());

        if (blockEntity.getXSizeScaled() != lastXSize) {
            update(level, pos, state, blockEntity, EUpdateReason.BLOCK_CHANGED);
        }
        lastXSize = blockEntity.getXSizeScaled();
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, EUpdateReason reason) {
        carriageIndexLabel = null;
        EDisplayType type = blockEntity.getDisplayType();
        EDisplayInfo info = blockEntity.getInfoType();

        if (lastType != type || lastInfo != info) {
            if (renderSubtypes.containsKey(type)) {
                Map<EDisplayInfo, Supplier<IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean>>> selectedType = renderSubtypes.get(type);
                if (selectedType.containsKey(info)) {
                    renderSubtype = selectedType.get(info).get();
                }
            }
    
            if (renderSubtype == null) {
                renderSubtype = new BERRenderSubtypeBase<>();
            }
        }

        lastType = type;
        lastInfo = info;

        renderSubtype.update(level, pos, state, blockEntity, this, reason);
    }
}
