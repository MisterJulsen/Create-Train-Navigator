package de.mrjulsen.crn.network.packets.pain;

import de.mrjulsen.crn.api.web.StationStopTracker;
import de.mrjulsen.crn.api.web.StationStopWebApi;
import de.mrjulsen.crn.api.web.TrainScheduleCache;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public class ShowWebApiScreenPacketData extends NetworkPacketData {

    private static final String NBT_ENABLED = "Enabled";
    private static final String NBT_RUNNING = "Running";
    private static final String NBT_BASE_URL = "BaseUrl";
    private static final String NBT_BIND = "Bind";
    private static final String NBT_PORT = "Port";
    private static final String NBT_ACTIVE_STOPS = "ActiveStops";
    private static final String NBT_TRACKED_TRAINS = "TrackedTrains";
    private static final String NBT_BUFFERED_EVENTS = "BufferedEvents";
    private static final String NBT_MAX_EVENTS = "MaxEvents";
    private static final String NBT_LATEST_EVENT_ID = "LatestEventId";

    private boolean enabled;
    private boolean running;
    private String baseUrl = "";
    private String bindAddress = "";
    private int port;
    private int activeStops;
    private int trackedTrains;
    private int bufferedEvents;
    private int maxEvents;
    private long latestEventId;

    public ShowWebApiScreenPacketData(DLStatus status) {
        super(status);
    }

    public ShowWebApiScreenPacketData(
        boolean enabled,
        boolean running,
        String baseUrl,
        String bindAddress,
        int port,
        int activeStops,
        int trackedTrains,
        int bufferedEvents,
        int maxEvents,
        long latestEventId
    ) {
        super(DLStatus.OK);
        this.enabled = enabled;
        this.running = running;
        this.baseUrl = baseUrl;
        this.bindAddress = bindAddress;
        this.port = port;
        this.activeStops = activeStops;
        this.trackedTrains = trackedTrains;
        this.bufferedEvents = bufferedEvents;
        this.maxEvents = maxEvents;
        this.latestEventId = latestEventId;
    }

    public static ShowWebApiScreenPacketData fromServer() {
        StationStopTracker tracker = StationStopTracker.getInstance();
        return new ShowWebApiScreenPacketData(
            StationStopWebApi.isEnabledInConfig(),
            StationStopWebApi.isRunning(),
            StationStopWebApi.getBaseUrl(),
            StationStopWebApi.getBindAddress(),
            StationStopWebApi.getPort(),
            tracker.getActiveStopCount(),
            TrainScheduleCache.getTrackedTrainCount(),
            tracker.getBufferedEventCount(),
            ModCommonConfig.WEB_API_MAX_EVENTS.get(),
            tracker.getLatestEventId()
        );
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.putBoolean(NBT_ENABLED, enabled);
        nbt.putBoolean(NBT_RUNNING, running);
        nbt.putString(NBT_BASE_URL, baseUrl);
        nbt.putString(NBT_BIND, bindAddress);
        nbt.putInt(NBT_PORT, port);
        nbt.putInt(NBT_ACTIVE_STOPS, activeStops);
        nbt.putInt(NBT_TRACKED_TRAINS, trackedTrains);
        nbt.putInt(NBT_BUFFERED_EVENTS, bufferedEvents);
        nbt.putInt(NBT_MAX_EVENTS, maxEvents);
        nbt.putLong(NBT_LATEST_EVENT_ID, latestEventId);
    }

    @Override
    protected void read(CompoundTag nbt) {
        enabled = nbt.getBoolean(NBT_ENABLED);
        running = nbt.getBoolean(NBT_RUNNING);
        baseUrl = nbt.getString(NBT_BASE_URL);
        bindAddress = nbt.getString(NBT_BIND);
        port = nbt.getInt(NBT_PORT);
        activeStops = nbt.getInt(NBT_ACTIVE_STOPS);
        trackedTrains = nbt.getInt(NBT_TRACKED_TRAINS);
        bufferedEvents = nbt.getInt(NBT_BUFFERED_EVENTS);
        maxEvents = nbt.getInt(NBT_MAX_EVENTS);
        latestEventId = nbt.getLong(NBT_LATEST_EVENT_ID);
    }

    public static void handle(ShowWebApiScreenPacketData packet, NetworkPacketContext context) {
        ClientWrapper.showWebApiScreen(packet);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean running() {
        return running;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String bindAddress() {
        return bindAddress;
    }

    public int port() {
        return port;
    }

    public int activeStops() {
        return activeStops;
    }

    public int trackedTrains() {
        return trackedTrains;
    }

    public int bufferedEvents() {
        return bufferedEvents;
    }

    public int maxEvents() {
        return maxEvents;
    }

    public long latestEventId() {
        return latestEventId;
    }
}
