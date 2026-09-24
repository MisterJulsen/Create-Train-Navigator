package de.mrjulsen.crn.client.gui.widgets;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.windows.TrainJourneyWindow;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.api.core.snapshot.BoardEntry;
import de.mrjulsen.crn.api.core.CallDirection;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class StationDeparturesWidget extends DLButton {    

    public static final int HEADER_HEIGHT = 20;
    public static final int DEFAULT_LINE_HEIGHT = 12;
    public static final float DEFAULT_SCALE = 0.75f;


    private final MutableComponent connectionInPast = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.route_entry.connection_in_past");
    private final MutableComponent trainCanceled = CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.stop_cancelled");
    
    private final BoardEntry entry;
    private final CallDirection direction;

    public StationDeparturesWidget(StationDeparturesViewer viewer, BoardEntry entry, CallDirection direction) {
        super(0, 0, 100, 32);
        this.entry = entry;
        this.direction = direction;

        this.cursor.set(CursorType.HAND);

        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new TrainJourneyWindow(mgr, entry.trainId()));
            return false;
        });
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, width(), height(), ColorShade.DARK.getColor());

        if (isSelected()) {
            GuiUtils.fill(graphics, 0, 0, width(), height(), DLColor.fromInt(0x22FFFFFF));
        }

        boolean arrival = direction.isArrival();

        final float scale = 0.75f;
        Component trainName = TextUtils.text(entry.displayName(direction)).withStyle(ChatFormatting.BOLD);
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, scale);
        if (arrival) {
            AllIcons.I_CONFIG_OPEN.render(graphics.graphics(), 8, 5);
        } else {
            AllIcons.I_CONFIG_BACK.render(graphics.graphics(), 8, 5);
        }

        if (entry.isCancelled()) {
            GuiUtils.drawString(graphics, graphics.defaultFont(), (int)((width() - 5) / scale), (int)(15 / scale), trainCanceled, Constants.COLOR_DELAYED, ETextAlignment.RIGHT, false);
        } else if (entry.realtimeTime(direction) < ModUtils.getTransformedWorldTime()) {
            GuiUtils.drawString(graphics, graphics.defaultFont(), (int)((width() - 5) / scale), (int)(15 / scale), connectionInPast, Constants.COLOR_DELAYED, ETextAlignment.RIGHT, false);
        }

        CreateDynamicWidgets.renderTextHighlighted(graphics, 30, 6, graphics.defaultFont(), trainName, entry.displayColor(direction));
        graphics.poseStack().popPose();

        Component platformText = TextUtils.text(entry.station().platform());
        final int maxStationNameWidth = width() - 6 - (int)((45 + graphics.defaultFont().width(trainName)) * scale);
        MutableComponent stationText = arrival
            ? TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", entry.origin().displayName())
            : TextUtils.text(entry.destinationText());
        if (graphics.defaultFont().width(stationText) > maxStationNameWidth) {
            stationText = TextUtils.text(graphics.defaultFont().substrByWidth(stationText, maxStationNameWidth).getString()).append(TextUtils.text("...")).withStyle(stationText.getStyle());
        }

        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)((45 + graphics.defaultFont().width(trainName)) * scale), 6, stationText, DLColor.WHITE, ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), width() - 6, 20, platformText, DLColor.WHITE, ETextAlignment.RIGHT, false);

        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)(30 * scale), 20, formatTime(entry.scheduledTime(direction)), DLColor.WHITE, ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)(30 * scale) + 40, 20, formatTime(entry.realtimeTime(direction)), entry.isDelayed(direction) ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, ETextAlignment.LEFT, false);
    }

    private static String formatTime(long time) {
        return new DLTime(time, VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME, DLTime.defaultTimeSystem());
    }
}
