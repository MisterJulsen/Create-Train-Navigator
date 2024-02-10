package de.mrjulsen.crn.client.gui.overlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.screen.OverlayPosition;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleTrainConnection;
import de.mrjulsen.crn.item.ModItems;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.data.SimpleRoute.StationEntry;
import de.mrjulsen.crn.data.SimpleRoute.StationTag;
import de.mrjulsen.crn.data.SimpleRoute.TaggedStationEntry;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.NextConnectionsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.RealtimeRequestPacket;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils.TimeFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.gui.ForgeIngameGui;

public class RouteDetailsOverlayScreen implements HudOverlay {

    public static final int ID = 1;

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/overview.png");
    private static final int GUI_WIDTH = 226;
    private static final int GUI_HEIGHT = 118;
    private static final int SLIDING_TEXT_AREA_WIDTH = 220;
    //private static final float getUIScale() = 0.5f;

    private static final int ON_TIME = 0x1AEA5F;
    private static final int DELAYED = 0xFF4242;
    private static final int COLOR_WARN = 16755200;

    private static final int INFO_BEFORE_NEXT_STOP = 500;
    //private static final int DELAY_TRESHOLD = 500;

    private long fadeStart = 0L;
    private boolean fading = false;
    private boolean fadeInvert = true;
    private Runnable fadeDoneAction;
    
    private static final int ROUTE_LINE_HEIGHT = 14;

    private final Level level;
    
    private static final int MAX_STATION_PER_PAGE = 4;
    private final TaggedStationEntry[] taggedRoute;
    private final int transferCount;
    private final int totalDuration;
    private int stationIndex = 0;

    private Page currentPage = Page.ROUTE_OVERVIEW;
    private State currentState = State.BEFORE_JOURNEY;

    private Collection<SimpleTrainConnection> connections;
    private static final int CONNECTION_ENTRIES_PER_PAGE = 3;
    private static final int TIME_PER_CONNECTIONS_SUBPAGE = 200;
    private int connectionsSubPageTime = 0;
    private int connectionsSubPageIndex = 0;
    private int connectionsSubPagesCount = 0;

    private static final int TIME_PER_TRAIN_DATA_SUBPAGE = 200;   
    private static final int TRAIN_DATA_PAGES = 2;  
    private int trainDataSubPageTime = 0;
    private int trainDataSubPageIndex = 0;

    private static final Component title = new TextComponent("Route Details");
    private static final Component textConcat = new TextComponent("     ***     ");
    private final Font shadowlessFont;

    private Component slidingText;
    private float slidingTextOffset = 0;
    private int slidingTextWidth = 0;

    private static final int REALTIME_REFRESH_TIME = 100;
    private int realTimeRefreshTimer = 0;
    
    private MultiLineLabel messageLabel;

    private LerpedFloat xPos = LerpedFloat.linear().startWithValue(0);
    private LerpedFloat yPos = LerpedFloat.linear().startWithValue(0);
    
    private static final String keyJourneyBegins = "gui.createrailwaysnavigator.route_overview.journey_begins";
    private static final String keyTrainDetails = "gui.createrailwaysnavigator.route_overview.train_details";
    private static final String keyTrainSpeed = "gui.createrailwaysnavigator.route_overview.train_speed";
    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyTransferCount = "gui.createrailwaysnavigator.navigator.route_entry.transfer";
    private static final String keyAfterJourney = "gui.createrailwaysnavigator.route_overview.after_journey";
    private static final String keyJourneyCompleted = "gui.createrailwaysnavigator.route_overview.journey_completed";
    private static final String keyNextConnections = "gui.createrailwaysnavigator.route_overview.next_connections";
    private static final String keyScheduleTransfer = "gui.createrailwaysnavigator.route_overview.schedule_transfer";
    private static final String keyConnectionEndangered = "gui.createrailwaysnavigator.route_overview.connection_endangered";
    private static final String keyConnectionMissed = "gui.createrailwaysnavigator.route_overview.connection_missed";
    private static final String keyJourneyInterrupted = "gui.createrailwaysnavigator.route_overview.journey_interrupted";
    private static final String keyConnectionMissedInfo = "gui.createrailwaysnavigator.route_overview.connection_missed_info";
    private static final String keyConnectionMissedPageText = "gui.createrailwaysnavigator.route_overview.journey_interrupted_info";
    private static final String keyDepartureIn = "gui.createrailwaysnavigator.route_details.departure";
    private static final String keyTimeNow = "gui.createrailwaysnavigator.time.now";
    private static final String keyOptionsText = "gui.createrailwaysnavigator.route_overview.options";
    private static final String keyKeybindOptions = "key.createrailwaysnavigator.route_overlay_options";

    @SuppressWarnings("resource")
    public RouteDetailsOverlayScreen(Level level, int lastRefreshedTime, SimpleRoute route) {
        this.level = level;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);
        this.taggedRoute = route.getRoutePartsTagged();
        this.transferCount = route.getTransferCount();
        this.totalDuration = route.getTotalDuration();

        setPageJourneyStart();
        
        Component text = new TranslatableComponent(keyJourneyBegins,
            currentStation().train().trainName(),
            currentStation().train().scheduleTitle(),
            TimeUtils.parseTime((int)currentStation().station().getEstimatedTimeWithThreshold() + Constants.TIME_SHIFT, TimeFormat.HOURS_24),
            currentStation().station().getInfo().platform()
        );
        setSlidingText(text);

        if (ModClientConfig.ROUTE_NARRATOR.get()) {
            NarratorChatListener.INSTANCE.narrator.say(text.getString() + ". " + Utils.translate(keyOptionsText, new KeybindComponent(keyKeybindOptions)).getString(), true);
        }
    }

    private float getUIScale() {
        return (float)ModClientConfig.OVERLAY_SCALE.get().doubleValue();
    }


    @Override
    public void tick() {

        if (ModKeys.keyRouteOverlayOptions.isDown() && Minecraft.getInstance().player.getInventory().hasAnyOf(Set.of(ModItems.NAVIGATOR.get()))) {
            ClientWrapper.showRouteOverlaySettingsGui();
        }

        xPos.tickChaser();
        yPos.tickChaser();

        // Sliding text
        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * getUIScale()) {
            slidingTextOffset--;
            if (slidingTextOffset < -(slidingTextWidth / 2)) {
                slidingTextOffset = (int)((SLIDING_TEXT_AREA_WIDTH + slidingTextWidth / 2) + 20);                
            }
        }
        
        // train info while traveling
        if (currentState == State.WHILE_TRAVELING) {
            trainDataSubPageTime++;
            if ((slidingTextWidth <= SLIDING_TEXT_AREA_WIDTH && trainDataSubPageTime > TIME_PER_TRAIN_DATA_SUBPAGE) || slidingTextOffset < -(slidingTextWidth / 2)) {
                setTrainDataSubPage(false);
            }
        }

        switch (currentPage) {
            case NEXT_CONNECTIONS: // Next connections animation
                if (fading) {
                    break;
                }

                connectionsSubPageTime++;
                if (connectionsSubPageTime > TIME_PER_CONNECTIONS_SUBPAGE) {
                    setNextConnectionsSubPage();
                }
                break;
            case ROUTE_OVERVIEW:
            default:
                break; 
        }

        // refresh data loop (core)
        if (currentState != State.AFTER_JOURNEY && currentState != State.JOURNEY_INTERRUPTED) {
            realTimeRefreshTimer++;
            if (realTimeRefreshTimer > REALTIME_REFRESH_TIME) {
                realTimeRefreshTimer = 0;
                requestRealtimeData();
            }
        }
    }

    /**
     * Core
     */
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

    //#region FUNCTIONS    
    @Override
    public int getId() {
        return ID;
    }
    
    private TaggedStationEntry currentStation() {
        return taggedRoute[stationIndex];
    }

    private void fadeIn(Runnable andThen) {
        fadeInternal(andThen, false);
    }

    private void fadeOut(Runnable andThen) {
        fadeInternal(andThen, true);
    }

    private void fadeInternal(Runnable andThen, boolean fadeOut) {
        fadeDoneAction = andThen;
        fadeInvert = fadeOut;
        this.fadeStart = Util.getMillis();
        fading = true;
    }

    private void setSlidingText(Component component) {
        slidingText = component;
        slidingTextWidth = shadowlessFont.width(component);

        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * getUIScale()) {
            slidingTextOffset = (int)((SLIDING_TEXT_AREA_WIDTH + slidingTextWidth / 2) + 20);
        } else {
            slidingTextOffset = (int)(SLIDING_TEXT_AREA_WIDTH * 0.75f / 2);
        }
    }

    private Component concat(Component... components) {
        if (components.length <= 0) {
            return new TextComponent("");
        }

        MutableComponent c = components[0].copy();
        for (int i = 1; i < components.length; i++) {
            c.append(textConcat);
            c.append(components[i]);
        }
        return c;
    }
    
    private void startStencil(PoseStack poseStack, int x, int y, int w, int h) {
        UIRenderHelper.swapAndBlitColor(Minecraft.getInstance().getMainRenderTarget(), UIRenderHelper.framebuffer);
        ModGuiUtils.startStencil(poseStack, x, y, w, h);
    }

    private void endStencil() {
        ModGuiUtils.endStencil();
        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, Minecraft.getInstance().getMainRenderTarget());
    }
    //#endregion

    //#region ACTIONS
    private void nextStop() {
        if (!changeCurrentStation()) {
            return;
        }

        setTrainDataSubPage(true);
        setPageRouteOverview();
        currentState = State.WHILE_TRAVELING;
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
        setSlidingText(new TranslatableComponent(keyAfterJourney,
            taggedRoute[taggedRoute.length - 1].station().getStationName()
        ));
        currentState = State.AFTER_JOURNEY;
        setPageJourneyCompleted();
    }

    private void announceNextStop() {
        Component textA = Utils.emptyText();
        Component textB = Utils.emptyText();
        Component display;
        display = textA = new TranslatableComponent(keyNextStop,
            currentStation().station().getStationName()
        );
        if (currentStation().tag() == StationTag.PART_END && currentStation().index() + 1 < taggedRoute.length) {
            Component transferText = textB = new TranslatableComponent(keyTransfer,
                taggedRoute[stationIndex + 1].train().trainName(),
                taggedRoute[stationIndex + 1].train().scheduleTitle(),
                taggedRoute[stationIndex + 1].station().getInfo().platform()
            );
            display = concat(display, transferText);
            currentState = State.BEFORE_TRANSFER;
        } else {
            currentState = State.BEFORE_NEXT_STOP;
        }
        setSlidingText(display);

        if (ModClientConfig.ROUTE_NARRATOR.get()) {
            NarratorChatListener.INSTANCE.narrator.say(textA.getString() + ". " + textB.getString(), true);
        }

        setPageNextConnections();
    }

    private void reachNextStop() {
        setSlidingText(new TextComponent(currentStation().station().getStationName()));
        if (ModClientConfig.ROUTE_NARRATOR.get()) {
            NarratorChatListener.INSTANCE.narrator.say(currentStation().station().getStationName(), true);
        }
        currentState = State.WHILE_NEXT_STOP;
    }

    private void reachTransferStop() {
        Component text = new TranslatableComponent(keyTransfer,
            taggedRoute[stationIndex + 1].train().trainName(),
            taggedRoute[stationIndex + 1].train().scheduleTitle(),
            taggedRoute[stationIndex + 1].station().getInfo().platform()
        );
        setSlidingText(text);
        if (ModClientConfig.ROUTE_NARRATOR.get()) {
            NarratorChatListener.INSTANCE.narrator.say(text.getString(), true);
        }
        currentState = State.WHILE_TRANSFER;
        changeCurrentStation();
        setPageTransfer();
    }

    private void reachTransferStopConnectionMissed() {
        setSlidingText(concat(
            Utils.text(currentStation().station().getStationName()),
            new TranslatableComponent(keyConnectionMissedInfo),
            new TranslatableComponent(keyJourneyInterrupted,
                taggedRoute[taggedRoute.length - 1].station().getStationName()
            )
        ));
        setPageJourneyInterrupted();
        currentState = State.JOURNEY_INTERRUPTED;
    }
    //#endregion

    //#region PAGE MANAGEMENT
    private void setPageRouteOverview() {
        fadeOut(() -> {
            currentPage = Page.ROUTE_OVERVIEW;
            fadeIn(null);
        });
    }

    private void setPageJourneyStart() {
        fadeOut(() -> {
            currentPage = Page.JOURNEY_START;
            fadeIn(null);
        });
    }

    private void setPageTransfer() {
        fadeOut(() -> {
            currentPage = Page.TRANSFER;            
            this.messageLabel = MultiLineLabel.create(shadowlessFont, new TranslatableComponent(keyTransfer,
                taggedRoute[stationIndex + 1].train().trainName(),
                taggedRoute[stationIndex + 1].train().scheduleTitle(),
                taggedRoute[stationIndex + 1].station().getInfo().platform()
            ), SLIDING_TEXT_AREA_WIDTH - (15 + ModGuiIcons.ICON_SIZE));
            fadeIn(null);
        });
    }

    private void setPageJourneyInterrupted() {
        fadeOut(() -> {
            currentPage = Page.CONNECTION_MISSED;
            this.messageLabel = MultiLineLabel.create(shadowlessFont, new TranslatableComponent(keyConnectionMissedPageText, taggedRoute[taggedRoute.length - 1].station().getStationName()), SLIDING_TEXT_AREA_WIDTH - 10);
            fadeIn(null);
        });
    }

    private void setPageJourneyCompleted() {
        fadeOut(() -> {
            currentPage = Page.JOURNEY_END;
            this.messageLabel = MultiLineLabel.create(shadowlessFont, new TranslatableComponent(keyAfterJourney, taggedRoute[taggedRoute.length - 1].station().getStationName()), SLIDING_TEXT_AREA_WIDTH - 10);
            fadeIn(null);
        });
    }

    private void setPageNextConnections() {
        long id = InstanceManager.registerClientNextConnectionsResponseAction((connections, time) -> {
            this.connections = connections;
            if (!connections.isEmpty()) {                
                fadeOut(() -> {
                    currentPage = Page.NEXT_CONNECTIONS;
                    connectionsSubPagesCount = connections.size() / CONNECTION_ENTRIES_PER_PAGE + (connections.size() % CONNECTION_ENTRIES_PER_PAGE == 0 ? 0 : 1);
                    connectionsSubPageIndex = 0;
                    connectionsSubPageTime = 0;
                    fadeIn(null);
                });
            }
        });
        NetworkManager.sendToServer(new NextConnectionsRequestPacket(id, currentStation().train().trainId(), currentStation().station().getStationName(), currentStation().station().getTicks()));
    }

    private void setNextConnectionsSubPage() {
        if (connectionsSubPagesCount > 1) {
            fadeOut(() -> {
                connectionsSubPageTime = 0;
                connectionsSubPageIndex++;
                if (connectionsSubPageIndex >= connectionsSubPagesCount) {
                    connectionsSubPageIndex = 0;
                }
                fadeIn(null);
            });
        } else {
            connectionsSubPageTime = 0;
        }
    }

    private void setTrainDataSubPage(boolean reset) {
        if (reset || trainDataSubPageIndex >= TRAIN_DATA_PAGES) {
            trainDataSubPageIndex = 0;
        } else {
            trainDataSubPageIndex++;
        }

        switch (trainDataSubPageIndex) {
            default:
            case 0:
                setSlidingText(new TranslatableComponent(keyTrainDetails,
                    currentStation().train().trainName(),
                    currentStation().train().scheduleTitle()
                ));
                trainDataSubPageTime = 0;
                break;
            case 1:
                long id = InstanceManager.registerClientTrainDataResponseAction((data, time) -> {
                    setSlidingText(new TranslatableComponent(keyTrainSpeed,
                        (int)Math.abs(Math.round(data.speed() * 20 * 3.6F))
                    ));
                    trainDataSubPageTime = 0;
                });
                NetworkManager.sendToServer(new TrainDataRequestPacket(id, currentStation().train().trainId()));
                break;
        }
    }
    //#endregion

    //#region RENDERING
    @Override
    public void render(ForgeIngameGui gui, PoseStack poseStack, int width, int height, float partialTicks) {
        OverlayPosition pos = ModClientConfig.ROUTE_OVERLAY_POSITION.get();
        final int x = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.BOTTOM_LEFT ? 10 : (int)(width - GUI_WIDTH * getUIScale() - 10);
        final int y = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.TOP_RIGHT ? 10 : (int)(height - GUI_HEIGHT * getUIScale() - 10);

        xPos.chase(x, 0.2f, Chaser.EXP);
        yPos.chase(y, 0.2f, Chaser.EXP);

        poseStack.pushPose();
        poseStack.translate((int)xPos.getValue(), (int)yPos.getValue(), 0);
        renderInternal(gui, poseStack, 0, 0, width, height, partialTicks);
        poseStack.popPose();
    }

    private void renderInternal(ForgeIngameGui gui, PoseStack poseStack, int x, int y, int width, int height, float partialTicks) {
        poseStack.pushPose();
        float fadePercentage = this.fading ? Mth.clamp((float)(Util.getMillis() - this.fadeStart) / 500.0F, 0.0F, 1.0F) : 1.0F;
        float alpha = fadeInvert ? Mth.clamp(1.0f - fadePercentage, 0, 1) : Mth.clamp(fadePercentage, 0, 1);
        int fontAlpha = Mth.ceil(alpha * 255.0F) << 24; // <color> | fontAlpha

        poseStack.scale(getUIScale(), getUIScale(), getUIScale());
        RenderSystem.setShaderTexture(0, GUI);
        GuiComponent.blit(poseStack, x, y, 0, currentState.important() ? 138 : 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
        
        GuiComponent.drawString(poseStack, shadowlessFont, title, x + 6, y + 4, 0x4F4F4F);
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyOptionsText, new KeybindComponent(keyKeybindOptions).withStyle(ChatFormatting.BOLD)), x + 6, y + GUI_HEIGHT - 2 - shadowlessFont.lineHeight, 0x4F4F4F);
        
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + Constants.TIME_SHIFT) % Constants.TICKS_PER_DAY), TimeFormat.HOURS_24);
        GuiComponent.drawString(poseStack, shadowlessFont, timeString, x + GUI_WIDTH - 4 - shadowlessFont.width(timeString), y + 4, 0x4F4F4F);
        
        // Test
        renderSlidingText(poseStack, x, y + 2);

        startStencil(poseStack, x + 3, y + 40, 220, 62);
        poseStack.pushPose();
        poseStack.translate((fadeInvert ? 0 : -20) + fadePercentage * 20, 0, 0);
        if (alpha > 0.1f && (fontAlpha & -67108864) != 0) {
            
            switch (currentPage) {
                case JOURNEY_START:
                    renderPageJourneyStart(poseStack, x, y + 40, fadePercentage, fontAlpha);
                    break;
                case NEXT_CONNECTIONS:
                    renderNextConnections(poseStack, x, y + 40, fadePercentage, fontAlpha, null);
                    break;
                case TRANSFER:
                    renderPageTransfer(poseStack, x, y + 40, fadePercentage, fontAlpha, null);
                    break;
                case CONNECTION_MISSED:
                    renderPageJourneyInterrupted(poseStack, x, y + 40, fadePercentage, fontAlpha);
                    break;
                case JOURNEY_END:
                    renderPageJourneyCompleted(poseStack, x, y + 40, fadePercentage, fontAlpha);
                    break;
                case ROUTE_OVERVIEW:
                default:
                    final int[] yOffset = new int[] { y + 40 - 1 };
                    final boolean[] b = new boolean[] { true };
                    for (int i = stationIndex; i < Math.min(stationIndex + MAX_STATION_PER_PAGE, taggedRoute.length); i++) {
                        final int k = i;
                        yOffset[0] += renderRouteOverview(poseStack, k, x, yOffset[0], alpha, fontAlpha, b[0], (bool) -> {
                            if (b[0]) {
                                b[0] = bool;
                            }
                        });
                    }                    
                    break;
            }
        }
        poseStack.popPose();
        endStencil();

        poseStack.popPose();

        if (fadePercentage >= 1.0f) {
            fading = false;
            if (fadeDoneAction != null) {
                fadeDoneAction.run();
            }
        }

        
        GuiComponent.drawString(poseStack, shadowlessFont, currentState.name(), x, y + 4 + GUI_HEIGHT, 0xFFFFFF);
    }

    public void renderSlidingText(PoseStack poseStack, int x, int y) {
        startStencil(poseStack, x + 3, y + 16, 220, 21);
        poseStack.pushPose();
        poseStack.scale(1.0f / 0.75f, 1.0f / 0.75f, 1.0f / 0.75f);
        GuiComponent.drawCenteredString(poseStack, shadowlessFont, slidingText, (int)((x + 3) + slidingTextOffset), y + 14, 0xFF9900);
        poseStack.popPose();
        endStencil();
    }

    public int renderRouteOverview(PoseStack poseStack, int index, int x, int y, float alphaPercentage, int fontAlpha, boolean valid, Consumer<Boolean> reachConnection) {
        RenderSystem.setShaderTexture(0, GUI);
        RenderSystem.setShaderColor(1, 1, 1, alphaPercentage);        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        TaggedStationEntry station = taggedRoute[index];
        TaggedStationEntry lastStation = index > stationIndex ? taggedRoute[index - 1] : null;
        TaggedStationEntry nextStation = index < taggedRoute.length - 1 ? taggedRoute[index + 1] : null;

        boolean reachable = valid && !station.isDeparted();

        // Icon
        int dY = index <= 0 ? 0 : ROUTE_LINE_HEIGHT;
        int transferY = ROUTE_LINE_HEIGHT * 3;
        switch (station.tag()) {
            case PART_START:
                dY = ROUTE_LINE_HEIGHT * 2;
                break;
            case START:
                dY = ROUTE_LINE_HEIGHT * 4;
                break;
            default:
                break;
        }        
        GuiComponent.blit(poseStack, x + 75, y, 226, dY, 7, ROUTE_LINE_HEIGHT, 256, 256);
        if (index >= MAX_STATION_PER_PAGE - 1 && station.tag() != StationTag.END) {            
            GuiComponent.blit(poseStack, x + 75, y + ROUTE_LINE_HEIGHT, 226, ROUTE_LINE_HEIGHT, 7, ROUTE_LINE_HEIGHT, 256, 256);
        }

        // time display
        long timeDiff = station.station().getCurrentRefreshTime() + station.station().getCurrentTicks() - station.station().getRefreshTime() - station.station().getTicks();
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.text(TimeUtils.parseTime((int)(station.station().getScheduleTime() + Constants.TIME_SHIFT), TimeFormat.HOURS_24)).withStyle(reachable ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, reachable ? (index <= stationIndex ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha);
        
        if (station.station().getEstimatedTimeWithThreshold() > 0 && reachable && (lastStation == null || lastStation.station().getEstimatedTime() < station.station().getEstimatedTime()) && (station.train().trainId().equals(currentStation().train().trainId()) || station.station().getEstimatedTime() + ModClientConfig.TRANSFER_TIME.get() + ModClientConfig.EARLY_ARRIVAL_THRESHOLD.get() > station.station().getScheduleTime())) {
            GuiComponent.drawString(poseStack, shadowlessFont, Utils.text(TimeUtils.parseTime((int)(station.station().getEstimatedTimeWithThreshold() + Constants.TIME_SHIFT), TimeFormat.HOURS_24)).withStyle(reachable ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), x + 40, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, timeDiff < ModClientConfig.DEVIATION_THRESHOLD.get() && reachable ? ON_TIME | fontAlpha : DELAYED | fontAlpha);
        }

        // station name display
        Component platformText = Utils.text(station.station().getInfo().platform());
        int platformTextWidth = shadowlessFont.width(platformText);

        final int maxStationNameWidth = SLIDING_TEXT_AREA_WIDTH - platformTextWidth - 105;
        MutableComponent stationText = Utils.text(station.station().getStationName());
        if (index == stationIndex) stationText = stationText.withStyle(ChatFormatting.BOLD);
        if (!reachable) stationText = stationText.withStyle(ChatFormatting.STRIKETHROUGH);
        if (shadowlessFont.width(stationText) > maxStationNameWidth) {
            stationText = Utils.text(shadowlessFont.substrByWidth(stationText, maxStationNameWidth).getString()).append(Utils.text("...")).withStyle(stationText.getStyle());
        }

        GuiComponent.drawString(poseStack, shadowlessFont, stationText, x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, reachable ? (index <= 0 ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha);
        GuiComponent.drawString(poseStack, shadowlessFont, platformText, x + SLIDING_TEXT_AREA_WIDTH - platformTextWidth, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, reachable ? (index <= 0 ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha);
        
        if (station.isDeparted()) {
            //ModGuiIcons.CROSS.render(poseStack, x + 10, y + ROUTE_LINE_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);                
        }

        // render transfer
        if (station.tag() == StationTag.PART_END) {
            y += ROUTE_LINE_HEIGHT;
            long transferTime = -1;
            if (nextStation != null && !nextStation.isDeparted()) {
                if (nextStation.station().getCurrentTime() + ModClientConfig.TRANSFER_TIME.get() < nextStation.station().getScheduleTime()) {
                    transferTime = nextStation.station().getScheduleTime() - taggedRoute[index].station().getScheduleTime();
                } else {
                    transferTime = nextStation.station().getCurrentTime() - taggedRoute[index].station().getCurrentTime();
                }
            }
            RenderSystem.setShaderTexture(0, GUI);
            GuiComponent.blit(poseStack, x + 75, y, 226, transferY, 7, ROUTE_LINE_HEIGHT, 256, 256);
            if (transferTime < 0) {
                reachConnection.accept(false);
                if (nextStation.isDeparted()) {
                    ModGuiIcons.CROSS.render(poseStack, x + 10, y + ROUTE_LINE_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
                    GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyConnectionMissed).withStyle(ChatFormatting.BOLD), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, DELAYED | fontAlpha);
                } else {
                    ModGuiIcons.WARN.render(poseStack, x + 10, y + ROUTE_LINE_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
                    GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyConnectionEndangered).withStyle(ChatFormatting.BOLD), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, COLOR_WARN | fontAlpha);
                }
            } else {
                reachConnection.accept(!station.isDeparted());
                GuiComponent.drawString(poseStack, shadowlessFont, Utils.text(TimeUtils.parseDurationShort((int)(transferTime))).withStyle(ChatFormatting.ITALIC), x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, 0xDBDBDB | fontAlpha);
                GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyScheduleTransfer).withStyle(ChatFormatting.ITALIC), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, 0xDBDBDB | fontAlpha);
            }
                    
            return ROUTE_LINE_HEIGHT * 2;
        } else {            
            reachConnection.accept(!station.isDeparted());
        }
        return ROUTE_LINE_HEIGHT;
        
    }

    public void renderNextConnections(PoseStack poseStack, int x, int y, float alphaPercentage, int fontAlpha, StationEntry station) {
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyNextConnections).withStyle(ChatFormatting.BOLD), x + 10, y + 4, 0xFFFFFF | fontAlpha);

        SimpleTrainConnection[] conns = connections.toArray(SimpleTrainConnection[]::new);
        for (int i = connectionsSubPageIndex * CONNECTION_ENTRIES_PER_PAGE, k = 0; i < Math.min((connectionsSubPageIndex + 1) * CONNECTION_ENTRIES_PER_PAGE, connections.size()); i++, k++) {
            GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(TimeUtils.parseTime((int)((conns[i].ticks() + Constants.TIME_SHIFT) % Constants.TICKS_PER_DAY), TimeFormat.HOURS_24)), x + 10, y + 15 + 12 * k, 0xDBDBDB | fontAlpha);
            GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(conns[i].trainName()), x + 40, y + 15 + 12 * k, 0xDBDBDB | fontAlpha);
            GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(conns[i].scheduleTitle()), x + 90, y + 15 + 12 * k, 0xDBDBDB | fontAlpha);
        }

        // page
        final int dotSize = 4;
        final int dotY = y + 62 - 10;
        final int startX = x + GUI_WIDTH / 2 - connectionsSubPagesCount * dotSize - dotSize;

        for (int i = 0; i < connectionsSubPagesCount; i++) {
            int s = dotSize + (i == connectionsSubPageIndex ? 2 : 0);
            int dX = startX + i * dotSize * 3 - (i == connectionsSubPageIndex ? 1 : 0);
            int dY = dotY - (i == connectionsSubPageIndex ? 1 : 0);
            GuiComponent.fill(poseStack, dX, dY, dX + s, dY + s, i == connectionsSubPageIndex ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha);
            GuiComponent.fill(poseStack, dX + 1, dY + 1, dX + s - 1, dY + s - 1, i == connectionsSubPageIndex ? 0xAAAAAA | fontAlpha : 0x888888 | fontAlpha);
        }

    }

    public void renderPageJourneyStart(PoseStack poseStack, int x, int y, float alphaPercentage, int fontAlpha) {
        y += 3 + renderRouteOverview(poseStack, stationIndex, x, y - 3, alphaPercentage, fontAlpha, true, (bool) -> {});
        GuiComponent.fill(poseStack, x + 3, y, x + 3 + SLIDING_TEXT_AREA_WIDTH, y + 1, 0xDBDBDB | fontAlpha);
        
        // Title
        ModGuiIcons.TIME.render(poseStack, x + 10, y + 3);
        long time = currentStation().station().getEstimatedTimeWithThreshold() - level.getDayTime();
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyDepartureIn).append(" ").append(time > 0 ? Utils.text(TimeUtils.parseTime((int)(time % 24000), TimeFormat.HOURS_24)) : Utils.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, 0xFFFFFF | fontAlpha);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        final int detailsLineHeight = 12;
        //StationEntry station = taggedRoute[0].station();
        StationEntry endStation = taggedRoute[taggedRoute.length - 1].station();

        Component platformText = Utils.text(endStation.getInfo().platform());
        int platformTextWidth = shadowlessFont.width(platformText);
        final int maxStationNameWidth = SLIDING_TEXT_AREA_WIDTH - platformTextWidth - 10 - 5;
        MutableComponent stationText = Utils.text(TimeUtils.parseTime((int)(endStation.getEstimatedTimeWithThreshold() + Constants.TIME_SHIFT % 24000), TimeFormat.HOURS_24)).append(Utils.text(" " + endStation.getStationName()));
        if (shadowlessFont.width(stationText) > maxStationNameWidth) {
            stationText = Utils.text(shadowlessFont.substrByWidth(stationText, maxStationNameWidth).getString()).append(Utils.text("...")).withStyle(stationText.getStyle());
        }

        ModGuiIcons.TARGET.render(poseStack, x + 10, y + shadowlessFont.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiComponent.drawString(poseStack, shadowlessFont, stationText, x + 15 + ModGuiIcons.ICON_SIZE, y, 0xDBDBDB | fontAlpha);
        GuiComponent.drawString(poseStack, shadowlessFont, platformText, x + SLIDING_TEXT_AREA_WIDTH - platformTextWidth, y, 0xDBDBDB | fontAlpha);
        ModGuiIcons.INFO.render(poseStack, x + 10, y + detailsLineHeight + shadowlessFont.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.text(String.format("%s %s | %s",
            transferCount,
            Utils.translate(keyTransferCount).getString(),
            TimeUtils.parseDurationShort(totalDuration)
        )), x + 15 + ModGuiIcons.ICON_SIZE, y + detailsLineHeight, 0xDBDBDB | fontAlpha);
    }

    public void renderPageTransfer(PoseStack poseStack, int x, int y, float alphaPercentage, int fontAlpha, StationEntry station) {
        y += 3 + renderRouteOverview(poseStack, stationIndex, x, y - 3, alphaPercentage, fontAlpha, true, (bool) -> {});
        GuiComponent.fill(poseStack, x + 3, y, x + 3 + SLIDING_TEXT_AREA_WIDTH, y + 1, 0xDBDBDB | fontAlpha);
        
        // Title
        ModGuiIcons.WALK.render(poseStack, x + 10, y + 3);
        
        TaggedStationEntry nextStation = stationIndex < taggedRoute.length - 1 ? taggedRoute[stationIndex + 1] : null;
        long transferTime = -1;
        if (nextStation != null && !nextStation.isDeparted()) {
            if (nextStation.station().getCurrentTime() + ModClientConfig.TRANSFER_TIME.get() < nextStation.station().getScheduleTime()) {
                transferTime = nextStation.station().getScheduleTime() - taggedRoute[stationIndex].station().getScheduleTime();
            } else {
                transferTime = nextStation.station().getCurrentTime() - taggedRoute[stationIndex].station().getCurrentTime();
            }
        }
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyScheduleTransfer).append(" ").append(Utils.text(TimeUtils.parseDurationShort((int)transferTime))).withStyle(ChatFormatting.BOLD), x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, 0xFFFFFF | fontAlpha);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(poseStack, x + 15 + ModGuiIcons.ICON_SIZE, y, 12, 0xDBDBDB | fontAlpha);
    }

    public void renderPageJourneyInterrupted(PoseStack poseStack, int x, int y, float alphaPercentage, int fontAlpha) {
        // Title
        ModGuiIcons.CROSS.render(poseStack, x + 10, y + 3);
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyConnectionMissed).withStyle(ChatFormatting.BOLD), x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, DELAYED | fontAlpha);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(poseStack, x + 10, y, 10, 0xDBDBDB | fontAlpha);
    }

    public void renderPageJourneyCompleted(PoseStack poseStack, int x, int y, float alphaPercentage, int fontAlpha) {
        // Title
        ModGuiIcons.CHECK.render(poseStack, x + 10, y + 3);
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyJourneyCompleted).withStyle(ChatFormatting.BOLD), x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, ON_TIME | fontAlpha);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(poseStack, x + 10, y, 10, 0xDBDBDB | fontAlpha);
    }
    //#endregion

    protected static enum Page {
        ROUTE_OVERVIEW,
        NEXT_CONNECTIONS,
        JOURNEY_START,
        JOURNEY_END,
        TRANSFER,
        CONNECTION_MISSED;
    }

    protected static enum State {
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
