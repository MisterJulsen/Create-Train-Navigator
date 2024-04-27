package de.mrjulsen.crn.event.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleRoute.StationEntry;
import de.mrjulsen.crn.data.SimpleRoute.StationTag;
import de.mrjulsen.crn.data.TrainStationAlias.StationInfo;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.packets.cts.RealtimeRequestPacket;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.KeybindComponent;

public class JourneyListener {

    public static final int ID = 1;
    private static final int REALTIME_REFRESH_TIME = 100;
    
    private final SimpleRoute route;
    private int stationIndex = 0;
    private State currentState = State.BEFORE_JOURNEY;
    private int realTimeRefreshTimer = 0;
    private boolean isStarted;
    
    private static final String keyJourneyBegins = "gui.createrailwaysnavigator.route_overview.journey_begins";
    private static final String keyJourneyBeginsWithPlatform = "gui.createrailwaysnavigator.route_overview.journey_begins_with_platform";
    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyTransferWithPlatform = "gui.createrailwaysnavigator.route_overview.transfer_with_platform";
    private static final String keyAfterJourney = "gui.createrailwaysnavigator.route_overview.after_journey";
    private static final String keyJourneyInterruptedTitle = "gui.createrailwaysnavigator.route_overview.train_canceled_title";
    private static final String keyJourneyInterrupted = "gui.createrailwaysnavigator.route_overview.train_canceled_info";
    private static final String keyConnectionMissedInfo = "gui.createrailwaysnavigator.route_overview.connection_missed_info";
    private static final String keyOptionsText = "gui.createrailwaysnavigator.route_overview.options";
    private static final String keyKeybindOptions = "key.createrailwaysnavigator.route_overlay_options";
    private static final String keyNotificationJourneyBeginsTitle = "gui.createrailwaysnavigator.route_overview.notification.journey_begins.title";
    private static final String keyNotificationJourneyBegins = "gui.createrailwaysnavigator.route_overview.notification.journey_begins";
    private static final String keyNotificationJourneyBeginsWithPlatform = "gui.createrailwaysnavigator.route_overview.notification.journey_begins_with_platform";
    private static final String keyNotificationPlatformChangedTitle = "gui.createrailwaysnavigator.route_overview.notification.platform_changed.title";
    private static final String keyNotificationPlatformChanged = "gui.createrailwaysnavigator.route_overview.notification.platform_changed";    
    private static final String keyNotificationTrainDelayedTitle = "gui.createrailwaysnavigator.route_overview.notification.train_delayed.title";
    private static final String keyNotificationTrainDelayed = "gui.createrailwaysnavigator.route_overview.notification.train_delayed";
    private static final String keyNotificationTransferTitle = "gui.createrailwaysnavigator.route_overview.notification.transfer.title";
    private static final String keyNotificationTransfer = "gui.createrailwaysnavigator.route_overview.notification.transfer";
    private static final String keyNotificationTransferWithPlatform = "gui.createrailwaysnavigator.route_overview.notification.transfer_with_platform";
    private static final String keyNotificationConnectionEndangeredTitle = "gui.createrailwaysnavigator.route_overview.notification.connection_endangered.title";
    private static final String keyNotificationConnectionEndangered = "gui.createrailwaysnavigator.route_overview.notification.connection_endangered";
    private static final String keyNotificationConnectionMissedTitle = "gui.createrailwaysnavigator.route_overview.notification.connection_missed.title";
    private static final String keyNotificationConnectionMissed = "gui.createrailwaysnavigator.route_overview.notification.connection_missed";
    private static final String keyNotificationJourneyCompletedTitle = "gui.createrailwaysnavigator.route_overview.notification.journey_completed.title";
    private static final String keyNotificationJourneyCompleted = "gui.createrailwaysnavigator.route_overview.notification.journey_completed";

    // Events
    public static enum TransferState {
        NONE,
        DEFAULT,
        CONNECTION_MISSED,
        CONNECTION_CANCELLED        
    }

    public static record NotificationData(State state, Component title, Component text) {}
    public static record JourneyBeginData(State state, Component infoText, String narratorText) {}
    public static record JourneyInterruptData(State state, Component title, Component text, String narratorText) {}
    public static record ReachNextStopData(State state, Component infoText, String narratorText, TransferState transferState) {}
    public static record ContinueData(State state) {}
    public static record FinishJourneyData(State state, Component infoText) {}    
    public static record AnnounceNextStopData(State state, Component infoText, String narratorText, boolean isTransfer) {}

    private Map<UUID, Optional<Runnable>> onUpdateRealtime = new HashMap<>();
    private Map<UUID, Optional<Consumer<Component>>> onInfoTextChange = new HashMap<>();
    private Map<UUID, Optional<Consumer<NotificationData>>> onNotificationSend = new HashMap<>();
    private Map<UUID, Optional<Consumer<State>>> onStateChange = new HashMap<>();
    private Map<UUID, Optional<Consumer<String>>> onNarratorAnnounce = new HashMap<>();
    private Map<UUID, Optional<Consumer<JourneyBeginData>>> onJourneyBegin = new HashMap<>();
    private Map<UUID, Optional<Consumer<JourneyInterruptData>>> onJourneyInterrupt = new HashMap<>();
    private Map<UUID, Optional<Consumer<ReachNextStopData>>> onReachNextStop = new HashMap<>();
    private Map<UUID, Optional<Consumer<ContinueData>>> onContinue = new HashMap<>();
    private Map<UUID, Optional<Consumer<FinishJourneyData>>> onFinishJourney = new HashMap<>();
    private Map<UUID, Optional<Consumer<AnnounceNextStopData>>> onAnnounceNextStop = new HashMap<>();

    private Component lastInfoText = TextUtils.empty();
    private NotificationData lastNotification = null;
    private String lastNarratorText = "";

    private boolean beginAnnounced = false;

    public JourneyListener(SimpleRoute route) {
        this.route = route;
    }

    public static JourneyListener listenTo(SimpleRoute route) {
        return new JourneyListener(route);
    }

    public JourneyListener start() {
        Component text = currentStation().getInfo().platform() == null || currentStation().getInfo().platform().isBlank() ?
        TextUtils.translate(keyJourneyBegins,
            currentStation().getTrain().trainName(),
            currentStation().getTrain().scheduleTitle(),
            TimeUtils.parseTime((int)currentStation().getEstimatedTimeWithThreshold() + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get())
        ) :
        TextUtils.translate(keyJourneyBeginsWithPlatform,
            currentStation().getTrain().trainName(),
            currentStation().getTrain().scheduleTitle(),
            TimeUtils.parseTime((int)currentStation().getEstimatedTimeWithThreshold() + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get()),
            currentStation().getInfo().platform()
        );
        String narratorText = text.getString() + ". " + TextUtils.translate(keyOptionsText, new KeybindComponent(keyKeybindOptions)).getString();

        onJourneyBegin.values().forEach(x -> {
            if (x.isPresent()) {
                x.get().accept(new JourneyBeginData(currentState, text, narratorText));
            }
        });
        setInfoText(text);
        setNarratorText(narratorText);

        isStarted = true;
        requestRealtimeData();
        return this;
    }

    public JourneyListener stop() {
        isStarted = false;
        return this;
    }

    public JourneyListener registerOnUpdateRealtime(IJourneyListenerClient client, Runnable m) {
        unregisterOnUpdateRealtime(client);
        this.onUpdateRealtime.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }

    public JourneyListener registerOnStateChange(IJourneyListenerClient client, Consumer<State> m) {
        unregisterOnUpdateRealtime(client);
        this.onStateChange.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }

    public JourneyListener registerOnNarratorAnnounce(IJourneyListenerClient client, Consumer<String> m) {
        unregisterOnNarratorAnnounce(client);
        this.onNarratorAnnounce.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }

    public JourneyListener registerOnInfoTextChange(IJourneyListenerClient client, Consumer<Component> m) {
        unregisterOnInfoTextChange(client);
        this.onInfoTextChange.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }

    public JourneyListener registerOnNotification(IJourneyListenerClient client, Consumer<NotificationData> m) {
        unregisterOnNotification(client);
        this.onNotificationSend.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }

    public JourneyListener registerOnReachNextStop(IJourneyListenerClient client, Consumer<ReachNextStopData> m) {
        unregisterOnReachNextStop(client);
        this.onReachNextStop.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }
    
    public JourneyListener registerOnContinueWithJourneyAfterStop(IJourneyListenerClient client, Consumer<ContinueData> m) {
        unregisterOnContinueWithJourneyAfterStop(client);
        this.onContinue.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }

    public JourneyListener registerOnFinishJourney(IJourneyListenerClient client, Consumer<FinishJourneyData> m) {
        unregisterOnFinishJourney(client);
        this.onFinishJourney.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }
    
    public JourneyListener registerOnAnnounceNextStop(IJourneyListenerClient client, Consumer<AnnounceNextStopData> m) {
        unregisterOnAnnounceNextStop(client);
        this.onAnnounceNextStop.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }

    public JourneyListener registerOnJourneyBegin(IJourneyListenerClient client, Consumer<JourneyBeginData> m) {
        unregisterOnJourneyBegin(client);
        this.onJourneyBegin.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }

    public JourneyListener registerOnJourneyInterrupt(IJourneyListenerClient client, Consumer<JourneyInterruptData> m) {
        unregisterOnJourneyInterrupt(client);
        this.onJourneyInterrupt.put(client.getJourneyListenerClientId(), Optional.of(m));
        return this;
    }


    public void unregisterOnUpdateRealtime(IJourneyListenerClient client) {
        if (onUpdateRealtime.containsKey(client.getJourneyListenerClientId())) {
            onUpdateRealtime.remove(client.getJourneyListenerClientId());
        }
    }

    public void unregisterOnStateChange(IJourneyListenerClient client) {
        if (onStateChange.containsKey(client.getJourneyListenerClientId())) {
            onStateChange.remove(client.getJourneyListenerClientId());
        }
    }

    public void unregisterOnNarratorAnnounce(IJourneyListenerClient client) {
        if (onNarratorAnnounce.containsKey(client.getJourneyListenerClientId())) {
            onNarratorAnnounce.remove(client.getJourneyListenerClientId());
        }
    }

    public void unregisterOnNotification(IJourneyListenerClient client) {
        if (onNotificationSend.containsKey(client.getJourneyListenerClientId())) {
            onNotificationSend.remove(client.getJourneyListenerClientId());
        }
    }

    public void unregisterOnInfoTextChange(IJourneyListenerClient client) {
        if (onInfoTextChange.containsKey(client.getJourneyListenerClientId())) {
            onInfoTextChange.remove(client.getJourneyListenerClientId());
        }
    }

    public void unregisterOnReachNextStop(IJourneyListenerClient client) {
        if (onReachNextStop.containsKey(client.getJourneyListenerClientId())) {
            onReachNextStop.remove(client.getJourneyListenerClientId());
        }
    }
    
    public void unregisterOnContinueWithJourneyAfterStop(IJourneyListenerClient client) {
        if (onContinue.containsKey(client.getJourneyListenerClientId())) {
            onContinue.remove(client.getJourneyListenerClientId());
        }
    }

    public void unregisterOnFinishJourney(IJourneyListenerClient client) {
        if (onFinishJourney.containsKey(client.getJourneyListenerClientId())) {
            onFinishJourney.remove(client.getJourneyListenerClientId());
        }
    }
    
    public void unregisterOnAnnounceNextStop(IJourneyListenerClient client) {
        if (onAnnounceNextStop.containsKey(client.getJourneyListenerClientId())) {
            onAnnounceNextStop.remove(client.getJourneyListenerClientId());
        }
    }

    public void unregisterOnJourneyBegin(IJourneyListenerClient client) {
        if (onJourneyBegin.containsKey(client.getJourneyListenerClientId())) {
            onJourneyBegin.remove(client.getJourneyListenerClientId());
        }
    }

    public void unregisterOnJourneyInterrupt(IJourneyListenerClient client) {
        if (onJourneyInterrupt.containsKey(client.getJourneyListenerClientId())) {
            onJourneyInterrupt.remove(client.getJourneyListenerClientId());
        }
    }

    public void unregister(IJourneyListenerClient client) {
        unregisterOnAnnounceNextStop(client);
        unregisterOnContinueWithJourneyAfterStop(client);
        unregisterOnFinishJourney(client);
        unregisterOnInfoTextChange(client);
        unregisterOnJourneyBegin(client);
        unregisterOnNarratorAnnounce(client);
        unregisterOnReachNextStop(client);
        unregisterOnStateChange(client);
        unregisterOnUpdateRealtime(client);
        unregisterOnNotification(client);
        unregisterOnJourneyInterrupt(client);
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

        if (!beginAnnounced && firstStation().getEstimatedTime() - ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get() < Minecraft.getInstance().level.getDayTime()) {
            Component title = TextUtils.translate(keyNotificationJourneyBeginsTitle,
                lastStation().getStationName()
            );
            Component description = currentStation().getInfo().platform() == null || currentStation().getInfo().platform().isBlank() ?
                TextUtils.translate(keyNotificationJourneyBegins,
                    currentStation().getTrain().trainName(),
                    currentStation().getTrain().scheduleTitle(),
                    TimeUtils.parseTime((int)currentStation().getEstimatedTimeWithThreshold() + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get())
                )
            :
                TextUtils.translate(keyNotificationJourneyBeginsWithPlatform,
                    currentStation().getTrain().trainName(),
                    currentStation().getTrain().scheduleTitle(),
                    TimeUtils.parseTime((int)currentStation().getEstimatedTimeWithThreshold() + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get()),
                    currentStation().getInfo().platform()
                );

            setNotificationText(new NotificationData(currentState, title, description));
            setNarratorText(title.getString() + " " + description.getString());
            beginAnnounced = true;
        }
    }
    
    private void requestRealtimeData() {
        final Collection<UUID> ids = Arrays.stream(route.getStationArray()).map(x -> x.getTrain().trainId()).distinct().toList();
        
        long id = InstanceManager.registerClientRealtimeResponseAction((predictions, time) -> {
            Map<UUID, List<SimpleDeparturePrediction>> predMap = predictions.stream().collect(Collectors.groupingBy(SimpleDeparturePrediction::trainId));            
            
            if (predMap.containsKey(currentStation().getTrain().trainId())) {
                SimpleDeparturePrediction currentTrainNextStop = predMap.get(currentStation().getTrain().trainId()).get(0);
                List<SimpleDeparturePrediction> currentTrainSchedule = predMap.get(currentStation().getTrain().trainId());

                if (currentState != State.BEFORE_JOURNEY && currentState != State.JOURNEY_INTERRUPTED) {                
                    if (currentState != State.WHILE_TRAVELING && currentState != State.WHILE_TRANSFER) {     
                        while (!currentTrainNextStop.stationTagName().equals(currentStation().getStationName()) && currentState != State.AFTER_JOURNEY) {
                            if (currentStation().getTag() != StationTag.END) {
                                nextStop();
                            }
                        }
                    }
                }

                if (((!currentState.isWaitingForNextTrainToDepart() || currentState == State.BEFORE_JOURNEY || currentState == State.WHILE_TRANSFER) && currentStation().shouldRenderRealtime())
                    && isStationValidForShedule(currentTrainSchedule, currentStation().getTrain().trainId(), stationIndex) && time >= currentStation().getEstimatedTime()) {                    
                    if (currentStation().getTag() == StationTag.PART_END) {
                        if (route.getStationArray()[stationIndex + 1].isTrainCanceled()) {
                            journeyInterrupt(route.getStationArray()[stationIndex + 1]);
                        } else if (route.getStationArray()[stationIndex + 1].isDeparted()) {
                            reachTransferStopConnectionMissed();
                        } else {
                            reachTransferStop();
                        }
                    } else if (currentStation().getTag() == StationTag.END) {
                        finishJourney();
                    }else {
                        reachNextStop();
                    }
                }

                if (currentState == State.AFTER_JOURNEY) {
                    return;
                }
            } else {
                journeyInterrupt(currentStation());
                return;
            }
            
            Map<UUID, List<StationEntry>> mappedRoute = Arrays.stream(route.getStationArray()).skip(stationIndex).collect(Collectors.groupingBy(x -> x.getTrain().trainId(), LinkedHashMap::new, Collectors.toList()));

            // Update realtime data
            for (int i = stationIndex; i < route.getStationCount(true); i++) {
                StationEntry e = route.getStationArray()[i];
                if (!predMap.containsKey(e.getTrain().trainId()) || e.isTrainCanceled()) {
                    e.setTrainCanceled(true, "", e.getTrain().trainName());
                    continue;                    
                }

                List<SimpleDeparturePrediction> preds = predMap.get(e.getTrain().trainId());
                List<StationEntry> stations = mappedRoute.get(e.getTrain().trainId());
                updateRealtime(preds, stations, e.getTrain().trainId(), stationIndex, time);                
            }

            boolean departed = false;
            // check if connection train has departed
            for (List<StationEntry> routePart : mappedRoute.values()) {                
                if (mappedRoute.size() < 2) {
                    continue;
                }

                if (routePart.get(0).isDeparted()) {
                    continue;
                }

                if (departed) {
                    routePart.forEach(x -> x.setDeparted(true));
                    continue;
                }

                long min = routePart.stream().filter(x -> x.getCurrentTime() + ModClientConfig.TRANSFER_TIME.get() > x.getScheduleTime()).mapToLong(x -> x.getCurrentTime()).min().orElse(-1);
                long currentTime = routePart.get(0).getCurrentTime();

                if (min > 0 && currentTime > min && currentTime + ModClientConfig.TRANSFER_TIME.get() > routePart.get(0).getScheduleTime()) {
                    routePart.forEach(x -> x.setDeparted(true));
                    departed = true;

                    Component title = TextUtils.translate(keyNotificationConnectionMissedTitle);
                    Component description = TextUtils.translate(keyNotificationConnectionMissed,
                        routePart.get(0).getTrain().trainName(),
                        routePart.get(0).getTrain().scheduleTitle()
                    );
                    setNotificationText(new NotificationData(currentState, title, description));
                    setNarratorText(title.getString() + " " + description.getString());
                }
            }

            checkStationAccessibility();
            
            // PROGRESS ANIMATION
            if (currentState != State.BEFORE_JOURNEY && currentState != State.JOURNEY_INTERRUPTED) {
                if (!currentState.nextStopAnnounced() && !currentState.isWaitingForNextTrainToDepart() // state check
                    && time >= route.getStationArray()[stationIndex].getEstimatedTime() - ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get()) // train check
                {                    
                    announceNextStop();
                }
            }

            onUpdateRealtime.values().forEach(x -> {
                if (x.isPresent()) {
                    x.get().run();
                }
            });
        });
        ExampleMod.net().CHANNEL.sendToServer(new RealtimeRequestPacket(id, ids));
    }

    private boolean isStationValidForShedule(List<SimpleDeparturePrediction> schedule, UUID trainId, int startIndex) {
        List<String> filteredStationEntryList = new ArrayList<>();
        for (int i = startIndex; i < route.getStationCount(true); i++) {
            StationEntry entry = route.getStationArray()[i];
            if (!entry.getTrain().trainId().equals(trainId)) {
                break;
            }
            filteredStationEntryList.add(entry.getStationName());
        }
        String[] filteredStationEntries = filteredStationEntryList.toArray(String[]::new);
        String[] sched = schedule.stream().map(x -> x.stationTagName()).toArray(String[]::new);
        
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

    private void updateRealtime(List<SimpleDeparturePrediction> schedule, List<StationEntry> route, UUID trainId, int startIndex, long updateTime) {
        boolean b = false;
        long lastTime = -1;

        List<StationEntry> routePart = route.stream().filter(x -> x.getTrain().trainId().equals(trainId)).toList();
        StationEntry first = routePart.get(0);
        if (first.getTag() != StationTag.PART_START && first.getTag() != StationTag.START) {
            first = null;
        }
        StationEntry last = routePart.get(routePart.size() - 1);
        boolean wasDelayed = last.isDelayed();
        StationInfo oldInfo = first == null ? null : first.getUpdatedInfo();

        for (int i = 0, k = 0; i < schedule.size() && k < route.size(); i++) {
            SimpleDeparturePrediction current = schedule.get(i);
            long newTime = current.departureTicks() + updateTime;
            if (route.get(0).getStationName().equals(current.stationTagName())) {
                k = 0;
                b = true;
            }

            if (route.get(k).getStationName().equals(current.stationTagName()) && b == true) {
                if (newTime > lastTime/* && newTime + EARLY_ARRIVAL_THRESHOLD > route.get(k).station().getScheduleTime()*/) {
                    route.get(k).updateRealtimeData(current.departureTicks(), updateTime, current.stationInfo(), () -> {

                    });
                    lastTime = route.get(k).getCurrentTime();
                }
                k++;
            } else {
                b = false;
            }
        }

        if (oldInfo != null && !first.getUpdatedInfo().equals(oldInfo)) {
            setNotificationText(new NotificationData(currentState, TextUtils.translate(keyNotificationPlatformChangedTitle), TextUtils.translate(keyNotificationPlatformChanged,
                first.getStationName(),
                first.getUpdatedInfo().platform()
            )));
        }

        if (!wasDelayed && last.isDelayed()) {
            setNotificationText(new NotificationData(currentState, TextUtils.translate(keyNotificationTrainDelayedTitle,
                last.getTrain().trainName(),
                TimeUtils.parseDuration((int)last.getDifferenceTime())
            ), TextUtils.translate(keyNotificationTrainDelayed,
                TimeUtils.parseTime((int)(last.getEstimatedTimeWithThreshold() % 24000) + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get()),
                TimeUtils.parseTime((int)(last.getScheduleTime() % 24000) + DragonLib.DAYTIME_SHIFT, ModClientConfig.TIME_FORMAT.get()),
                last.getStationName()
            )));
        }
    }

    

    private void checkStationAccessibility() {
        boolean willMiss = false;
        for (int i = stationIndex; i < route.getStationCount(true); i++) {
            StationEntry station = route.getStationArray()[i];
            StationEntry nextStation = i < route.getStationCount(true) - 1 ? route.getStationArray()[i + 1] : null;
            
            boolean wasWillMiss = station.willMissStop();

            if (nextStation == null) {
                continue;
            }

            if (station.isDeparted()) {
                willMiss = true;
            }

            if (!willMiss) {
                long transferTime = -1;
                if (nextStation != null && !nextStation.isDeparted()) {
                    if (nextStation.getCurrentTime() + ModClientConfig.TRANSFER_TIME.get() < nextStation.getScheduleTime()) {
                        transferTime = nextStation.getScheduleTime() - station.getScheduleTime();
                    } else {
                        transferTime = nextStation.getCurrentTime() - station.getCurrentTime();
                    }
                }

                if (transferTime < 0) {
                    willMiss = true;
                }
            }
            
            station.setWillMiss(willMiss);

            if (station.getTag() == StationTag.PART_START && !wasWillMiss && station.willMissStop()) {
                setNotificationText(new NotificationData(currentState, TextUtils.translate(keyNotificationConnectionEndangeredTitle), TextUtils.translate(keyNotificationConnectionEndangered,
                    station.getTrain().trainName(),
                    station.getTrain().scheduleTitle()
                )));
            }
        }
    }

    public Component getLastInfoText() {
        return lastInfoText;
    }

    public String lastNarratorText() {
        return lastNarratorText;
    }

    public NotificationData getLastNotification() {
        return lastNotification;
    }

    private void setState(State state) {
        this.currentState = state;
        onStateChange.values().forEach(x -> {            
            if (x.isPresent()) {
                x.get().accept(currentState);
            }
        });
    }

    private void setInfoText(Component text) {
        this.lastInfoText = text;
        onInfoTextChange.values().forEach(x -> {            
            if (x.isPresent()) {
                x.get().accept(text);
            }
        });
    }

    private void setNotificationText(NotificationData data) {
        this.lastNotification = data;
        onNotificationSend.values().forEach(x -> {            
            if (x.isPresent()) {
                x.get().accept(data);
            }
        });
    }

    private void setNarratorText(String text) {
        this.lastNarratorText = text;
        onNarratorAnnounce.values().forEach(x -> {            
            if (x.isPresent()) {
                x.get().accept(text);
            }
        });
    }
    

    private void nextStop() {
        if (!changeCurrentStation()) {
            return;
        }

        setState(State.WHILE_TRAVELING);
        
        onContinue.values().forEach(x -> {
            if (x.isPresent()) {
                x.get().accept(new ContinueData(currentState));
            }
        });        
    }

    private boolean changeCurrentStation() {
        if (stationIndex + 1 >= route.getStationCount(true)) {
            finishJourney();
            return false;
        }
        stationIndex++;
        return true;
    }    

    private void finishJourney() {
        Component text = TextUtils.translate(keyAfterJourney, route.getStationArray()[route.getStationCount(true) - 1].getStationName());
        setState(State.AFTER_JOURNEY);
        
        onFinishJourney.values().forEach(x -> {
            if (x.isPresent()) {
                x.get().accept(new FinishJourneyData(currentState, text));
            }
        });
        setInfoText(text);
        setNotificationText(new NotificationData(currentState, TextUtils.translate(keyNotificationJourneyCompletedTitle), TextUtils.translate(keyNotificationJourneyCompleted)));

        stop();
    }

    private void announceNextStop() {
        Component textA = TextUtils.empty();
        Component textB = TextUtils.empty();
        Component text;
        text = textA = TextUtils.translate(keyNextStop,
            currentStation().getStationName()
        );
        if (currentStation().getTag() == StationTag.PART_END && currentStation().getIndex() + 1 < route.getStationCount(true)) {
            Component transferText = textB = nextStation().get().getInfo().platform() == null || nextStation().get().getInfo().platform().isBlank() ?
            TextUtils.translate(keyTransfer,
                nextStation().get().getTrain().trainName(),
                nextStation().get().getTrain().scheduleTitle()
            ) :
            TextUtils.translate(keyTransferWithPlatform,
                nextStation().get().getTrain().trainName(),
                nextStation().get().getTrain().scheduleTitle(),
                nextStation().get().getInfo().platform()
            );
            text = TextUtils.concatWithStarChars(text, transferText);
            setState(State.BEFORE_TRANSFER);
            setNotificationText(new NotificationData(currentState, TextUtils.translate(keyNotificationTransferTitle), 
                nextStation().get().getInfo().platform() == null || nextStation().get().getInfo().platform().isBlank() ?
                TextUtils.translate(keyNotificationTransfer,
                    nextStation().get().getTrain().trainName(),
                    nextStation().get().getTrain().scheduleTitle()
                ) : 
                TextUtils.translate(keyNotificationTransferWithPlatform,
                    nextStation().get().getTrain().trainName(),
                    nextStation().get().getTrain().scheduleTitle(),
                    nextStation().get().getInfo().platform()
                )
            ));
        } else {
            setState(State.BEFORE_NEXT_STOP);
        }

        String narratorText = textA.getString() + ". " + textB.getString();
        boolean transfer = currentStation().getTag() == StationTag.PART_END && currentStation().getIndex() + 1 < route.getStationCount(true);
        
        final Component fText = text;
        onAnnounceNextStop.values().forEach(x -> {
            if (x.isPresent()) {
                x.get().accept(new AnnounceNextStopData(currentState, fText, narratorText, transfer));
            }
        });
        setInfoText(text);
        setNarratorText(narratorText);
    }

    private void reachNextStop() {
        Component text = TextUtils.text(currentStation().getStationName());        
        String narratorText = text.getString();
        
        setState(State.WHILE_NEXT_STOP);
        onReachNextStop.values().forEach(x -> {
                if (x.isPresent()) {
                    x.get().accept(new ReachNextStopData(currentState, text, narratorText, TransferState.NONE));
                }
        });
        
        setInfoText(text);
        setNarratorText(narratorText);
    }

    private void reachTransferStop() {
        Component text = nextStation().isPresent() ? (
            nextStation().get().getInfo().platform() == null || nextStation().get().getInfo().platform().isBlank() ?
            TextUtils.translate(keyTransfer,
                nextStation().get().getTrain().trainName(),
                nextStation().get().getTrain().scheduleTitle()
            ) :
            TextUtils.translate(keyTransferWithPlatform,
                nextStation().get().getTrain().trainName(),
                nextStation().get().getTrain().scheduleTitle(),
                nextStation().get().getInfo().platform()
            )
        ) : TextUtils.empty();
        String narratorText = text.getString();
        
        setState(State.WHILE_TRANSFER);
        changeCurrentStation();

        onReachNextStop.values().forEach(x -> {
            if (x.isPresent()) {
                x.get().accept(new ReachNextStopData(currentState, text, narratorText, TransferState.DEFAULT));
            }
        });
        setInfoText(text);
        setNarratorText(narratorText);
    }

    private void reachTransferStopConnectionMissed() {
        Component text = TextUtils.concatWithStarChars(
            TextUtils.text(currentStation().getStationName()),
            TextUtils.translate(keyConnectionMissedInfo)
        );
        String narratorText = "";

        setState(State.JOURNEY_INTERRUPTED);
        onReachNextStop.values().forEach(x -> {
            if (x.isPresent()) {
                x.get().accept(new ReachNextStopData(currentState, text, narratorText, TransferState.CONNECTION_MISSED));
            }
        });
        setInfoText(text);
        setNarratorText(narratorText);
        
        stop();
    }

    private void journeyInterrupt(StationEntry station) {
        Component text = TextUtils.translate(keyJourneyInterruptedTitle);
        Component desc = TextUtils.translate(keyJourneyInterrupted, station.getTrain().trainName());
        String narratorText = "";

        setState(State.JOURNEY_INTERRUPTED);
        onJourneyInterrupt.values().forEach(x -> {
            if (x.isPresent()) {
                x.get().accept(new JourneyInterruptData(currentState, text, desc, narratorText));
            }
        });
        setInfoText(text);
        setNarratorText(narratorText);
        
        stop();
    }

    public long getTransferTime(int index) {
        Optional<StationEntry> station = getEntryAt(index);
        Optional<StationEntry> nextStation = getEntryAt(index + 1);
        long transferTime = -1;
        if (station.isPresent() && nextStation.isPresent() && !nextStation.get().isDeparted()) {
            if (nextStation.get().getCurrentTime() + ModClientConfig.TRANSFER_TIME.get() < nextStation.get().getScheduleTime()) {
                transferTime = nextStation.get().getScheduleTime() - station.get().getScheduleTime();
            } else {
                transferTime = nextStation.get().getCurrentTime() - station.get().getCurrentTime();
            }
        }

        return transferTime;
    }




    public StationEntry currentStation() {
        return route.getStationArray()[stationIndex];
    }

    public Optional<StationEntry> nextStation() {
        return stationIndex + 1 < route.getStationCount(true) ? Optional.of(route.getStationArray()[stationIndex + 1]) : Optional.empty();
    }

    public Optional<StationEntry> previousSation() {
        return stationIndex > 0 ? Optional.of(route.getStationArray()[stationIndex - 1]) : Optional.empty();
    }

    public StationEntry firstStation() {
        return route.getStationArray()[0];
    }

    public StationEntry lastStation() {
        return route.getStationArray()[route.getStationCount(true) - 1];
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public long getTimeDifferenceForStation(int index) {        
        StationEntry station = route.getStationArray()[index];
        return station.getCurrentRefreshTime() + station.getCurrentTicks() - station.getRefreshTime() - station.getTicks();
    }    

    public int getIndex() {
        return stationIndex;
    }

    public SimpleRoute getListeningRoute() {
        return route;
    }
    
    public Optional<StationEntry> getEntryAt(int index) {
        return index >= 0 && index < route.getStationCount(true) ? Optional.of(route.getStationArray()[index]) : Optional.empty();
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
