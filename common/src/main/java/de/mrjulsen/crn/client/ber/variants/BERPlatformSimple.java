package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayScrollingTextSettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.BoundsHitReaction;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformSimple implements AbstractAdvancedDisplayRenderer<PlatformDisplayScrollingTextSettings> {

    private static final String keyTrainDeparture = "gui.createrailwaysnavigator.route_overview.notification.journey_begins";
    private static final String keyTrainDepartureWithPlatform = "gui.createrailwaysnavigator.route_overview.notification.journey_begins_with_platform";
    private static final String keyTime = "gui.createrailwaysnavigator.time";

    private final BERLabel label = new BERLabel()
        .setPos(3, 5.5f)
        .setYScale(0.75f)
        .setScale(0.75f, 0.75f)
        .setCentered(true)
        .setScrollingSpeed(2)
    ;
    private List<Component> texts;
    boolean updateLabel = false;
    


    @Override
    public void renderTick(float deltaTime) {
        label.renderTick();
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        List<Component> textContent = new ArrayList<>(texts);
        textContent.add(0, CustomLanguage.translate(keyTime, TimeUtils.parseTime((int)(blockEntity.getLevel().getDayTime() % DragonLib.ticksPerDay() + DragonLib.daytimeShift()), ModClientConfig.TIME_FORMAT.get())));
        MutableComponent txt = TextUtils.concat(textContent);
        label
            .setText(txt)
        ;
    }
    
    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        label.render(graphics, light);
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        List<StationDisplayData> preds = blockEntity.getStops().stream().filter(x -> x.getStationData().getRealTimeArrivalTime() < DragonLib.getCurrentWorldTime() + ModClientConfig.DISPLAY_LEAD_TIME.get() && (!x.getTrainData().isCancelled() || DragonLib.getCurrentWorldTime() < x.getStationData().getScheduledDepartureTime() + ModClientConfig.DISPLAY_LEAD_TIME.get())).toList();
        
        label
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6, BoundsHitReaction.SCALE_SCROLL)
        ;

        texts = new ArrayList<>();
        texts.addAll(preds.stream().filter(x -> {
            return !x.isNextSectionExcluded();
        }).map(x -> {
            String timeString = ModUtils.formatTime(x.getStationData().getScheduledDepartureTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA);
            MutableComponent text = TextUtils.empty();
            if (x.getStationData().getStationInfo().platform() == null || x.getStationData().getStationInfo().platform().isBlank()) {
                text.append(CustomLanguage.translate(keyTrainDeparture, x.getTrainData().getName(), x.getStationData().getDestination(), timeString));
            } else {
                text.append(CustomLanguage.translate(keyTrainDepartureWithPlatform, x.getTrainData().getName(), x.getStationData().getDestination(), timeString, x.getStationData().getStationInfo().platform()));
            }

            if (x.getTrainData().isCancelled()) {
                text.append(", ").append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.cancelled2").getString());
            } else if (x.getStationData().isDepartureDelayed()) {
                String delay = getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA ? ModUtils.timeRemainingString(x.getStationData().getDepartureTimeDeviation()) : String.valueOf(TimeUtils.formatToMinutes(x.getStationData().getDepartureTimeDeviation()));
                String timeUnitSuffix = getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ABS ?
                    " " + CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delay_abs_suffix").getString() :
                    "";

                text.append(", ").append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delayed2", delay, timeUnitSuffix).getString());
                
                if (x.getTrainData().hasStatusInfo()) {
                    text.append(" ").append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.reason").getString()).append(x.getTrainData().getStatus().get(0).text());
                }
            }
            return text;
        }).toList());
    }
}
