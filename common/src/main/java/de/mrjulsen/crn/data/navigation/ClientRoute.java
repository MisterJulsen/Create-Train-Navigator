package de.mrjulsen.crn.data.navigation;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import java.lang.StringBuilder;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.data.SavedRoutesManager;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.train.RoutePartProgressState;
import de.mrjulsen.crn.data.train.RouteProgressState;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.events.DefaultTrainDataRefreshEvent;
import de.mrjulsen.crn.util.IListenable;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

public class ClientRoute extends Route implements AutoCloseable, IListenable<ClientRoute.ListenerNotificationData> {

    public static record ListenerNotificationData(ClientRoute route, ClientRoutePart part, ClientTrainStop trainStop, TransferConnection connection) {}
    public static record QueuedAnnouncementEvent(Runnable callback, ClientRoutePart part, ClientTrainStop trainStop) {}

    /** Called every time the real-time data updates. */
    public static final String EVENT_UPDATE = "update";

    /** Called when announcing the start of the journey. */
    public static final String EVENT_ANNOUNCE_START = "announce_start";
    /** Called when arriving at the first station of the journey. */
    public static final String EVENT_ARRIVAL_AT_START = "arrival_at_start";
    /** Called when departing from the first station of the journey. */
    public static final String EVENT_DEPARTURE_FROM_START = "departure_from_start";

    /** Called while travelling between two stations. */
    public static final String EVENT_WHILE_TRANSIT = "while_transit";

    /** Called when announcing a stopover station with no special role on the route. */
    public static final String EVENT_ANNOUNCE_STOPOVER = "announce_stopover";
    /** Called when arriving at a stopover station with no special role on the route. */
    public static final String EVENT_ARRIVAL_AT_STOPOVER = "arrival_at_stopover";
    /** Called when departing from a stopover station with no special role on the route. */
    public static final String EVENT_DEPARTURE_AT_STOPOVER = "departure_from_stopover";
    
    /** Called when announcing the arrival at a transfer station. */
    public static final String EVENT_ANNOUNCE_TRANSFER_ARRIVAL_STATION = "announce_transfer_arrival_station";
    /** Called when arraving at a transfer station. */
    public static final String EVENT_ARRIVAL_AT_TRANSFER_ARRIVAL_STATION = "arrival_at_transfer_arrival_station";
    /** Called when the train that brought you to the transfer station departs. */
    public static final String EVENT_DEPARTURE_FROM_TRANSFER_ARRIVAL_STATION = "departure_from_transfer_arrival_station";
    /** Called while waiting for the connecting train. */
    public static final String EVENT_WHILE_TRANSFER = "while_transfer";
    /** Called when the connecting train announces arrival at a transfer station. */
    public static final String EVENT_ANNOUNCE_TRANSFER_DEPARTURE_STATION = "announce_transfer_departure_station";
    /** Called when the connecting train arrives at a transfer station. */
    public static final String EVENT_ARRIVAL_AT_TRANSFER_DEPARTURE_STATION = "arrival_at_transfer_departure_station";
    /** Called when the connecting train departs at a transfer station. */
    public static final String EVENT_DEPARTURE_FROM_TRANSFER_DEPARTURE_STATION = "departure_from_transfer_departure_station";

    /** Called when announcing the end of the journey. */
    public static final String EVENT_ANNOUNCE_LAST_STOP = "announce_last_stop";
    /** Called when arriving at the last station of the journey. */
    public static final String EVENT_ARRIVAL_AT_LAST_STOP = "arrival_at_last_stop";
    /** Called when departing from the last station of the journey. */
    public static final String EVENT_DEPARTURE_FROM_LAST_STOP = "departure_from_last_stop";

    /** Called when the first stop changes. */
    public static final String EVENT_FIRST_STOP_STATION_CHANGED = "first_stop_station_changed";
    /** Called when the first stop is delayed. */
    public static final String EVENT_FIRST_STOP_DELAYED = "first_stop_delayed";
    
    /** Called when the transfer arrival station changes. */
    public static final String EVENT_TRANSFER_ARRIVAL_STATION_CHANGED = "transfer_arrival_station_changed";
    /** Called when the transfer arrival station is delayed. */
    public static final String EVENT_TRANSFER_ARRIVAL_DELAYED = "transfer_arrival_delayed";    
    /** Called when the transfer departure station changes. */
    public static final String EVENT_TRANSFER_DEPARTURE_STATION_CHANGED = "transfer_departure_station_changed";
    /** Called when the transfer departure station is delayed. */
    public static final String EVENT_TRANSFER_DEPARTURE_DELAYED = "transfer_departure_delayed";

    /** Called when the last stop changes. */
    public static final String EVENT_LAST_STOP_STATION_CHANGED = "last_stop_station_changed";
    /** Called when the last stop is delayed. */
    public static final String EVENT_LAST_STOP_DELAYED = "last_stop_delayed";

    /** Called when any stop is announced. */
    public static final String EVENT_ANY_STOP_ANNOUNCED = "any_stop_announced";
    /** Called when arriving at any stop. */
    public static final String EVENT_ARRIVAL_AT_ANY_STOP = "arrival_at_any_stop";
    /** Called when departing from any stop. */
    public static final String EVENT_DEPARTURE_FROM_ANY_STOP = "departure_from_any_stop";
    /** Called when announcing any important station (start, transfers and end) */
    public static final String EVENT_ANNOUNCE_ANY_IMPORTANT_STATION = "announce_any_important_station";
    /** Called when arriving at any important station (start, transfers and end) */
    public static final String EVENT_ARRIVAL_AT_ANY_IMPORTANT_STATION = "arrival_at_any_important_station";
    /** Called when departing from any important station (start, transfers and end) */
    public static final String EVENT_DEPARTURE_FROM_ANY_IMPORTANT_STATION = "departure_from_any_important_station";
    /** Called when any important station (start, transfers and end) reports delays. */
    public static final String EVENT_ANY_STATION_DELAYED = "any_station_delayed";
    /** Called when any important station (start, transfers and end) reports changes. */
    public static final String EVENT_ANY_STATION_CHANGED = "any_station_changed";

    /** Called when a transfer connection is endangered. */
    public static final String EVENT_ANY_TRANSFER_ENDANGERED = "any_transfer_endangered";
    /** Called when a transfer connection is missed. */
    public static final String EVENT_ANY_TRANSFER_MISSED = "any_transfer_missed";
    /** Called when the route part changes. */
    public static final String EVENT_PART_CHANGED = "part_changed";
    public static final String EVENT_SCHEDULE_CHANGED = "schedule_changed";
    public static final String EVENT_ANY_TRAIN_CANCELLED = "train_cancelled";


    // Texts    
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
    private static final String keyNotificationConnectionCanceledTitle = "gui.createrailwaysnavigator.route_overview.connection_cancelled";
    private static final String keyNotificationConnectionCanceled = "gui.createrailwaysnavigator.route_overview.journey_interrupted";

    private final Map<String, IdentityHashMap<Object, Consumer<ListenerNotificationData>>> listeners = new HashMap<>();
    private final Map<NotificationType, Collection<Runnable>> queuedNotifications = new HashMap<>();

    private final long id = System.nanoTime();
    private final Map<UUID, ClientRoutePart> listenerIds = new HashMap<>();
    private int listenersCount = 0;

    private boolean isClosed;

    // State
    private RouteProgressState progressState = RouteProgressState.BEFORE;
    private final Queue<QueuedAnnouncementEvent> queuedAnnouncements = new ConcurrentLinkedQueue<>();
    private ClientRoutePart currentPart;
    private int currentPartIndex;
    private final Cache<List<ClientRoutePart>> clientParts = new Cache<>(() -> getParts().stream().filter(x -> x instanceof ClientRoutePart).map(x -> (ClientRoutePart)x).toList());

    // User settings
    private boolean savedRouteRemoved = false;
    private boolean showNotifications = false;
    
    // spam blocker
    private boolean stationChangedSent = false;
    private boolean scheduleChangedSent = false;
    private boolean stationDelayedSent = false;
    private boolean connectionWarningSent = false;
    private boolean cancelledSent = false;

    private void resetSpamBlockers() {
        stationChangedSent = false;
        scheduleChangedSent = false;
        connectionWarningSent = false;
        stationDelayedSent = false;
        cancelledSent = false;
    }

    public ClientRoute(List<RoutePart> parts, boolean realTimeTracker) {
        super(parts, realTimeTracker);
        this.currentPart = getFirstClientPart();

        if (!realTimeTracker) return;
        getClientParts().stream().forEach(x -> listenerIds.put(ClientTrainListener.register(x.getSessionId(), x.getTrainId(), x::update), x));
        CRNEventsManager.getEvent(DefaultTrainDataRefreshEvent.class).register(CreateRailwaysNavigator.MOD_ID + "_" + id, this::update);
        addListener();
        
        createEvent(EVENT_UPDATE);
        createEvent(EVENT_ANNOUNCE_START);
        createEvent(EVENT_ARRIVAL_AT_START);
        createEvent(EVENT_DEPARTURE_FROM_START);
        createEvent(EVENT_WHILE_TRANSIT);
        createEvent(EVENT_ANNOUNCE_STOPOVER);
        createEvent(EVENT_ARRIVAL_AT_STOPOVER);
        createEvent(EVENT_DEPARTURE_AT_STOPOVER);
        createEvent(EVENT_ANNOUNCE_TRANSFER_ARRIVAL_STATION);
        createEvent(EVENT_ARRIVAL_AT_TRANSFER_ARRIVAL_STATION);
        createEvent(EVENT_DEPARTURE_FROM_TRANSFER_ARRIVAL_STATION);
        createEvent(EVENT_WHILE_TRANSFER);
        createEvent(EVENT_ANNOUNCE_TRANSFER_DEPARTURE_STATION);
        createEvent(EVENT_ARRIVAL_AT_TRANSFER_DEPARTURE_STATION);
        createEvent(EVENT_DEPARTURE_FROM_TRANSFER_DEPARTURE_STATION);
        createEvent(EVENT_ANNOUNCE_LAST_STOP);
        createEvent(EVENT_ARRIVAL_AT_LAST_STOP);
        createEvent(EVENT_DEPARTURE_FROM_LAST_STOP);
        createEvent(EVENT_FIRST_STOP_STATION_CHANGED);
        createEvent(EVENT_FIRST_STOP_DELAYED);
        createEvent(EVENT_TRANSFER_ARRIVAL_STATION_CHANGED);
        createEvent(EVENT_TRANSFER_ARRIVAL_DELAYED);
        createEvent(EVENT_TRANSFER_DEPARTURE_STATION_CHANGED);
        createEvent(EVENT_TRANSFER_DEPARTURE_DELAYED);
        createEvent(EVENT_LAST_STOP_STATION_CHANGED);
        createEvent(EVENT_LAST_STOP_DELAYED);
        createEvent(EVENT_ANY_STOP_ANNOUNCED);
        createEvent(EVENT_ARRIVAL_AT_ANY_STOP);
        createEvent(EVENT_DEPARTURE_FROM_ANY_STOP);
        createEvent(EVENT_ANNOUNCE_ANY_IMPORTANT_STATION);
        createEvent(EVENT_ARRIVAL_AT_ANY_IMPORTANT_STATION);
        createEvent(EVENT_DEPARTURE_FROM_ANY_IMPORTANT_STATION);
        createEvent(EVENT_ANY_STATION_DELAYED);
        createEvent(EVENT_ANY_STATION_CHANGED);
        createEvent(EVENT_ANY_TRANSFER_ENDANGERED);
        createEvent(EVENT_ANY_TRANSFER_MISSED);
        createEvent(EVENT_PART_CHANGED);
        createEvent(EVENT_SCHEDULE_CHANGED);
        createEvent(EVENT_ANY_TRAIN_CANCELLED);


        getFirstClientPart().listen(ClientRoutePart.EVENT_ANNOUNCE_START, this, x -> {
            if (currentPartIndex > 0) return;
            
            sendNotification(
                ELanguage.translate(keyNotificationJourneyBeginsTitle, getEnd().getClientTag().tagName()),
                getStart().getRealTimeStationTag().info().isPlatformKnown() ?
                    ELanguage.translate(keyNotificationJourneyBeginsWithPlatform, getStart().getTrainDisplayName(), getStart().getDisplayTitle(), ModUtils.formatTime(getStart().getScheduledDepartureTime(), false), getStart().getRealTimeStationTag().info().platform()) :
                    ELanguage.translate(keyNotificationJourneyBegins, getStart().getTrainDisplayName(), getStart().getDisplayTitle(), ModUtils.formatTime(getStart().getScheduledDepartureTime(), false))
            );

            queuedAnnouncements.add(new QueuedAnnouncementEvent(() -> {
                notifyListeners(EVENT_ANY_STOP_ANNOUNCED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                notifyListeners(EVENT_ANNOUNCE_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                notifyListeners(EVENT_ANNOUNCE_START, new ListenerNotificationData(this, x.part(), x.trainStop(), null));

            }, x.part(), x.trainStop()));
        });
        getFirstClientPart().listen(ClientRoutePart.EVENT_ARRIVAL_AT_START, this, x -> {
            if (currentPartIndex != 0) return;
            this.progressState = RouteProgressState.AT_START;
            notifyListeners(EVENT_ARRIVAL_AT_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_ARRIVAL_AT_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_ARRIVAL_AT_START, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
        });
        getFirstClientPart().listen(ClientRoutePart.EVENT_DEPARTURE_FROM_START, this, x -> {
            if (currentPartIndex != 0) return;
            this.progressState = RouteProgressState.TRAVELING;
            notifyListeners(EVENT_DEPARTURE_FROM_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_DEPARTURE_FROM_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_DEPARTURE_FROM_START, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
        });
        getFirstClientPart().listen(ClientRoutePart.EVENT_FIRST_STOP_STATION_CHANGED, this, x -> {
            if (currentPartIndex > 0) return;
            notifyListeners(EVENT_ANY_STATION_CHANGED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_FIRST_STOP_STATION_CHANGED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
        });
        getFirstClientPart().listen(ClientRoutePart.EVENT_FIRST_STOP_DELAYED, this, x -> {
            if (currentPartIndex > 0) return;
            notifyListeners(EVENT_ANY_STATION_DELAYED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_FIRST_STOP_DELAYED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
        });

        for (int i = 0; i < getParts().size(); i++) {
            ClientRoutePart part = getClientParts().get(i);
            final int k = i;

            if (i > 0) {
                part.listen(ClientRoutePart.EVENT_ANNOUNCE_START, this, x -> {
                    if (currentPartIndex > k) return;
                    queuedAnnouncements.add(new QueuedAnnouncementEvent(() -> {
                        notifyListeners(EVENT_ANY_STOP_ANNOUNCED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                        notifyListeners(EVENT_ANNOUNCE_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                        notifyListeners(EVENT_ANNOUNCE_TRANSFER_DEPARTURE_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                    }, x.part(), x.trainStop()));
                });
                part.listen(ClientRoutePart.EVENT_ARRIVAL_AT_START, this, x -> {
                    if (currentPartIndex != k) return;
                    this.progressState = RouteProgressState.BEFORE_CONTINUATION;
                    notifyListeners(EVENT_ARRIVAL_AT_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_ARRIVAL_AT_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_ARRIVAL_AT_TRANSFER_DEPARTURE_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                });
                part.listen(ClientRoutePart.EVENT_DEPARTURE_FROM_START, this, x -> {
                    if (currentPartIndex != k) return;
                    this.progressState = RouteProgressState.TRAVELING;
                    notifyListeners(EVENT_DEPARTURE_FROM_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_DEPARTURE_FROM_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_DEPARTURE_FROM_TRANSFER_DEPARTURE_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                });
                part.listen(ClientRoutePart.EVENT_FIRST_STOP_STATION_CHANGED, this, x -> {
                    if (currentPartIndex > k) return;
                    notifyListeners(EVENT_ANY_STATION_CHANGED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_TRANSFER_DEPARTURE_STATION_CHANGED, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                });
                part.listen(ClientRoutePart.EVENT_FIRST_STOP_DELAYED, this, x -> {
                    if (currentPartIndex > k) return;
                    notifyListeners(EVENT_ANY_STATION_DELAYED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_TRANSFER_DEPARTURE_DELAYED, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                });
            }

            part.listen(ClientRoutePart.EVENT_NEXT_STOP_ANNOUNCED, this, x -> {
                if (currentPartIndex > k) return;
                queuedAnnouncements.add(new QueuedAnnouncementEvent(() -> {
                    this.progressState = RouteProgressState.NEXT_STOP_ANNOUNCED;
                    notifyListeners(EVENT_ANY_STOP_ANNOUNCED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_ANNOUNCE_STOPOVER, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                }, x.part(), x.trainStop()));
            });
            part.listen(ClientRoutePart.EVENT_ARRIVAL_AT_STOPOVER, this, x -> {
                if (currentPartIndex != k) return;
                this.progressState = RouteProgressState.AT_STOPOVER;
                notifyListeners(EVENT_ARRIVAL_AT_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                notifyListeners(EVENT_ARRIVAL_AT_STOPOVER, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            });
            part.listen(ClientRoutePart.EVENT_DEPARTURE_FROM_STOPOVER, this, x -> {
                if (currentPartIndex != k) return;
                this.progressState = RouteProgressState.TRAVELING;
                notifyListeners(EVENT_DEPARTURE_FROM_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                notifyListeners(EVENT_DEPARTURE_AT_STOPOVER, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            });

            part.listen(ClientRoutePart.EVENT_SCHEDULE_CHANGED, this, x -> {
                if (scheduleChangedSent) return;
                sendNotification(ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.notification.schedule_changed.title"), ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_overview.notification.schedule_changed"));
                notifyListeners(EVENT_SCHEDULE_CHANGED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                scheduleChangedSent = true;
            });

            part.listen(ClientRoutePart.EVENT_TRAIN_CANCELLED, this, x -> {
                if (cancelledSent) return;
                sendNotification(ELanguage.translate(keyNotificationConnectionCanceledTitle), ELanguage.translate(keyNotificationConnectionCanceled, x.part().getFirstStop().getTrainDisplayName()));
                notifyListeners(EVENT_ANY_TRAIN_CANCELLED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                cancelledSent = true;
            });

            if (i < parts.size() - 1) {
                part.listen(ClientRoutePart.EVENT_LAST_STOP_ANNOUNCED, this, x -> {
                    if (currentPartIndex > k) return;

                    queuedAnnouncements.add(new QueuedAnnouncementEvent(() -> {
                        this.progressState = RouteProgressState.TRANSFER_ANNOUNCED;
                        notifyListeners(EVENT_ANY_STOP_ANNOUNCED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                        notifyListeners(EVENT_ANNOUNCE_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                        notifyListeners(EVENT_ANNOUNCE_TRANSFER_ARRIVAL_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                    }, x.part(), x.trainStop()));
                });
                part.listen(ClientRoutePart.EVENT_ARRIVAL_AT_LAST_STOP, this, x -> {
                    if (currentPartIndex != k) return;
                    this.progressState = RouteProgressState.AT_TRANSFER;
                    notifyListeners(EVENT_ARRIVAL_AT_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_ARRIVAL_AT_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_ARRIVAL_AT_TRANSFER_ARRIVAL_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                });
                part.listen(ClientRoutePart.EVENT_DEPARTURE_FROM_LAST_STOP, this, x -> {
                    if (currentPartIndex != k) return;
                    this.progressState = RouteProgressState.WHILE_TRANSFER;
                    notifyListeners(EVENT_DEPARTURE_FROM_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_DEPARTURE_FROM_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_DEPARTURE_FROM_TRANSFER_ARRIVAL_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                });
                part.listen(ClientRoutePart.EVENT_LAST_STOP_STATION_CHANGED, this, x -> {
                    if (currentPartIndex > k) return;
                    notifyListeners(EVENT_ANY_STATION_CHANGED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_TRANSFER_ARRIVAL_STATION_CHANGED, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                });
                part.listen(ClientRoutePart.EVENT_LAST_STOP_DELAYED, this, x -> {
                    if (currentPartIndex > k) return;
                    notifyListeners(EVENT_ANY_STATION_DELAYED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                    notifyListeners(EVENT_TRANSFER_ARRIVAL_DELAYED, new ListenerNotificationData(this, x.part(), x.trainStop(), getConnectionWith(x.trainStop()).orElse(null)));
                });
            }
        }
        
        getLastClientPart().listen(ClientRoutePart.EVENT_LAST_STOP_ANNOUNCED, this, x -> {
            if (currentPartIndex > parts.size() - 1) return;
                queuedAnnouncements.add(new QueuedAnnouncementEvent(() -> {                
                this.progressState = RouteProgressState.END_ANNOUNCED;
                notifyListeners(EVENT_ANY_STOP_ANNOUNCED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                notifyListeners(EVENT_ANNOUNCE_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
                notifyListeners(EVENT_ANNOUNCE_LAST_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            }, x.part(), x.trainStop()));
        });
        getLastClientPart().listen(ClientRoutePart.EVENT_ARRIVAL_AT_LAST_STOP, this, x -> {
            if (currentPartIndex != parts.size() - 1) return;
            this.progressState = RouteProgressState.AT_END;
            notifyListeners(EVENT_ARRIVAL_AT_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_ARRIVAL_AT_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_ARRIVAL_AT_LAST_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
        });
        getLastClientPart().listen(ClientRoutePart.EVENT_DEPARTURE_FROM_LAST_STOP, this, x -> {
            if (currentPartIndex < parts.size() - 1) return;
            this.progressState = RouteProgressState.AFTER;
            sendNotification(ELanguage.translate(keyNotificationJourneyCompletedTitle), ELanguage.translate(keyNotificationJourneyCompleted));
            if (!savedRouteRemoved) {
                savedRouteRemoved = true;
                SavedRoutesManager.removeRoute(this);
                SavedRoutesManager.push(true, null);
            }
            queuedAnnouncements.clear();
            notifyListeners(EVENT_DEPARTURE_FROM_ANY_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_DEPARTURE_FROM_ANY_IMPORTANT_STATION, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_DEPARTURE_FROM_LAST_STOP, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
        });
        getLastClientPart().listen(ClientRoutePart.EVENT_LAST_STOP_STATION_CHANGED, this, x -> {
            if (currentPartIndex > parts.size() - 1) return;
            notifyListeners(EVENT_ANY_STATION_CHANGED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_LAST_STOP_STATION_CHANGED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
        });
        getLastClientPart().listen(ClientRoutePart.EVENT_LAST_STOP_DELAYED, this, x -> {
            if (currentPartIndex > parts.size() - 1) return;
            notifyListeners(EVENT_ANY_STATION_DELAYED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
            notifyListeners(EVENT_LAST_STOP_DELAYED, new ListenerNotificationData(this, x.part(), x.trainStop(), null));
        });

        for (TransferConnection connection : getConnections()) {
            connection.listen(TransferConnection.EVENT_CONNECTION_ENDANGERED, this, x -> {
                if (connectionWarningSent) return;
                queueConnectionEndangeredNotification(x);
                notifyListeners(EVENT_ANY_TRANSFER_ENDANGERED, new ListenerNotificationData(this, null, null, x));
                connectionWarningSent = true;
            });
            connection.listen(TransferConnection.EVENT_CONNECTION_MISSED, this, x -> {
                if (connectionWarningSent) return;
                queueConnectionMissedNotification(x);
                notifyListeners(EVENT_ANY_TRANSFER_MISSED, new ListenerNotificationData(this, null, null, x));
                closeAll();
                connectionWarningSent = true;
            });
        }

        listen(EVENT_DEPARTURE_FROM_TRANSFER_ARRIVAL_STATION, this, (p) -> {
            int idx = parts.indexOf(p.part());
            if (idx >= 0) {
                if (idx < parts.size() - 1) {
                    currentPart = getClientParts().get(idx + 1);
                    currentPartIndex = idx + 1;
                    ClientRoutePart part = getClientParts().get(currentPartIndex);
                    notifyListeners(EVENT_PART_CHANGED, new ListenerNotificationData(this, part, part.getFirstClientStop(), p.connection()));

                    if (part.getProgressState() != RoutePartProgressState.BEFORE && part.getProgressState() != RoutePartProgressState.AT_START) {
                        queueConnectionMissedNotification(p.connection());
                        notifyListeners(EVENT_ANY_TRANSFER_MISSED, new ListenerNotificationData(this, null, null, p.connection()));
                        closeAll();
                    }
                    return;
                }
            }
            currentPart = p.part();
            currentPartIndex = idx;
        });

        listen(EVENT_ANY_STATION_CHANGED, this, (p) -> {
            if (stationChangedSent) return;
            sendNotification(ELanguage.translate(keyNotificationPlatformChangedTitle), ELanguage.translate(keyNotificationPlatformChanged,
                p.trainStop().getTrainDisplayName(),
                p.trainStop().getRealTimeStationTag().info().platform()
            ));
            stationChangedSent = true;
        });

        listen(EVENT_ANY_STATION_DELAYED, this, (p) -> {
            if (stationDelayedSent) return;
            queueDelayNotification(p.trainStop(), p.part().getFirstStop() == p.trainStop());
            stationDelayedSent = true;
        });

        listen(EVENT_ANNOUNCE_TRANSFER_ARRIVAL_STATION, this, (p) -> {
            sendNotification(ELanguage.translate(keyNotificationTransferTitle), getStart().getRealTimeStationTag().info().isPlatformKnown() ? ELanguage.translate(keyNotificationTransferWithPlatform,
                    p.connection().getDepartureStation().getTrainDisplayName(),
                    p.connection().getDepartureStation().getDisplayTitle(),
                    p.connection().getDepartureStation().getRealTimeStationTag().info().platform()
                ) : ELanguage.translate(keyNotificationTransfer,
                    p.connection().getDepartureStation().getTrainDisplayName(),
                    p.connection().getDepartureStation().getDisplayTitle()
                )
            );
        });
    }

    private void sendNotification(Component title, Component description) {
        if (shouldShowNotifications()) {
            ClientWrapper.sendCRNNotification(title, description);
        }
    }

    private void queueDelayNotification(ClientTrainStop stop, boolean start) {
        if (shouldShowNotifications()) {
            ClientWrapper.sendCRNNotification(
                ELanguage.translate(keyNotificationTrainDelayedTitle, stop.getTrainDisplayName(), TimeUtils.parseDurationShort((int)(start ? stop.getDepartureTimeDeviation() : stop.getArrivalTimeDeviation()))),
                ELanguage.translate(keyNotificationTrainDelayed,
                ModUtils.formatTime(start ? stop.getRoundedRealTimeDepartureTime() : stop.getRoundedRealTimeArrivalTime(), false),
                ModUtils.formatTime(start ? stop.getScheduledDepartureTime() : stop.getScheduledArrivalTime(), false),
                stop.getClientTag().tagName()
            ));
        }
    }

    private void queueConnectionEndangeredNotification(TransferConnection connection) {
        if (shouldShowNotifications()) {
            ClientWrapper.sendCRNNotification(ELanguage.translate(keyNotificationConnectionEndangeredTitle), ELanguage.translate(keyNotificationConnectionEndangered, connection.getDepartureStation().getTrainDisplayName(), connection.getDepartureStation().getDisplayTitle()));
        }
    }

    private void queueConnectionMissedNotification(TransferConnection connection) {
        if (shouldShowNotifications()) {
            ClientWrapper.sendCRNNotification(ELanguage.translate(keyNotificationConnectionMissedTitle), ELanguage.translate(keyNotificationConnectionMissed, connection.getDepartureStation().getTrainDisplayName(), connection.getDepartureStation().getDisplayTitle()));
        }
    }

    public static ClientRoute empty(boolean realTimeTracker) {
        return new ClientRoute(List.of(), realTimeTracker);
    }

    public RouteProgressState getState() {
        return progressState;
    }

    @Override
    public Map<String, IdentityHashMap<Object, Consumer<ListenerNotificationData>>> getListeners() {
        return listeners;
    }

    public boolean shouldShowNotifications() {
        return showNotifications;
    }

    public void setShowNotifications(boolean b) {
        this.showNotifications = b;
    }

    public List<ClientRoutePart> getClientParts() {
        return clientParts.get();
    }

    public ClientRoutePart getFirstClientPart() {
        return getClientParts().get(0);
    }

    public ClientRoutePart getLastClientPart() {
        return getClientParts().get(getClientParts().size() - 1);
    }
/*
    public TrainStop getStart() {
        return getFirstPart().getFirstStop();
    }

    public TrainStop getEnd() {
        return getLastPart().getLastStop();
    }

    public ImmutableList<ClientRoutePart> getParts() {
        return ImmutableList.copyOf(parts);
    }
    */

    public ClientRoutePart getCurrentPart() {
        return currentPart;
    }

    public int getCurrentPartIndex() {
        return parts.indexOf(currentPart);
    }

    public void update() {
        if (isClosed) {
            return;
        }

        if (isAnyCancelled()) {
            closeAll();
            return;
        }

        notifyListeners(EVENT_UPDATE, new ListenerNotificationData(this, getCurrentPart(), getCurrentPart().getNextStop(), null));
        if (getState() == RouteProgressState.TRAVELING) {
            notifyListeners(EVENT_WHILE_TRANSIT, new ListenerNotificationData(this, getCurrentPart(), getCurrentPart().getNextStop(), null));
        } else if (getState() == RouteProgressState.WHILE_TRANSFER) {
            notifyListeners(EVENT_WHILE_TRANSFER, new ListenerNotificationData(this, getCurrentPart(), getCurrentPart().getNextStop(), null));
        }
        
        if (getState() == RouteProgressState.TRAVELING || getState() == RouteProgressState.WHILE_TRANSFER) {
            while (!queuedAnnouncements.isEmpty()) {
                QueuedAnnouncementEvent event = queuedAnnouncements.poll();
                if (getCurrentPart().getNextStop() != event.trainStop() || getCurrentPart() != event.part()) {
                    continue;
                }
                event.callback().run();
                break;
            }
        }

        getConnections().stream().forEach(x -> x.update());

        // process notifications
        queuedNotifications.entrySet().forEach(x -> {
            if (x.getValue().isEmpty()) {
                return;
            }
        });
        queuedNotifications.clear();
        isCancelled.clear();
        resetSpamBlockers();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ROUTE[" + getStart().getClientTag().tagName() + " -> " + getEnd().getClientTag().tagName() + "]");
        return builder.toString();
    }

    public void addListener() {        
        listenersCount++;
    }

    @Override
    public void close() {
        listenersCount--;
        if (listenersCount <= 0) {
            closeAll();
        }
    }
    
    public void closeAll() {
        listenersCount = 0;
        listenerIds.entrySet().stream().forEach(x -> ClientTrainListener.unregister(x.getValue().getTrainId(), x.getKey()));
        getClientParts().stream().forEach(x -> {
            x.stopListeningAll(this);
            x.close();
        });
        getConnections().stream().forEach(x -> {
            x.stopListeningAll(this);
            x.close();
        });
        stopListeningAll(this);
        CRNEventsManager.getEvent(DefaultTrainDataRefreshEvent.class).unregister(CreateRailwaysNavigator.MOD_ID + "_" + id);
        clearEvents();
        isClosed = true;
        CreateRailwaysNavigator.LOGGER.info("Route listener closed.");
    }

    public static ClientRoute fromNbt(CompoundTag nbt, boolean realTimeTracker) {
        return new ClientRoute(
            nbt.getList(NBT_PARTS, Tag.TAG_COMPOUND).stream().map(x -> ClientRoutePart.fromNbt((CompoundTag)x)).toList(),
            realTimeTracker
        );
    }

    @Override
    public long timeOrderValue() {
        return getStart().getScheduledDepartureTime();
    }

    private static enum NotificationType {
        DELAY,
        CONNECTION_ENDANGERED,
        CONNECTION_MISSED
    }

    public boolean isClosed() {
        return isClosed;
    }
}
