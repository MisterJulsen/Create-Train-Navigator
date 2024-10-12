package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource.ETimeDisplay;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.BoundsHitReaction;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformDetailed implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private static final String keyTime = "gui.createrailwaysnavigator.time";

    private static final float LINE_HEIGHT = 5.4f;

    private boolean showInfoLine = false;
    private MutableComponent infoLineText = TextUtils.empty();
    private int maxLines = 0;
    
    private final BERLabel timeLabel = new BERLabel(TextUtils.empty())
        .setCentered(true)
        .setScale(0.4f, 0.4f)
        .setYScale(0.4f);
    private final BERLabel statusLabel = new BERLabel(TextUtils.empty())
        .setCentered(true)
        .setScale(0.4f, 0.4f)
        .setYScale(0.4f)
        .setColor(0xFF111111)
        .setBackground(0xFFFFFFFF, true)
        .setScrollingSpeed(2);
    private BERLabel[][] lines = new BERLabel[0][];


    @Override
    public void renderTick(float deltaTime) {
        timeLabel.renderTick();
        statusLabel.renderTick();
        DLUtils.doIfNotNull(lines, x -> {
            for (int i = 0; i < x.length; i++) {
                BERLabel[] line = x[i];
                if (line == null) continue;
                for (int k = 0; k < line.length; k++) {                    
                    DLUtils.doIfNotNull(line[k], y -> y.renderTick());
                }
            }
        });
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        timeLabel
            .setText(ELanguage.translate(keyTime, ModUtils.formatTime(DragonLib.getCurrentWorldTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
        ;
    }
    
    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        for (int i = 0; i < lines.length && i < maxLines; i++) {
            for (int k = 0; k < lines[i].length; k++) {
                if (i >= maxLines - 1 && (DragonLib.getCurrentWorldTime() % 200 > 100)) {
                    timeLabel.render(graphics, light);
                    continue;
                }
                lines[i][k].render(graphics, light);
            }
        }

        if (lines.length < maxLines) {            
            timeLabel.render(graphics, light);
        }

        if (showInfoLine) {
            statusLabel.render(graphics, light);
        }
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        List<StationDisplayData> preds = blockEntity.getStops().stream().filter(x -> x.getStationData().getScheduledArrivalTime() < DragonLib.getCurrentWorldTime() + ModClientConfig.DISPLAY_LEAD_TIME.get() && (!x.getTrainData().isCancelled() || DragonLib.getCurrentWorldTime() < x.getStationData().getScheduledDepartureTime() + ModClientConfig.DISPLAY_LEAD_TIME.get())).toList();
        
        showInfoLine = !preds.isEmpty() && preds.get(0).getStationData().isDepartureDelayed() && preds.get(0).getTrainData().hasStatusInfo();
        if (showInfoLine) {
            // Update status label
            this.infoLineText = TextUtils.concat(TextUtils.text("  +++  "), preds.stream().limit(maxLines).filter(x -> x.getTrainData().hasStatusInfo() && x.getStationData().isDepartureDelayed()).flatMap(x -> {
                Collection<Component> content = new ArrayList<>();
                if (x.getTrainData().isCancelled()) {
                    content.add(ELanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.information_about_cancelled", x.getTrainData().getName()));
                    return content.stream();
                }
                content.add(ELanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.information_about_delayed", x.getTrainData().getName(), TimeUtils.formatToMinutes(x.getStationData().getDepartureTimeDeviation())));
                for (CompiledTrainStatus status : x.getTrainData().getStatus()) {
                    content.add(status.text());
                }
                return content.stream();
            }).toArray(Component[]::new));
        } else {
            infoLineText = TextUtils.empty();
        }

        int defaultMaxLines = blockEntity.getYSizeScaled() * 3 - 1;
        this.maxLines = defaultMaxLines - (showInfoLine ? 1 : 0);        
        int maxIndices = Math.max(0, Math.min(this.maxLines, preds.size()));
        if (reason == EUpdateReason.LAYOUT_CHANGED || this.lines == null || lines.length != maxIndices) {
            updateLayout(blockEntity, preds, maxIndices);
        }
            
        for (int i = 0; i < this.lines.length; i++) {
            StationDisplayData stop = preds.get(i);
            updateContent(blockEntity, stop, i);
        }

        statusLabel
            .setText(infoLineText)
            .setPos(3, blockEntity.getYSizeScaled() * 16 - 12 * statusLabel.getYScale() - 2)
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6, BoundsHitReaction.SCALE_SCROLL)
        ;
    }

    private void updateLayout(AdvancedDisplayBlockEntity blockEntity, List<StationDisplayData> preds, int maxIndices) {
        this.lines = new BERLabel[maxIndices][];
        for (int i = 0; i < this.lines.length; i++) {
            StationDisplayData stop = preds.get(i);
            this.lines[i] = createLine(blockEntity, stop, i);
            updateContent(blockEntity, stop, i);
        }
        statusLabel
            .setBackground((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF), false)
        ;
        timeLabel
            .setPos(3, 3 + (Math.min(lines.length, maxLines) - (lines.length < maxLines ? 0 : 1)) * LINE_HEIGHT)
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6, BoundsHitReaction.CUT_OFF)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
    }

    private void updateContent(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop, int index) {
        boolean isLast = stop.isLastStop();
        BERLabel[] components = lines[index];
        components[LineComponent.TIME.i()]
            .setText(TextUtils.text(ModUtils.formatTime(stop.getScheduledTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
        ;
        components[LineComponent.REAL_TIME.i()]
            .setText(TextUtils.text(stop.getTrainData().isCancelled() ?
                " \u274C " : // X
                (stop.getStationData().isDepartureDelayed() ?
                    (ModUtils.formatTime(stop.getRealTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)) : 
                    ""))) // Nothing (not delayed)
        ;
        components[LineComponent.TRAIN_NAME.i()]
            .setText(TextUtils.text(stop.getTrainData().getName()))
        ;
        components[LineComponent.PLATFORM.i()]
            .setText(blockEntity.isPlatformFixed() ?
                TextUtils.empty() :
                TextUtils.text(stop.getStationData().getStationInfo().platform()))
        ;
        components[LineComponent.DESTINATION.i()]
            .setText(isLast ?
                ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.getFirstStopName()) :
                TextUtils.text(stop.getStationData().getDestination()))
        ;

        int x = 3;
        components[LineComponent.TIME.i()].setPos(x, 3 + index * LINE_HEIGHT);
        x += components[LineComponent.TIME.i()].getTextWidth() + 2;
        components[LineComponent.REAL_TIME.i()].setPos(x, 3 + index * LINE_HEIGHT);
        x += components[LineComponent.REAL_TIME.i()].getTextWidth() + 2 + (!components[LineComponent.REAL_TIME.i()].getText().getString().isEmpty() ? 2 : 0);
        
        BERLabel trainNameLabel = components[LineComponent.TRAIN_NAME.i()]
            .setPos(x, 3 + index * LINE_HEIGHT)
            .setMaxWidth(blockEntity.getTrainNameWidth(), BoundsHitReaction.SCALE_SCROLL)
        ;
        x += trainNameLabel.getMaxWidth() + 2;
        
        BERLabel platformLabel = components[LineComponent.PLATFORM.i()];
        float platformWidth = platformLabel.getTextWidth();
        platformLabel.setPos(blockEntity.getXSizeScaled() * 16 - 3 - platformWidth, 3 + index * LINE_HEIGHT);
        components[LineComponent.DESTINATION.i()].setPos(x, 3 + index * LINE_HEIGHT);
        components[LineComponent.DESTINATION.i()].setMaxWidth(blockEntity.getXSizeScaled() * 16 - 3 - x - platformWidth - 3, BoundsHitReaction.SCALE_SCROLL);
    }    

    private BERLabel[] createLine(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop, int index) {
        BERLabel[] components = new BERLabel[5];

        components[LineComponent.TIME.i()] = new BERLabel()
            .setYScale(0.4f)
            .setMaxWidth(12, BoundsHitReaction.SCALE_SCROLL)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        components[LineComponent.REAL_TIME.i()] = new BERLabel()
            .setYScale(0.4f)
            .setMaxWidth(12, BoundsHitReaction.SCALE_SCROLL)
            .setScale(0.4f, 0.2f)
            .setBackground((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF), false)
            .setColor(0xFF111111)
        ;
        components[LineComponent.TRAIN_NAME.i()] = new BERLabel()
            .setYScale(0.4f)
            .setScrollingSpeed(2)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        
        components[LineComponent.PLATFORM.i()] = new BERLabel()
            .setYScale(0.4f)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;

        components[LineComponent.DESTINATION.i()] = new BERLabel()
            .setYScale(0.4f)
            .setScrollingSpeed(2)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;

        return components;
    }

    private static enum LineComponent {
        TIME(0),
        REAL_TIME(1),
        TRAIN_NAME(2),
        DESTINATION(3),
        PLATFORM(4);

        int index;
        LineComponent(int index) {
            this.index = index;
        }
        public int i() {
            return index;
        }
    }
}
