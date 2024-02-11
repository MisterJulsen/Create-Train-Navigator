package de.mrjulsen.crn.event.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleRoute.StationTag;
import de.mrjulsen.crn.data.SimpleRoute.TaggedStationEntry;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.RealtimeRequestPacket;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils.TimeFormat;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class JourneyListener {

    public static final int ID = 1;
    private static final int INFO_BEFORE_NEXT_STOP = 500;
    private static final int REALTIME_REFRESH_TIME = 100;
    private static final Component TEXT_CONCAT = new TextComponent("     ***     ");
    
    private final TaggedStationEntry[] taggedRoute;
    private final int transferCount;
    private final int totalDuration;
    private int stationIndex = 0;
    private State currentState = State.BEFORE_JOURNEY;
    private int realTimeRefreshTimer = 0;
    private boolean isStarted;
    
    private static final String keyJourneyBegins = "gui.createrailwaysnavigator.route_overview.journey_begins";
    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyAfterJourney = "gui.createrailwaysnavigator.route_overview.after_journey";
    private static final String keyJourneyInterrupted = "gui.createrailwaysnavigator.route_overview.journey_interrupted";
    private static final String keyConnectionMissedInfo = "gui.createrailwaysnavigator.route_overview.connection_missed_info";
    private static final String keyOptionsText = "gui.createrailwaysnavigator.route_overview.options";
    private static final String keyKeybindOptions = "key.createrailwaysnavigator.route_overlay_options";

    // Events
    public static record JourneyBeginEvent(State state, Component infoText, String narratorText) {}
    public static record ReachNextStopData(State state, Component infoText, String narratorText, boolean isTransfer, boolean connectionMissed) {}
    public static record ContinueData(State state) {}
    public static record FinishJourneyData(State state, Component infoText) {}    
    public static record AnnounceNextStopData(State state, Component infoText, String narratorText, boolean isTransfer) {}

    private Optional<Runnable> onUpdateRealtime = Optional.empty();
    private Optional<Consumer<Component>> onInfoTextChange = Optional.empty();
    private Optional<Consumer<State>> onStateChange = Optional.empty();
    private Optional<Consumer<String>> onNarratorAnnounce = Optional.empty();
    private Optional<Consumer<JourneyBeginEvent>> onJourneyBegin = Optional.empty();
    private Optional<Consumer<ReachNextStopData>> onReachNextStop = Optional.empty();
    private Optional<Consumer<ContinueData>> onContinue = Optional.empty();
    private Optional<Consumer<FinishJourneyData>> onFinishJourney = Optional.empty();
    private Optional<Consumer<AnnounceNextStopData>> onAnnounceNextStop = Optional.empty();


    public JourneyListener(SimpleRoute route) {
        this.taggedRoute = route.getRoutePartsTagged();
        this.transferCount = route.getTransferCount();
        this.totalDuration = route.getTotalDuration();
    }

    public static JourneyListener listenTo(SimpleRoute route) {
        return new JourneyListener(route);
    }

    public JourneyListener start() {
        Component text = new TranslatableComponent(keyJourneyBegins,
            currentStation().train().trainName(),
            currentStation().train().scheduleTitle(),
            TimeUtils.parseTime((int)currentStation().station().getEstimatedTimeWithThreshold() + Constants.TIME_SHIFT, TimeFormat.HOURS_24),
            currentStation().station().getInfo().platform()
        );
        String narratorText = text.getString() + ". " + Utils.translate(keyOptionsText, new KeybindComponent(keyKeybindOptions)).getString();

        if (onJourneyBegin.isPresent()) {
            onJourneyBegin.get().accept(new JourneyBeginEvent(currentState, text, narratorText));
        }

        isStarted = true;
        return this;
    }

    public JourneyListener stop() {
        isStarted = false;
        return this;
    }

    public JourneyListener onUpdateRealtime(Runnable m) {
        this.onUpdateRealtime = Optional.of(m);
        return this;
    }

    public JourneyListener onStateChange(Consumer<State> m) {
        this.onStateChange = Optional.of(m);
        return this;
    }

    public JourneyListener onNarratorAnnounce(Consumer<String> m) {
        this.onNarratorAnnounce = Optional.of(m);
        return this;
    }

    public JourneyListener onInfoTextChange(Consumer<Component> m) {
        this.onInfoTextChange = Optional.of(m);
        return this;
    }

    public JourneyListener onReachNextStop(Consumer<ReachNextStopData> m) {
        this.onReachNextStop = Optional.of(m);
        return this;
    }
    
    public JourneyListener onContinueWithJourneyAfterStop(Consumer<ContinueData> m) {
        this.onContinue = Optional.of(m);
        return this;
    }

    public JourneyListener onFinishJourney(Consumer<FinishJourneyData> m) {
        this.onFinishJourney = Optional.of(m);
        return this;
    }
    
    public JourneyListener onAnnounceNextStop(Consumer<AnnounceNextStopData> m) {
        this.onAnnounceNextStop = Optional.of(m);
        return this;
    }

    public JourneyListener onJourneyBegin(Consumer<JourneyBeginEvent> m) {
        this.onJourneyBegin = Optional.of(m);
        return this;
    }




    public void tick() {
        if (!isStarted) {
            return;
        }

        if (currentState != State.AFTER_JOURNEY && currentState != State.JOURNEY_INTERRUPTED) {
            realTimeRefreshTimer++;
            if (realTimeRefreshTimer > REALTIME_REFRESH_TIME) {
                realTimeRefreshTimer = 0;
                requestRealtimeData();
            }
        }
    }
    
    private void requestRealtimeData() {
        final Collection<UUID> ids = Arrays.stream(taggedRoute).map(x -> x.train().trainId()).distinct().toList();
        long id = InstanceManager.registerClientRealtimeResponseAction((predictions, time) -> {
            Map<UUID, List<SimpleDeparturePrediction>> predMap = predictions.stream().collect(Collectors.groupingBy(SimpleDeparturePrediction::id));
            Map<UUID, List<TaggedStationEntry>> mappedRoute = Arrays.stream(taggedRoute).skip(stationIndex).collect(Collectors.groupingBy(x -> x.train().trainId(), LinkedHashMap::new, Collectors.toList()));
            
            // Update realtime data
            for (int i = stationIndex; i < taggedRoute.length; i++) {
                TaggedStationEntry e = taggedRoute[i];
                List<SimpleDeparturePrediction> preds = predMap.get(e.train().trainId());
                List<TaggedStationEntry> stations = mappedRoute.get(e.train().trainId());
                updateRealtime(preds, stations, e.train().trainId(), stationIndex, time);                
            }
            List<SimpleDeparturePrediction> currentTrainSchedule = predMap.get(currentStation().train().trainId());
            SimpleDeparturePrediction currentTrainNextStop = predMap.get(currentStation().train().trainId()).get(0);

            // check if connection train has departed
            for (List<TaggedStationEntry> routePart : mappedRoute.values()) {
                if (mappedRoute.size() < 2) {
                    continue;
                }
                long min = routePart.stream().filter(x -> x.station().getCurrentTime() + ModClientConfig.TRANSFER_TIME.get() > x.station().getScheduleTime()).mapToLong(x -> x.station().getCurrentTime()).min().orElse(-1);
                long currentTime = routePart.get(0).station().getCurrentTime();

                if (min > 0 && currentTime > min && currentTime + ModClientConfig.TRANSFER_TIME.get() > routePart.get(0).station().getScheduleTime()) {
                    routePart.get(0).setDeparted(true);
                }
            }

            checkStationAccessibility();
            
            // PROGRESS ANIMATION
            if (currentState != State.BEFORE_JOURNEY && currentState != State.JOURNEY_INTERRUPTED) {
                if (!currentState.nextStopAnnounced() && !currentState.isWaitingForNextTrainToDepart() // state check
                    && time >= taggedRoute[stationIndex].station().getEstimatedTime() - INFO_BEFORE_NEXT_STOP) // train check
                {                    
                    announceNextStop();
                }
                
                if (currentState != State.WHILE_TRAVELING && currentState != State.WHILE_TRANSFER) // train check
                {     
                    while (!currentTrainNextStop.station().equals(currentStation().station().getStationName()) && currentState != State.AFTER_JOURNEY) {
                        if (currentStation().tag() == StationTag.END) {
                            finishJourney();
                        } else {
                            nextStop();
                        }
                    }
                }
            }

            if ((!currentState.isWaitingForNextTrainToDepart() || currentState == State.BEFORE_JOURNEY || currentState == State.WHILE_TRANSFER)
                && isStationValidForShedule(currentTrainSchedule, currentStation().train().trainId(), stationIndex) && time >= currentStation().station().getEstimatedTime()) {                    
                if (currentStation().tag() == StationTag.PART_END) {
                    if (taggedRoute[stationIndex + 1].isDeparted()) {
                        reachTransferStopConnectionMissed();
                    } else {
                        reachTransferStop();
                    }
                } else {
                    reachNextStop();
                }
            }

            if (onUpdateRealtime.isPresent()) {
                onUpdateRealtime.get().run();
            }
        });
        NetworkManager.sendToServer(new RealtimeRequestPacket(id, ids));
    }

    private boolean isStationValidForShedule(List<SimpleDeparturePrediction> schedule, UUID trainId, int startIndex) {
        List<String> filteredStationEntryList = new ArrayList<>();
        for (int i = startIndex; i < taggedRoute.length; i++) {
            TaggedStationEntry entry = taggedRoute[i];
            if (!entry.train().trainId().equals(trainId)) {
                break;
            }
            filteredStationEntryList.add(entry.station().getStationName());
        }
        String[] filteredStationEntries = filteredStationEntryList.toArray(String[]::new);
        String[] sched = schedule.stream().map(x -> x.station()).toArray(String[]::new);
        
        int k = 0;
        for (int i = 0; i < filteredStationEntries.length; i++) {
            if (!filteredStationEntries[i].equals(sched[k])) {
                return false;
            }

            k++;
            if (k > sched.length) {
                k = 0;
            }
        }
        return true;
    }

    private void updateRealtime(List<SimpleDeparturePrediction> schedule, List<TaggedStationEntry> route, UUID trainId, int startIndex, long updateTime) {
        boolean b = false;
        long lastTime = -1;
        for (int i = 0, k = 0; i < schedule.size() && k < route.size(); i++) {
            SimpleDeparturePrediction current = schedule.get(i);
            long newTime = current.ticks() + updateTime;
            if (route.get(0).station().getStationName().equals(current.station())) {
                k = 0;
                b = true;
            }

            if (route.get(k).station().getStationName().equals(current.station()) && b == true) {
                if (newTime > lastTime/* && newTime + EARLY_ARRIVAL_THRESHOLD > route.get(k).station().getScheduleTime()*/) {
                    route.get(k).station().updateRealtimeData(current.ticks(), updateTime);
                    lastTime = route.get(k).station().getCurrentTime();
                }
                k++;
            } else {
                b = false;
            }
        }
    }

    private Component concat(Component... components) {
        if (components.length <= 0) {
            return new TextComponent("");
        }

        MutableComponent c = components[0].copy();
        for (int i = 1; i < components.length; i++) {
            c.append(TEXT_CONCAT);
            c.append(components[i]);
        }
        return c;
    }
    

    private void nextStop() {
        if (!changeCurrentStation()) {
            return;
        }

        //setTrainDataSubPage(true);
        //setPageRouteOverview();
        currentState = State.WHILE_TRAVELING;
        
        if (onContinue.isPresent()) {
            onContinue.get().accept(new ContinueData(currentState));
        }
        if (onStateChange.isPresent()) {
            onStateChange.get().accept(currentState);
        }
    }

    private boolean changeCurrentStation() {
        if (stationIndex + 1 >= taggedRoute.length) {
            finishJourney();
            return false;
        }
        stationIndex++;
        return true;
    }    

    private void finishJourney() {
        Component text = Utils.translate(keyAfterJourney, taggedRoute[taggedRoute.length - 1].station().getStationName());
        currentState = State.AFTER_JOURNEY;
        //setPageJourneyCompleted();
        if (onFinishJourney.isPresent()) {
            onFinishJourney.get().accept(new FinishJourneyData(currentState, text));
        }
        if (onStateChange.isPresent()) {
            onStateChange.get().accept(currentState);
        }
        if (onInfoTextChange.isPresent()) {
            onInfoTextChange.get().accept(text);
        }

        stop();
    }

    private void announceNextStop() {
        Component textA = Utils.emptyText();
        Component textB = Utils.emptyText();
        Component text;
        text = textA = new TranslatableComponent(keyNextStop,
            currentStation().station().getStationName()
        );
        if (currentStation().tag() == StationTag.PART_END && currentStation().index() + 1 < taggedRoute.length) {
            Component transferText = textB = new TranslatableComponent(keyTransfer,
                nextStation().get().train().trainName(),
                nextStation().get().train().scheduleTitle(),
                nextStation().get().station().getInfo().platform()
            );
            text = concat(text, transferText);
            currentState = State.BEFORE_TRANSFER;
        } else {
            currentState = State.BEFORE_NEXT_STOP;
        }

        String narratorText = textA.getString() + ". " + textB.getString();
        boolean transfer = currentStation().tag() == StationTag.PART_END && currentStation().index() + 1 < taggedRoute.length;
        /*
        setSlidingText(text);


        if (ModClientConfig.ROUTE_NARRATOR.get()) {
            NarratorChatListener.INSTANCE.narrator.say(textA.getString() + ". " + textB.getString(), true);
        }

        setPageNextConnections();
        */
        if (onAnnounceNextStop.isPresent()) {
            onAnnounceNextStop.get().accept(new AnnounceNextStopData(currentState, text, narratorText, transfer));
        }
        if (onStateChange.isPresent()) {
            onStateChange.get().accept(currentState);
        }
        if (onInfoTextChange.isPresent()) {
            onInfoTextChange.get().accept(text);
        }
        if (onNarratorAnnounce.isPresent()) {
            onNarratorAnnounce.get().accept(narratorText);
        }
    }

    private void reachNextStop() {
        Component text = Utils.text(currentStation().station().getStationName());        
        String narratorText = text.getString();
        /*
        setSlidingText();
        if (ModClientConfig.ROUTE_NARRATOR.get()) {
            NarratorChatListener.INSTANCE.narrator.say(currentStation().station().getStationName(), true);
        }
        */
        currentState = State.WHILE_NEXT_STOP;
        if (onReachNextStop.isPresent()) {
            onReachNextStop.get().accept(new ReachNextStopData(currentState, text, narratorText, false, false));
        }
        if (onStateChange.isPresent()) {
            onStateChange.get().accept(currentState);
        }
        if (onInfoTextChange.isPresent()) {
            onInfoTextChange.get().accept(text);
        }
        if (onNarratorAnnounce.isPresent()) {
            onNarratorAnnounce.get().accept(narratorText);
        }
    }

    private void reachTransferStop() {
        Component text = nextStation().isPresent() ? new TranslatableComponent(keyTransfer,
            nextStation().get().train().trainName(),
            nextStation().get().train().scheduleTitle(),
            nextStation().get().station().getInfo().platform()
        ) : Utils.emptyText();
        String narratorText = text.getString();
        /*
        setSlidingText(text);
        if (ModClientConfig.ROUTE_NARRATOR.get()) {
            NarratorChatListener.INSTANCE.narrator.say(text.getString(), true);
        }
        */
        currentState = State.WHILE_TRANSFER;
        changeCurrentStation();
        //setPageTransfer();
        if (onReachNextStop.isPresent()) {
            onReachNextStop.get().accept(new ReachNextStopData(currentState, text, narratorText, true, false));
        }
        if (onStateChange.isPresent()) {
            onStateChange.get().accept(currentState);
        }
        if (onInfoTextChange.isPresent()) {
            onInfoTextChange.get().accept(text);
        }
        if (onNarratorAnnounce.isPresent()) {
            onNarratorAnnounce.get().accept(narratorText);
        }
    }

    private void reachTransferStopConnectionMissed() {
        Component text = concat(
            Utils.text(currentStation().station().getStationName()),
            new TranslatableComponent(keyConnectionMissedInfo),
            new TranslatableComponent(keyJourneyInterrupted,
                taggedRoute[taggedRoute.length - 1].station().getStationName()
            )
        );
        String narratorText = "";

        //setPageJourneyInterrupted();
        currentState = State.JOURNEY_INTERRUPTED;
        if (onReachNextStop.isPresent()) {
            onReachNextStop.get().accept(new ReachNextStopData(currentState, text, narratorText, true, true));
        }
        if (onStateChange.isPresent()) {
            onStateChange.get().accept(currentState);
        }
        if (onInfoTextChange.isPresent()) {
            onInfoTextChange.get().accept(text);
        }
        if (onNarratorAnnounce.isPresent()) {
            onNarratorAnnounce.get().accept(narratorText);
        }
        
        stop();
    }

    private void checkStationAccessibility() {
        boolean willMiss = false;
        for (int i = stationIndex; i < taggedRoute.length; i++) {
            TaggedStationEntry station = taggedRoute[i];
            TaggedStationEntry nextStation = i < taggedRoute.length - 1 ? taggedRoute[i + 1] : null;
            if (station.isDeparted()) {
                willMiss = true;
            }

            if (!willMiss) {
                long transferTime = -1;
                if (nextStation != null && !nextStation.isDeparted()) {
                    if (nextStation.station().getCurrentTime() + ModClientConfig.TRANSFER_TIME.get() < nextStation.station().getScheduleTime()) {
                        transferTime = nextStation.station().getScheduleTime() - station.station().getScheduleTime();
                    } else {
                        transferTime = nextStation.station().getCurrentTime() - station.station().getCurrentTime();
                    }
                }

                if (transferTime < 0) {
                    willMiss = true;
                }
            }
            
            station.setWillMiss(false);
        }
    }

    public long getTransferTime(int index) {
        Optional<TaggedStationEntry> station = getEntryAt(index);
        Optional<TaggedStationEntry> nextStation = getEntryAt(index + 1);
        long transferTime = -1;
        if (station.isPresent() && nextStation.isPresent() && !nextStation.get().isDeparted()) {
            if (nextStation.get().station().getCurrentTime() + ModClientConfig.TRANSFER_TIME.get() < nextStation.get().station().getScheduleTime()) {
                transferTime = nextStation.get().station().getScheduleTime() - station.get().station().getScheduleTime();
            } else {
                transferTime = nextStation.get().station().getCurrentTime() - station.get().station().getCurrentTime();
            }
        }

        return transferTime;
    }




    public TaggedStationEntry currentStation() {
        return taggedRoute[stationIndex];
    }

    public Optional<TaggedStationEntry> nextStation() {
        return stationIndex + 1 < taggedRoute.length ? Optional.of(taggedRoute[stationIndex + 1]) : Optional.empty();
    }

    public Optional<TaggedStationEntry> previousSation() {
        return stationIndex > 0 ? Optional.of(taggedRoute[stationIndex - 1]) : Optional.empty();
    }

    public TaggedStationEntry firstStation() {
        return taggedRoute[0];
    }

    public TaggedStationEntry lastStation() {
        return taggedRoute[taggedRoute.length - 1];
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public TaggedStationEntry[] stations() {
        return taggedRoute;
    }

    public int getTransferCount() {
        return transferCount;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public long getTimeDifferenceForStation(int index) {        
        TaggedStationEntry station = taggedRoute[index];
        return station.station().getCurrentRefreshTime() + station.station().getCurrentTicks() - station.station().getRefreshTime() - station.station().getTicks();
    }    

    public int getIndex() {
        return stationIndex;
    }

    public int getStationCount() {
        return taggedRoute.length;
    }
    
    public Optional<TaggedStationEntry> getEntryAt(int index) {
        return index >= 0 && index < taggedRoute.length ? Optional.of(taggedRoute[index]) : Optional.empty();
    }



    public static enum State {
        BEFORE_JOURNEY,
        WHILE_TRAVELING,
        BEFORE_NEXT_STOP,
        WHILE_NEXT_STOP,
        BEFORE_TRANSFER,
        WHILE_TRANSFER,
        AFTER_JOURNEY,
        JOURNEY_INTERRUPTED;

        public boolean nextStopAnnounced() {
            return this == BEFORE_NEXT_STOP || this == BEFORE_TRANSFER;
        }

        public boolean isWhileTraveling() {
            return this == WHILE_TRAVELING;
        }

        public boolean isTranferring() {
            return this == WHILE_TRANSFER;
        }

        public boolean isAtNextStop() {
            return this == WHILE_NEXT_STOP;
        }

        public boolean isWaitingForNextTrainToDepart() {
            return isTranferring() || isAtNextStop();
        }

        public boolean important() {
            return this == State.WHILE_TRANSFER || this == State.BEFORE_TRANSFER || this == State.AFTER_JOURNEY || this == State.BEFORE_JOURNEY ||this == JOURNEY_INTERRUPTED;
        }
    }
}
