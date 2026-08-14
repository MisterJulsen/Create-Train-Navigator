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
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.api.core.snapshot.BoardEntry;
import de.mrjulsen.crn.api.core.CallDirection;
import de.mrjulsen.crn.block.display.properties.components.ITrainStopTypeSetting;
import de.mrjulsen.crn.block.display.properties.components.ITrainStopTypeSetting.ETrainStopType;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
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
        String formattedTime = new DLTime(blockEntity.getLevel(), DLTime.defaultTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME, DLTime.defaultTimeSystem());
        if (blockEntity.getXSize() > 2) {
            textContent.add(0, CustomLanguage.translate(keyTime, formattedTime));
        } else {
            textContent.add(0, TextUtils.text(formattedTime));
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
        long now = ModUtils.getTransformedWorldTime();
        List<BoardEntry> preds = blockEntity.getStops().stream()
            .filter(x -> x.realtime().arrival() < now + ModCommonConfig.DISPLAY_LEAD_TIME.get())
            .filter(x -> ITrainStopTypeSetting.accepts(x, ETrainStopType.DEPARTURES_ONLY, now))
            .toList();

        label.clippingArea.set(Rectangle.withSize(3, 3, blockEntity.getXSizeScaled() * 16 - 6, blockEntity.getYSizeScaled() * 16 - 6));
        label.glowing.set(blockEntity.isGlowing());
        label.color.set(getDisplaySettings(blockEntity).getFontColor());
        label.preferredWidth.set((float)(blockEntity.getXSizeScaled() * 16 - 6));
        label.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        texts = new ArrayList<>();
        texts.addAll(preds.stream().map(x -> {
            String timeString = ModUtils.formatTime(x.scheduled().departure(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA);
            String platform = x.station().platform();
            MutableComponent text = TextUtils.empty();
            if (platform == null || platform.isBlank()) {
                text.append(CustomLanguage.translate(keyTrainDeparture, x.displayName(CallDirection.DEPARTURE), x.destinationText(), timeString));
            } else {
                text.append(CustomLanguage.translate(keyTrainDepartureWithPlatform, x.displayName(CallDirection.DEPARTURE), x.destinationText(), timeString, platform));
            }

            if (x.isCancelled()) {
                text.append(", ").append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.cancelled2").getString());
            } else if (x.isDelayed(CallDirection.DEPARTURE)) {
                String delay = getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA ? ModUtils.timeRemainingString(x.departureDeviation()) : String.valueOf((long)DLTime.fromGameTicks(x.departureDeviation(), DLTime.defaultTimeSystem()).toGameMinutes(DLTime.defaultTimeSystem()));
                String timeUnitSuffix = getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ABS ?
                    " " + CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delay_abs_suffix").getString() :
                    "";

                text.append(", ").append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delayed2", delay, timeUnitSuffix).getString());

                x.primaryDelay().ifPresent(cause -> text
                    .append(" ")
                    .append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.reason").getString())
                    .append(CustomLanguage.translate(cause.translationKey())));
            }
            return text;
        }).toList());
    }
}
