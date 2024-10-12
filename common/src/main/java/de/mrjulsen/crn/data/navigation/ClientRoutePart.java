package de.mrjulsen.crn.data.navigation;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Queue;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.HashMap;

import com.google.common.collect.ImmutableList;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.train.ClientTrainStop;
import de.mrjulsen.crn.data.train.RoutePartProgressState;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.ClientTrainStop.TrainStopRealTimeData;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.util.IListenable;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.data.Single.MutableSingle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class ClientRoutePart extends RoutePart implements ITrainListenerClient<ClientRoutePart.TrainRealTimeData>, IListenable<ClientRoutePart.ListenerNotificationData> {

    public static record ListenerNotificationData(ClientRoutePart part, ClientTrainStop trainStop) {}
    public static record QueuedAnnouncementEvent(Runnable callback, ClientTrainStop trainStop) {}

    public static final String EVENT_UPDATE = "update";
    public static final String EVENT_ANNOUNCE_START = "announce_start";
    public static final String EVENT_ARRIVAL_AT_START = "arrival_at_start";
    public static final String EVENT_DEPARTURE_FROM_START = "departure_at_start";
    public static final String EVENT_WHILE_TRANSIT = "while_transit";
    public static final String EVENT_NEXT_STOP_ANNOUNCED = "next_stop_announced";
    public static final String EVENT_ARRIVAL_AT_STOPOVER = "arrival_at_stopover";
    public static final String EVENT_DEPARTURE_FROM_STOPOVER = "departure_at_stopover";
    public static final String EVENT_LAST_STOP_ANNOUNCED = "last_stop_announced";
    public static final String EVENT_ARRIVAL_AT_LAST_STOP = "arrival_at_last_stop";
    public static final String EVENT_DEPARTURE_FROM_LAST_STOP = "departure_at_last_stop";
    public static final String EVENT_FIRST_STOP_STATION_CHANGED = "first_stop_station_changed";
    public static final String EVENT_FIRST_STOP_DELAYED = "first_stop_delayed";
    public static final String EVENT_LAST_STOP_STATION_CHANGED = "last_stop_station_changed";
    public static final String EVENT_LAST_STOP_DELAYED = "last_stop_delayed";
    public static final String EVENT_ANY_STOP_ANNOUNCED = "any_stop_announced";
    public static final String EVENT_ARRIVAL_AT_ANY_STOP = "arrival_at_any_stop";
    public static final String EVENT_DEPARTURE_FROM_ANY_STOP = "departure_at_any_stop";
    public static final String EVENT_SCHEDULE_CHANGED = "schedule_changed";
    public static final String EVENT_TRAIN_CANCELLED = "train_cancelled";
    
    private final Map<String, IdentityHashMap<Object, Consumer<ListenerNotificationData>>> listeners = new HashMap<>();

    // state    
    private RoutePartProgressState progressState = RoutePartProgressState.BEFORE;
    private ClientTrainStop nextStop;
    private final Queue<QueuedAnnouncementEvent> queuedAnnouncements = new ConcurrentLinkedQueue<>();

    private final Cache<List<ClientTrainStop>> clientTrainStops = new Cache<>(() -> getAllStops().stream().filter(x -> x instanceof ClientTrainStop).map(x -> (ClientTrainStop)x).toList());
    private final Cache<List<ClientTrainStop>> clientJourneyStops = new Cache<>(() -> getAllJourneyStops().stream().filter(x -> x instanceof ClientTrainStop).map(x -> (ClientTrainStop)x).toList());


    public ClientRoutePart(UUID sessionId, UUID trainId, List<TrainStop> routeStops, List<TrainStop> allStops) {
        super(sessionId, trainId, routeStops, allStops);
        this.nextStop = getFirstClientStop();

        createEvent(EVENT_UPDATE);
        createEvent(EVENT_ANNOUNCE_START);
        createEvent(EVENT_ARRIVAL_AT_START);
        createEvent(EVENT_DEPARTURE_FROM_START);
        createEvent(EVENT_WHILE_TRANSIT);
        createEvent(EVENT_NEXT_STOP_ANNOUNCED);
        createEvent(EVENT_ARRIVAL_AT_STOPOVER);
        createEvent(EVENT_DEPARTURE_FROM_STOPOVER);
        createEvent(EVENT_LAST_STOP_ANNOUNCED);
        createEvent(EVENT_ARRIVAL_AT_LAST_STOP);
        createEvent(EVENT_DEPARTURE_FROM_LAST_STOP);
        createEvent(EVENT_FIRST_STOP_STATION_CHANGED);
        createEvent(EVENT_FIRST_STOP_DELAYED);
        createEvent(EVENT_LAST_STOP_STATION_CHANGED);
        createEvent(EVENT_LAST_STOP_DELAYED);
        createEvent(EVENT_ANY_STOP_ANNOUNCED);
        createEvent(EVENT_ARRIVAL_AT_ANY_STOP);
        createEvent(EVENT_DEPARTURE_FROM_ANY_STOP);
        createEvent(EVENT_SCHEDULE_CHANGED);
        createEvent(EVENT_TRAIN_CANCELLED);

        getFirstClientStop().listen(ClientTrainStop.EVENT_ANNOUNCE_NEXT_STOP, this, x -> {
            notifyListeners(EVENT_ANNOUNCE_START, new ListenerNotificationData(this, x));
        });
        getFirstClientStop().listen(ClientTrainStop.EVENT_STATION_REACHED, this, x -> {
            progressState = RoutePartProgressState.AT_START;
            notifyListeners(EVENT_ARRIVAL_AT_START, new ListenerNotificationData(this, x));
            notifyListeners(EVENT_ARRIVAL_AT_ANY_STOP, new ListenerNotificationData(this, x));
        });
        getFirstClientStop().listen(ClientTrainStop.EVENT_STATION_LEFT, this, x -> {
            progressState = RoutePartProgressState.TRAVELING;
            notifyListeners(EVENT_DEPARTURE_FROM_START, new ListenerNotificationData(this, x));
            notifyListeners(EVENT_DEPARTURE_FROM_ANY_STOP, new ListenerNotificationData(this, x));
        });
        getFirstClientStop().listen(ClientTrainStop.EVENT_DELAY, this, x -> {
            notifyListeners(EVENT_FIRST_STOP_DELAYED, new ListenerNotificationData(this, x));
        });
        getFirstClientStop().listen(ClientTrainStop.EVENT_STATION_CHANGED, this, x -> {
            notifyListeners(EVENT_FIRST_STOP_STATION_CHANGED, new ListenerNotificationData(this, x));
        });

        for (ClientTrainStop stop : getClientStopovers()) {
            stop.listen(ClientTrainStop.EVENT_ANNOUNCE_NEXT_STOP, this, x -> {
                queuedAnnouncements.add(new QueuedAnnouncementEvent(() -> {
                    progressState = RoutePartProgressState.NEXT_STOP_ANNOUNCED;
                    notifyListeners(EVENT_NEXT_STOP_ANNOUNCED, new ListenerNotificationData(this, x));
                    notifyListeners(EVENT_ANY_STOP_ANNOUNCED, new ListenerNotificationData(this, x));
                }, x));
            });
            stop.listen(ClientTrainStop.EVENT_STATION_REACHED, this, x -> {
                progressState = RoutePartProgressState.AT_STOPOVER;
                notifyListeners(EVENT_ARRIVAL_AT_STOPOVER, new ListenerNotificationData(this, x));
                notifyListeners(EVENT_ARRIVAL_AT_ANY_STOP, new ListenerNotificationData(this, x));
            });
            stop.listen(ClientTrainStop.EVENT_STATION_LEFT, this, x -> {
                progressState = RoutePartProgressState.TRAVELING;
                notifyListeners(EVENT_DEPARTURE_FROM_STOPOVER, new ListenerNotificationData(this, x));
                notifyListeners(EVENT_DEPARTURE_FROM_ANY_STOP, new ListenerNotificationData(this, x));
            });
        }

        getLastClientStop().listen(ClientTrainStop.EVENT_ANNOUNCE_NEXT_STOP, this, x -> {
            queuedAnnouncements.add(new QueuedAnnouncementEvent(() -> {
                progressState = RoutePartProgressState.END_ANNOUNCED;
                notifyListeners(EVENT_LAST_STOP_ANNOUNCED, new ListenerNotificationData(this, x));
                notifyListeners(EVENT_ANY_STOP_ANNOUNCED, new ListenerNotificationData(this, x));
            }, x));
        });
        getLastClientStop().listen(ClientTrainStop.EVENT_STATION_REACHED, this, x -> {
            progressState = RoutePartProgressState.AT_END;
            queuedAnnouncements.clear();
            notifyListeners(EVENT_ARRIVAL_AT_LAST_STOP, new ListenerNotificationData(this, x));
            notifyListeners(EVENT_ARRIVAL_AT_ANY_STOP, new ListenerNotificationData(this, x));
        });
        getLastClientStop().listen(ClientTrainStop.EVENT_STATION_LEFT, this, x -> {
            progressState = RoutePartProgressState.AFTER;
            queuedAnnouncements.clear();
            notifyListeners(EVENT_DEPARTURE_FROM_LAST_STOP, new ListenerNotificationData(this, x));
            notifyListeners(EVENT_DEPARTURE_FROM_ANY_STOP, new ListenerNotificationData(this, x));
            close();
        });
        getLastClientStop().listen(ClientTrainStop.EVENT_DELAY, this, x -> {
            notifyListeners(EVENT_LAST_STOP_DELAYED, new ListenerNotificationData(this, x));
        });
        getLastClientStop().listen(ClientTrainStop.EVENT_STATION_CHANGED, this, x -> {
            notifyListeners(EVENT_LAST_STOP_STATION_CHANGED, new ListenerNotificationData(this, x));
        });

        getAllClientStops().stream().forEach(x -> {
            x.listen(ClientTrainStop.EVENT_SCHEDULE_CHANGED, this, a -> {
                notifyListeners(EVENT_SCHEDULE_CHANGED, new ListenerNotificationData(this, a));
            });
        });


        // TEST
        listen(EVENT_DEPARTURE_FROM_ANY_STOP, this, (p) -> {
            int idx = routeStops.indexOf(p.trainStop());
            if (idx < 0 || idx >= routeStops.size() - 1) {
                nextStop = p.trainStop();
            } else {
                nextStop = (ClientTrainStop)routeStops.get(idx + 1);
            }
        });
    }

    @Override
    public Map<String, IdentityHashMap<Object, Consumer<ListenerNotificationData>>> getListeners() {
        return listeners;
    }

    public List<ClientTrainStop> getAllJourneyClientStops() {
        return clientJourneyStops.get();
    }

    public List<ClientTrainStop> getAllClientStops() {
        return clientTrainStops.get();
    }

    public List<ClientTrainStop> getClientStopovers() {
        return getAllClientStops().size() <= 2 ? List.of() : ImmutableList.copyOf(getAllClientStops().subList(1, routeStops.size() - 1));
    }

    public ClientTrainStop getFirstClientStop() {
        return getAllClientStops().get(0);
    }

    public ClientTrainStop getLastClientStop() {
        return getAllClientStops().get(getAllClientStops().size() - 1);
    }

    public ClientTrainStop getNextStop() {
        return nextStop;
    }

    public int getNextStopIndex() {
        return routeStops.indexOf(nextStop);
    }

    public RoutePartProgressState getProgressState() {
        return progressState;
    }

    @Override
    public void update(TrainRealTimeData data) {
        if (isCancelled()) {
            return;
        }

        status.clear();
        if (!data.sessionId().equals(getSessionId())) {
            cancelled = true;
        }

        MutableSingle<Boolean> shouldRenderStatus = new MutableSingle<>(false);

        getAllClientStops().stream().forEach(x -> {
            if (data.stationData().containsKey(x.getScheduleIndex())) {
                x.update(data.stationData().get(x.getScheduleIndex()));
                if (x.shouldRenderRealTime())  {
                    shouldRenderStatus.setFirst(true);
                }
            }
        });
        getAllJourneyClientStops().stream().forEach(x -> {
            if (data.stationData().containsKey(x.getScheduleIndex())) {
                x.update(data.stationData().get(x.getScheduleIndex()));
            }
        });

        if (shouldRenderStatus.getFirst() || data.cancelled()) {
            status.addAll(data.statusInfo());
        }

        this.cancelled = this.cancelled || data.cancelled();

        notifyListeners(EVENT_UPDATE, new ListenerNotificationData(this, nextStop));
        if (getProgressState() == RoutePartProgressState.TRAVELING) {
            notifyListeners(EVENT_WHILE_TRANSIT, new ListenerNotificationData(this, nextStop));

            while (!queuedAnnouncements.isEmpty()) {
                QueuedAnnouncementEvent event = queuedAnnouncements.peek();
                if (routeStops.indexOf(event.trainStop()) > getNextStopIndex()) {
                    break;
                }
                event = queuedAnnouncements.poll();
                if (getNextStop() != event.trainStop()) {
                    continue;
                }
                event.callback().run();
                break;
            }
        }

        if (isCancelled()) {
            CreateRailwaysNavigator.LOGGER.info("Train got cancelled. Closing route...");
            notifyListeners(EVENT_TRAIN_CANCELLED, new ListenerNotificationData(this, nextStop));
            close();
        }
    }

    public static RoutePart fromNbt(CompoundTag nbt) {
        return new ClientRoutePart(
            nbt.contains(NBT_SESSION_ID) ? nbt.getUUID(NBT_SESSION_ID) : new UUID(0, 0),
            nbt.getUUID(NBT_TRAIN_ID),
            nbt.getList(NBT_STOPS, Tag.TAG_COMPOUND).stream().map(x -> ClientTrainStop.fromNbt((CompoundTag)x)).toList(),
            nbt.getList(NBT_JOURNEY, Tag.TAG_COMPOUND).stream().map(x -> ClientTrainStop.fromNbt((CompoundTag)x)).toList()
        );
    }

    @Override
    public void close() {
        getAllClientStops().stream().forEach(x -> {
            x.stopListeningAll(this);
            x.close();
        });
        getAllJourneyClientStops().stream().forEach(x -> {
            x.stopListeningAll(this);
            x.close();
        });
        stopListeningAll(this);
    }


    public static record TrainRealTimeData(UUID sessionId, Map<Integer, TrainStopRealTimeData> stationData, Set<CompiledTrainStatus> statusInfo, boolean cancelled) {

        private static final String NBT_SESSION_ID = "SessionId";
        private static final String NBT_STATUS_INFOS = "Status";
        private static final String NBT_CANCELLED = "Cancelled";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putUUID(NBT_SESSION_ID, sessionId);
            ListTag status = new ListTag();
            status.addAll(statusInfo().stream().map(x -> x.toNbt()).toList());
            nbt.put(NBT_STATUS_INFOS, status);
            nbt.putBoolean(NBT_CANCELLED, cancelled);

            for (Map.Entry<Integer, TrainStopRealTimeData> e : stationData.entrySet()) {
                nbt.put("" + e.getKey(), e.getValue().toNbt());
            }
            return nbt;
        }

        public static TrainRealTimeData fromNbt(CompoundTag nbt) {
            return new TrainRealTimeData(
                nbt.getUUID(NBT_SESSION_ID),
                nbt.getAllKeys().stream().filter(x -> { try { Integer.parseInt(x); return true; } catch (Exception e) { return false; } }).collect(Collectors.toMap(x -> Integer.parseInt(x), x -> TrainStopRealTimeData.fromNbt(nbt.getCompound(x)))),
                nbt.getList(NBT_STATUS_INFOS, Tag.TAG_COMPOUND).stream().map(x -> CompiledTrainStatus.fromNbt((CompoundTag)x)).collect(Collectors.toSet()),
                nbt.getBoolean(NBT_CANCELLED)
            );
        }
    }
}
