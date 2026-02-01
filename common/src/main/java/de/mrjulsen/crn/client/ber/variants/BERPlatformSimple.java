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
import de.mrjulsen.crn.data.train.ETrainStopState;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformSimple implements AbstractAdvancedDisplayRenderer<PlatformDisplayScrollingTextSettings> {

    private static final String keyTrainDeparture = "gui.createrailwaysnavigator.route_overview.notification.journey_begins";
    private static final String keyTrainDepartureWithPlatform = "gui.createrailwaysnavigator.route_overview.notification.journey_begins_with_platform";
    private static final String keyTime = "gui.createrailwaysnavigator.time";

    private final BERLabel label = new BERLabel();
    private List<Component> texts;
    boolean updateLabel = false;
    

    public BERPlatformSimple() {
        label.position.set(Point.of(3, 5.5f));
        label.verticalScale.set(Pair.of(0.75f, 0.75f));
        label.horizontalScale.set(Pair.of(0.75f, 0.75f));
        label.horizontalAlign.set(ETextAlignment.CENTER);
        label.horizontalScrollingSpeed.set(SCROLLING_SPEED);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        List<Component> textContent = new ArrayList<>(texts);
        if (blockEntity.getXSize() > 2) {
            textContent.add(0, CustomLanguage.translate(keyTime, DLTime.fromLevelTime(blockEntity.getLevel(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)));
        } else {
            textContent.add(0, TextUtils.text(DLTime.fromLevelTime(blockEntity.getLevel(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)));
        }
        MutableComponent txt = TextUtils.concat(textContent);
        label.text.set(txt);
    }
    
    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        label.render(graphics, light);
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        List<StationDisplayData> preds = blockEntity.getStops().stream().filter(x -> x.getStationData().getRealTimeArrivalTime() < DragonLib.getCurrentWorldTime() + ModClientConfig.DISPLAY_LEAD_TIME.get() && (!x.getTrainData().isCancelled() || DragonLib.getCurrentWorldTime() < x.getStationData().getScheduledDepartureTime() + ModClientConfig.DISPLAY_LEAD_TIME.get())).toList();

        label.clippingArea.set(Rectangle.withSize(3, 3, blockEntity.getXSizeScaled() * 16 - 6, blockEntity.getYSizeScaled() * 16 - 6));   
        label.glowing.set(blockEntity.isGlowing());
        label.color.set(getDisplaySettings(blockEntity).getFontColor());
        label.preferredWidth.set((float)(blockEntity.getXSizeScaled() * 16 - 6));
        label.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        texts = new ArrayList<>();
        texts.addAll(preds.stream().filter(x -> {
            return !x.isNextSectionExcluded();
        }).map(x -> {
            String timeString = ModUtils.formatTime(x.getStationData().getScheduledDepartureTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA);
            MutableComponent text = TextUtils.empty();
            if (x.getStationData().getRealTimeStation().info().platform() == null || x.getStationData().getRealTimeStation().info().platform().isBlank()) {
                text.append(CustomLanguage.translate(keyTrainDeparture, x.getTrainData().getName(ETrainStopState.DEPARTURE), x.getStationData().getDestination(), timeString));
            } else {
                text.append(CustomLanguage.translate(keyTrainDepartureWithPlatform, x.getTrainData().getName(ETrainStopState.DEPARTURE), x.getStationData().getDestination(), timeString, x.getStationData().getRealTimeStation().info().platform()));
            }

            if (x.getTrainData().isCancelled()) {
                text.append(", ").append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.cancelled2").getString());
            } else if (x.getStationData().isDepartureDelayed()) {
                String delay = getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA ? ModUtils.timeRemainingString(x.getStationData().getDepartureTimeDeviation()) : String.valueOf((long)DLTime.fromTicks(x.getStationData().getDepartureTimeDeviation(), new ConfiguredTimeSystem()).toGameMinutes());
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
