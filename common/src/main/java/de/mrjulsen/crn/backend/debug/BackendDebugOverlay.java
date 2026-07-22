package de.mrjulsen.crn.backend.debug;

import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import org.lwjgl.glfw.GLFW;

import de.mrjulsen.crn.backend.RailwayBackend;
import de.mrjulsen.crn.backend.TrainManager;
import de.mrjulsen.crn.backend.api.BoardEntry;
import de.mrjulsen.crn.backend.api.BoardQuery;
import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.backend.core.LiveTrainState;
import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.core.TrainLifecycleState;
import de.mrjulsen.crn.backend.realtime.SignalWait;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.timing.StopTimings;
import de.mrjulsen.crn.util.ESpeedUnit;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.DLOverlayManager;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.events.EventListenerId;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Debug overlay showing everything the backend maintains for one train: identity, lifecycle, live
 * measurements, the full journey with its timetable and projected times, sections, status reasons
 * and a sample departure board.
 * <p>
 * Singleplayer only, since it reads the server-side backend directly.
 */
public class BackendDebugOverlay extends DLWindow {

    private static BackendDebugOverlay instance;
    private final EventListenerId event;

    private int trainIndex = 0;
    private int line = 0;

    public BackendDebugOverlay(DLWindowManager manager) {
        super(manager);
        fullscreen.set(true);

        event = getWindowManager().addEventListener(DLGuiStandardEvents.KeyPressEvent.class, (s, e) -> {
            if (e.keyCode() == GLFW.GLFW_KEY_K) {
                trainIndex++;
                return true;
            } else if (e.keyCode() == GLFW.GLFW_KEY_J) {
                trainIndex--;
                return true;
            }
            return false;
        });
    }

    @Override
    public void close() throws Exception {
        getWindowManager().removeEventListener(DLGuiStandardEvents.KeyPressEvent.class, event);
        super.close();
    }

    /** Opens the overlay, or closes it if it is already open. */
    public static void toggle() {
        if (instance != null) {
            DLOverlayManager.getWindowManager().ifPresent(w -> w.closeWindow(instance));
            instance = null;
        } else {
            DLOverlayManager.addOverlay(mgr -> instance = new BackendDebugOverlay(mgr));
        }
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
        line = 0;

        if (!RailwayBackend.isActive()) {
            drawLine(graphics, TextUtils.text("CRN Backend is not active.").withStyle(ChatFormatting.RED));
            graphics.poseStack().popPose();
            return;
        }

        List<TrackedTrain> trains = TrainManager.getInstance().getAllTrains().stream()
            .sorted((a, b) -> a.getTrainName().compareToIgnoreCase(b.getTrainName()))
            .toList();

        if (trains.isEmpty()) {
            drawLine(graphics, TextUtils.text("CRN Backend: no tracked trains.").withStyle(ChatFormatting.YELLOW));
            graphics.poseStack().popPose();
            return;
        }

        trainIndex = ((trainIndex % trains.size()) + trains.size()) % trains.size();
        TrackedTrain train = trains.get(trainIndex);
        long now = ModUtils.getTransformedWorldTime();

        renderHeader(graphics, train, trains.size(), now);
        renderLiveData(graphics, train);
        renderStatuses(graphics, train, now);
        renderJourney(graphics, train, now);
        renderSections(graphics, train);

        drawLine(graphics, TextUtils.empty());
        drawLine(graphics, TextUtils.text("[K] next train   [J] previous train").withStyle(ChatFormatting.AQUA));
        graphics.poseStack().popPose();
    }

    private void renderHeader(DLGuiGraphics graphics, TrackedTrain train, int trainCount, long now) {
        drawLine(graphics, TextUtils.text("CRN TRAIN TRACKING  ").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GOLD)
            .append(TextUtils.text("Train " + (trainIndex + 1) + "/" + trainCount + ", now=" + String.format("%,d", now)).withStyle(ChatFormatting.GRAY)));

        MutableComponent title = TextUtils.text(train.getTrainName()).withStyle(ChatFormatting.WHITE).withStyle(ChatFormatting.BOLD);
        if (!train.getDisplayName().equals(train.getTrainName())) {
            title.append(TextUtils.text(" (" + train.getDisplayName() + ")").withStyle(ChatFormatting.GRAY));
        }
        title.append(TextUtils.text(" [" + train.getLifecycleState() + "]").withStyle(lifecycleColor(train.getLifecycleState())));
        title.append(TextUtils.text(" [" + train.getLiveState() + "]").withStyle(liveStateColor(train.getLiveState())));
        drawLine(graphics, title);

        drawLine(graphics, TextUtils.text("Id: " + train.getTrainId() + ", Session: " + train.getSessionId()).withStyle(ChatFormatting.DARK_GRAY));

        String sectionInfo = train.getCurrentSection().map(s ->
            "Section " + (s.getSectionIndex() + 1) + "/" + train.getJourney().getSections().size() + " -> " + s.getLastStop().toString()
            + "   Line: " + s.getTrainLine().map(x -> x.getLineName()).orElse("-")
            + "   Category: " + s.getTrainCategory().map(x -> x.getCategoryName()).orElse("-")
        ).orElse("Section: -");
        drawLine(graphics, TextUtils.text(sectionInfo
            + "   Cyclic: " + yesNo(train.getJourney().isCyclic())
            + "   FlexibleDwell: " + yesNo(train.getJourney().hasFlexibleDwellTimes())
            + "   Title: " + train.getCurrentStop().map(JourneyStop::getTitle).orElse("-")
        ).withStyle(ChatFormatting.GRAY));
    }

    private void renderLiveData(DLGuiGraphics graphics, TrackedTrain train) {
        var rt = train.getRealtime();
        drawLine(graphics, TextUtils.text("Live: ")
            .append(TextUtils.text(", transit: " + rt.getTransitTicks() + "t").withStyle(ChatFormatting.RESET).withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(", dwell: " + rt.getDwellTicks() + "t").withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(", signal: " + rt.getTotalSignalWaitTicks() + "t (" + rt.getSignalWaits().size() + " waits)").withStyle(rt.getCurrentSignalId() != null ? ChatFormatting.RED : ChatFormatting.WHITE))
            .append(TextUtils.text(", stalled: " + rt.getStalledTicks() + "t").withStyle(rt.getStalledTicks() > 0 ? ChatFormatting.RED : ChatFormatting.WHITE))
            .append(TextUtils.text(", speed: " + (int)ModUtils.calcSpeed(train.getTrain().speed, ESpeedUnit.MS) + "/" + (int)ModUtils.calcSpeed(train.getTrain().targetSpeed, ESpeedUnit.MS) + " " + ESpeedUnit.MS).withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(", cycle: " + train.getTotalDuration() + "t").withStyle(ChatFormatting.WHITE))
        );

        for (SignalWait wait : rt.getSignalWaits()) {
            drawLine(graphics, TextUtils.text("   - Signal " + shortId(wait.signalId()) + ": " + wait.waitTicks() + "t"
                + (wait.occupyingTrains().isEmpty() ? "" : "  blocked by: " + String.join(", ", wait.occupyingTrains()))).withStyle(ChatFormatting.RED));
        }
    }

    private void renderStatuses(DLGuiGraphics graphics, TrackedTrain train, long now) {
        long deviation = train.getMaxDeviation();
        ChatFormatting devColor = train.isDelayed() ? ChatFormatting.RED : ChatFormatting.GREEN;
        drawLine(graphics, TextUtils.text("Delay: ").withStyle(ChatFormatting.UNDERLINE)
            .append(TextUtils.text("  max +" + deviation + "t").withStyle(ChatFormatting.RESET).withStyle(devColor))
            .append(TextUtils.text("   carried-over " + train.getDelayOffset() + "t").withStyle(ChatFormatting.GRAY))
            .append(TextUtils.text(train.isDelayed() ? "   DELAYED" : "   ON TIME").withStyle(devColor).withStyle(ChatFormatting.BOLD))
            .append(TextUtils.text(train.isCancelled() ? "   CANCELLED" : "").withStyle(ChatFormatting.DARK_RED).withStyle(ChatFormatting.BOLD))
        );

        List<DelayInstance> delays = train.getActiveDelays();
        if (delays.isEmpty()) {
            drawLine(graphics, TextUtils.text("   (no active status)").withStyle(ChatFormatting.DARK_GRAY));
        }
        for (DelayInstance delay : delays) {
            drawLine(graphics, TextUtils.text("   ! " + delay.causeId().getPath()
                + (delay.hasArgs() ? ": " + String.join(", ", delay.argValues()) : "")
                + "  (since " + (now - delay.since()) + "t)").withStyle(statusColor(delay)));
        }
    }

    private void renderJourney(DLGuiGraphics graphics, TrackedTrain train, long now) {
        List<JourneyStop> stops = train.getJourney().getStops();
        drawLine(graphics, TextUtils.text("Journey (" + stops.size() + " stops):").withStyle(ChatFormatting.UNDERLINE));
        drawLine(graphics, TextUtils.text("     idx  station                     sched arr/dep    real arr/dep     dev a/d      leg (last | hist)         visits").withStyle(ChatFormatting.DARK_GRAY));

        JourneyStop current = train.getCurrentStop().orElse(null);
        for (JourneyStop stop : stops) {
            StopTimings timing = train.getTimings(stop);
            boolean isCurrent = stop == current;

            String marker = isCurrent ? (train.getLiveState() == LiveTrainState.AT_STATION ? " ■ " : " ▶ ") : " □ ";
            String station = fit(stop.getStationName(), 26);
            String sched = "S: " + (timing == null ? "?" : String.format("%,d", timing.getScheduled().arrival()) + "/" + String.format("%,d", timing.getScheduled().departure()));
            String real = "R: " + (timing == null ? "?" : String.format("%,d", timing.getRealtime().arrival()) + "/" + String.format("%,d", timing.getRealtime().departure()));
            String dev = "D: " + (timing == null ? "?" : String.format("%+,d", timing.getArrivalDeviation()) + "/" + String.format("%+,d", timing.getDepartureDeviation()));
            String leg = timing == null ? "?" : timing.legDuration().get() + " (" + timing.legDuration().lastMeasurement() + " | "
                + String.join(",", timing.legDuration().getHistory().stream().map(String::valueOf).toList()) + ")";
            String visits = timing == null ? "?" : "x" + timing.getCompletedVisits();

            ChatFormatting color = timing != null && timing.isDelayed(500) ? (isCurrent ? ChatFormatting.GOLD : ChatFormatting.RED)
                : isCurrent ? ChatFormatting.YELLOW
                : timing != null && timing.legDuration().isInitialized() ? ChatFormatting.WHITE
                : ChatFormatting.DARK_GRAY;

            drawLine(graphics, TextUtils.text(String.format("%s[%3d] %s  %-16s %-16s %-12s %-26s %s",
                marker, stop.entryIndex(), station, sched, real, dev, leg, visits)).withStyle(color));
        }
    }

    private void renderSections(DLGuiGraphics graphics, TrackedTrain train) {
        drawLine(graphics, TextUtils.text("Sections:").withStyle(ChatFormatting.UNDERLINE));
        for (JourneySection section : train.getJourney().getSections()) {
            boolean isCurrent = train.getCurrentSection().map(x -> x == section).orElse(false);
            drawLine(graphics, TextUtils.text((isCurrent ? " ▶ " : " □ ")
                + "[" + section.entryIndex() + "] "
                + section.getFirstStop().map(JourneyStop::getStationFilter).orElse("?") + " -> " + section.getLastStop().map(JourneyStop::getStationFilter).orElse("?")
                + "   Line: " + section.getTrainLine().map(TrainLine::getLineName).orElse("-")
                + "   Category: " + section.getTrainCategory().map(TrainCategory::getCategoryName).orElse("-")
                + "   Usable: " + yesNo(section.isUsable())
                + "   IncludesNextStart: " + yesNo(section.includesNextSectionStart())
                + (section.isDefault() ? "   (default)" : "")
            ).withStyle(isCurrent ? ChatFormatting.YELLOW : section.isUsable() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
        }
    }

    private void renderDepartureBoardSample(DLGuiGraphics graphics, TrackedTrain train, long now) {
        Optional<JourneyStop> current = train.getCurrentStop();
        if (current.isEmpty()) {
            return;
        }
        String station = current.get().getStationName();
        drawLine(graphics, TextUtils.text("API sample - departures at '" + station + "':").withStyle(ChatFormatting.UNDERLINE));
        try {
            List<BoardEntry> board = RailwayBackendApi.getDepartures(station, BoardQuery.defaults().withLimit(4));
            if (board.isEmpty()) {
                drawLine(graphics, TextUtils.text("   (none)").withStyle(ChatFormatting.DARK_GRAY));
            }
            for (BoardEntry entry : board) {
                drawLine(graphics, TextUtils.text("   " + fit(entry.displayName(), 20)
                    + " -> " + fit(entry.destinationText(), 24)
                    + "  dep " + rel(entry.realtimeDeparture(), now)
                    + (entry.isDelayed() ? " (+" + entry.departureDeviation() + ")" : "")
                ).withStyle(entry.isDelayed() ? ChatFormatting.RED : ChatFormatting.GREEN));
            }
        } catch (Exception e) {
            drawLine(graphics, TextUtils.text("   API error: " + e.getMessage()).withStyle(ChatFormatting.RED));
        }
    }

    /** Formats an absolute tick timestamp relative to now, e.g. {@code +120t}. */
    private static String rel(long time, long now) {
        if (time < 0) {
            return "?";
        }
        long diff = time - now;
        return (diff >= 0 ? "+" : "") + diff + "t";
    }

    private static String fit(String text, int length) {
        if (text == null) text = "";
        if (text.length() > length) {
            return text.substring(0, length - 1) + "…";
        }
        return String.format("%-" + length + "s", text);
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String shortId(java.util.UUID id) {
        return id == null ? "?" : id.toString().substring(0, 8);
    }

    private static ChatFormatting lifecycleColor(TrainLifecycleState state) {
        return switch (state) {
            case READY -> ChatFormatting.GREEN;
            case LEARNING -> ChatFormatting.YELLOW;
            case PREPARING -> ChatFormatting.GRAY;
            case IDLE -> ChatFormatting.DARK_GRAY;
            case CANCELLED -> ChatFormatting.RED;
        };
    }

    private static ChatFormatting liveStateColor(LiveTrainState state) {
        return switch (state) {
            case EN_ROUTE -> ChatFormatting.GREEN;
            case AT_STATION -> ChatFormatting.AQUA;
            case WAITING_FOR_SIGNAL, STALLED -> ChatFormatting.RED;
            case SCHEDULE_PAUSED, SCHEDULE_COMPLETED, NO_SCHEDULE, DERAILED -> ChatFormatting.DARK_RED;
        };
    }

    private static ChatFormatting statusColor(DelayInstance delay) {
        return switch (delay.severity()) {
            case INFO -> ChatFormatting.WHITE;
            case DELAY -> ChatFormatting.GOLD;
            case IMPORTANT -> ChatFormatting.RED;
        };
    }

    private void drawLine(DLGuiGraphics graphics, Component text) {
        int x = 2;
        int y = 2;
        GuiUtils.fill(graphics, x - 1, y - 1 + line * (graphics.defaultFont().lineHeight + 2), graphics.defaultFont().width(text) + 2, graphics.defaultFont().lineHeight + 2, DLColor.fromInt(0x44000000));
        GuiUtils.drawString(graphics, graphics.defaultFont(), x, y + 1 + line * (graphics.defaultFont().lineHeight + 2), text, DLColor.WHITE, ETextAlignment.LEFT, true);
        line++;
    }
}
