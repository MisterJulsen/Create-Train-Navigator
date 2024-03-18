package de.mrjulsen.crn.client.ber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ber.base.AbstractBlockEntityRenderInstance;
import de.mrjulsen.crn.client.ber.base.BERText;
import de.mrjulsen.crn.client.ber.variants.BERPassengerInfoDetailed;
import de.mrjulsen.crn.client.ber.variants.BERPassengerInfoInformative;
import de.mrjulsen.crn.client.ber.variants.BERPassengerInfoSimple;
import de.mrjulsen.crn.client.ber.variants.BERRenderSubtypeBase;
import de.mrjulsen.crn.client.ber.variants.BERTrainDestinationDetailed;
import de.mrjulsen.crn.client.ber.variants.BERTrainDestinationInformative;
import de.mrjulsen.crn.client.ber.variants.BERTrainDestinationSimple;
import de.mrjulsen.crn.client.ber.variants.IBERRenderSubtype;
import de.mrjulsen.crn.data.EDisplayInfo;
import de.mrjulsen.crn.data.EDisplayType;
import de.mrjulsen.crn.util.Tripple;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplayRenderInstance extends AbstractBlockEntityRenderInstance<AdvancedDisplayBlockEntity> {

    private Map<EDisplayType, Map<EDisplayInfo, Supplier<IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance>>>> renderSubtypes;

    public Collection<BERText> labels;
    public BERText carriageIndexLabel;
    private IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance> renderSubtype;

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
            )
        );
    }

    public MutableComponent getStopoversString(AdvancedDisplayBlockEntity blockEntity) {
        MutableComponent line = Utils.text("");
        for (int i = 0; i < blockEntity.getTrainData().stopovers().size(); i++) {
            if (i > 0) {
                line = line.append(Utils.text(" ● "));
            }
            line = line.append(Utils.text(blockEntity.getTrainData().stopovers().get(i).stationName()));
        }
        return line;
    }

    @Override
    public void render(BlockEntityRendererContext context, AdvancedDisplayBlockEntity pBlockEntity, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay) {
        if (!pBlockEntity.isController()) {
            return;
        }
        pPoseStack.pushPose();
        Tripple<Float, Float, Float> offset = pBlockEntity.renderOffset.get();
        float scale = pBlockEntity.renderScale.get();
        pPoseStack.translate(offset.getFirst(), offset.getSecond(), offset.getThird());
        pPoseStack.scale(scale, scale, 1);
        labels.forEach(x -> x.render(pPoseStack, pBufferSource, pBlockEntity.isGlowing() ? LightTexture.FULL_BRIGHT : pPackedLight));

        renderSubtype.renderAdditional(context, pBlockEntity, this, pPartialTicks, pPoseStack, pBufferSource, pBlockEntity.isGlowing() ? LightTexture.FULL_BRIGHT : pPackedLight, pOverlay);
        pPoseStack.popPose();
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity) {
        renderSubtype.tick(level, pos, state, blockEntity, this);
        labels.forEach(x -> x.tick());

        if (blockEntity.getXSizeScaled() != lastXSize) {
            update(level, pos, state, blockEntity);
        }
        lastXSize = blockEntity.getXSizeScaled();
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity) {
        carriageIndexLabel = null;
        EDisplayType type = blockEntity.getDisplayType();
        EDisplayInfo info = blockEntity.getInfoType();

        if (lastType != type || lastInfo != info) {
            if (renderSubtypes.containsKey(type)) {
                Map<EDisplayInfo, Supplier<IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance>>> selectedType = renderSubtypes.get(type);
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

        renderSubtype.update(level, pos, state, blockEntity, this);
    }
}
