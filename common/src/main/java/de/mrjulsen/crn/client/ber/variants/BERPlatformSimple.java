package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.base.BERText;
import de.mrjulsen.crn.client.ber.base.BERText.TextTransformation;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.BlockEntityRendererContext;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.EUpdateReason;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformSimple implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private static final String keyTrainDeparture = "gui.createrailwaysnavigator.route_overview.notification.journey_begins";
    private static final String keyTrainDepartureWithPlatform = "gui.createrailwaysnavigator.route_overview.notification.journey_begins_with_platform";
    private static final String keyTime = "gui.createrailwaysnavigator.time";

    private Collection<UUID> lastTrainOrder = new ArrayList<>();

    @Override
    public boolean isSingleLined() {
        return true;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent) {
        
    }
    
    @Override
    public void renderAdditional(BlockEntityRendererContext context, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay, Boolean backSide) {
        
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        Collection<SimpleDeparturePrediction> preds = blockEntity.getPredictions().stream().filter(x -> x.departureTicks() < ModClientConfig.DISPLAY_LEAD_TIME.get()).toList();
        Collection<UUID> uuidOrder = preds.stream().map(x -> x.trainId()).toList();

        if (reason == EUpdateReason.DATA_CHANGED && lastTrainOrder.equals(uuidOrder)) {
            return;
        }

        lastTrainOrder = uuidOrder;
        parent.labels.clear();

        int displayWidth = blockEntity.getXSizeScaled();
        float maxWidth = displayWidth * 16 - 6;        
        parent.labels.add(new BERText(parent.getFontUtils(), () -> {
            List<Component> texts = new ArrayList<>();
            texts.add(ELanguage.translate(keyTime, TimeUtils.parseTime((int)(blockEntity.getLevel().getDayTime() % DragonLib.TICKS_PER_DAY + DragonLib.DAYTIME_SHIFT), ModClientConfig.TIME_FORMAT.get())));
            texts.addAll(preds.stream().map(x -> {
                String timeString;
                switch (blockEntity.getTimeDisplay()) {
                    case ETA:
                        timeString = ModUtils.timeRemainingString(x.departureTicks());
                        break;
                    default:
                        timeString = TimeUtils.parseTime((int)(blockEntity.getLastRefreshedTime() % DragonLib.TICKS_PER_DAY + DragonLib.DAYTIME_SHIFT + x.departureTicks()), ModClientConfig.TIME_FORMAT.get());
                        break;
                }

                if (x.stationInfo().platform() == null || x.stationInfo().platform().isBlank()) {
                    return ELanguage.translate(keyTrainDeparture, x.trainName(), x.scheduleTitle(), timeString);
                }
                return ELanguage.translate(keyTrainDepartureWithPlatform, x.trainName(), x.scheduleTitle(), timeString, x.stationInfo().platform());
            }).toList());
            
            return List.of(TextUtils.concatWithStarChars(texts.toArray(Component[]::new)));
        }, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.75f, 0.75f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withTicksPerPage(100)
            .withRefreshRate(16)
            .withPredefinedTextTransformation(new TextTransformation(3, 5.5f, 0.0f, 1, 0.75f))
            .build()
        );
    }
}
