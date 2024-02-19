package de.mrjulsen.crn.client.gui.overlay;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
import de.mrjulsen.crn.client.gui.NavigatorToast;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.OverlayPosition;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleTrainConnection;
import de.mrjulsen.crn.item.ModItems;
import de.mrjulsen.crn.data.SimpleRoute.StationEntry;
import de.mrjulsen.crn.data.SimpleRoute.StationTag;
import de.mrjulsen.crn.event.listeners.IJourneyListenerClient;
import de.mrjulsen.crn.event.listeners.JourneyListener;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.event.listeners.JourneyListener.AnnounceNextStopData;
import de.mrjulsen.crn.event.listeners.JourneyListener.ContinueData;
import de.mrjulsen.crn.event.listeners.JourneyListener.FinishJourneyData;
import de.mrjulsen.crn.event.listeners.JourneyListener.JourneyBeginData;
import de.mrjulsen.crn.event.listeners.JourneyListener.JourneyInterruptData;
import de.mrjulsen.crn.event.listeners.JourneyListener.NotificationData;
import de.mrjulsen.crn.event.listeners.JourneyListener.ReachNextStopData;
import de.mrjulsen.crn.event.listeners.JourneyListener.State;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.NextConnectionsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.DragonLibConstants;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.gui.ForgeIngameGui;

public class RouteDetailsOverlayScreen implements IHudOverlay, IJourneyListenerClient {

    public static final int ID = 1;

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/overview.png");
    private static final Component title = Utils.translate("gui.createrailwaysnavigator.route_overview.title");
    private static final int GUI_WIDTH = 226;
    private static final int GUI_HEIGHT = 118;
    private static final int SLIDING_TEXT_AREA_WIDTH = 220;

    private static final int ON_TIME = 0x1AEA5F;
    private static final int DELAYED = 0xFF4242;
    private static final int COLOR_WARN = 16755200;

    private long fadeStart = 0L;
    private boolean fading = false;
    private boolean fadeInvert = true;
    private Runnable fadeDoneAction;
    
    private static final int ROUTE_LINE_HEIGHT = 14;

    private final Level level;
    
    private static final int MAX_STATION_PER_PAGE = 4;

    private Page currentPage = Page.NONE;

    private Collection<SimpleTrainConnection> connections;
    private long connectionsRefreshTime;
    private static final int CONNECTION_ENTRIES_PER_PAGE = 3;
    private static final int TIME_PER_CONNECTIONS_SUBPAGE = 200;
    private int connectionsSubPageTime = 0;
    private int connectionsSubPageIndex = 0;
    private int connectionsSubPagesCount = 0;

    private static final int TIME_PER_TRAIN_DATA_SUBPAGE = 200;   
    private static final int TRAIN_DATA_PAGES = 2;  
    private int trainDataSubPageTime = 0;
    private int trainDataSubPageIndex = 0;

    private final Font shadowlessFont;

    private Component slidingText = Utils.emptyText();
    private float slidingTextOffset = 0;
    private int slidingTextWidth = 0;
    
    private MultiLineLabel messageLabel;
    private MutableComponent interruptedText;

    private LerpedFloat xPos;
    private LerpedFloat yPos;
    
    private static final String keyTrainDetails = "gui.createrailwaysnavigator.route_overview.train_details";
    private static final String keyTrainSpeed = "gui.createrailwaysnavigator.route_overview.train_speed";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyTransferCount = "gui.createrailwaysnavigator.navigator.route_entry.transfer";
    private static final String keyTrainCanceled = "gui.createrailwaysnavigator.route_overview.stop_canceled";
    private static final String keyAfterJourney = "gui.createrailwaysnavigator.route_overview.after_journey";
    private static final String keyJourneyCompleted = "gui.createrailwaysnavigator.route_overview.journey_completed";
    private static final String keyNextConnections = "gui.createrailwaysnavigator.route_overview.next_connections";
    private static final String keyScheduleTransfer = "gui.createrailwaysnavigator.route_overview.schedule_transfer";
    private static final String keyConnectionEndangered = "gui.createrailwaysnavigator.route_overview.connection_endangered";
    private static final String keyConnectionMissed = "gui.createrailwaysnavigator.route_overview.connection_missed";
    private static final String keyConnectionCanceled = "gui.createrailwaysnavigator.route_overview.connection_canceled";
    private static final String keyConnectionMissedPageText = "gui.createrailwaysnavigator.route_overview.journey_interrupted_info";
    private static final String keyDepartureIn = "gui.createrailwaysnavigator.route_details.departure";
    private static final String keyTimeNow = "gui.createrailwaysnavigator.time.now";
    private static final String keyOptionsText = "gui.createrailwaysnavigator.route_overview.options";
    private static final String keyKeybindOptions = "key.createrailwaysnavigator.route_overlay_options";

    private final UUID listenerId;
    private JourneyListener listener;
    private final UUID clientId = UUID.randomUUID();



    @SuppressWarnings("resource")
    public RouteDetailsOverlayScreen(Level level, SimpleRoute route, int width, int height) {
        this.level = level;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);
        this.listenerId = route.getListenerId();

        xPos = LerpedFloat.linear().startWithValue(width / 2 - (ModClientConfig.OVERLAY_SCALE.get() * (GUI_WIDTH / 2)));
        yPos = LerpedFloat.linear().startWithValue(height / 2 - (ModClientConfig.OVERLAY_SCALE.get() * (GUI_HEIGHT / 2)));

        getListener()
            .registerOnNarratorAnnounce(this, this::narratorAnnouncement)
            .registerOnAnnounceNextStop(this, this::announceNextStop)
            .registerOnContinueWithJourneyAfterStop(this, this::nextStop)
            .registerOnFinishJourney(this, this::finishJourney)
            .registerOnReachNextStop(this, this::reachNextStop)
            .registerOnJourneyBegin(this, this::journeyBegin)
            .registerOnNotification(this, this::notificationReceive)
            .registerOnJourneyInterrupt(this, this::journeyInterrupt)
        ;

        setSlidingText(getListener().getLastInfoText());
        if (getListener().getCurrentState() == State.BEFORE_JOURNEY) {
            if (getListener().getLastNotification() != null) {
                notificationReceive(getListener().getLastNotification());
            }
            setPageJourneyStart();
        } else {
            setPageRouteOverview();
        }
    }

    private float getUIScale() {
        return (float)ModClientConfig.OVERLAY_SCALE.get().doubleValue();
    }

    public JourneyListener getListener() {
        return listener == null ? listener = JourneyListenerManager.getInstance().get(listenerId, this) : listener;
    }

    public UUID getListenerId() {
        return listenerId;
    }

    @Override
    public UUID getJourneyListenerClientId() {
        return clientId;
    }

    @Override
    public void onClose() {
        getListener().unregister(this);
        JourneyListenerManager.getInstance().removeClientListenerForAll(this);
    }


    @Override
    public void tick() {

        if (ModKeys.keyRouteOverlayOptions.isDown() && Minecraft.getInstance().player.getInventory().hasAnyOf(Set.of(ModItems.NAVIGATOR.get()))) {
            ClientWrapper.showRouteOverlaySettingsGui(this);
        }

        xPos.tickChaser();
        yPos.tickChaser();

        // Sliding text
        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * 0.75f) {
            slidingTextOffset--;
            if (slidingTextOffset < -(slidingTextWidth / 2)) {
                slidingTextOffset = (int)((SLIDING_TEXT_AREA_WIDTH + slidingTextWidth / 2) + 20);                
            }
        }
        
        // train info while traveling
        if (getListener() != null && getListener().getCurrentState() == JourneyListener.State.WHILE_TRAVELING) {
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
    }

    //#region FUNCTIONS    
    @Override
    public int getId() {
        return ID;
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

        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * 0.75f) {
            slidingTextOffset = (int)((SLIDING_TEXT_AREA_WIDTH + slidingTextWidth / 2) + 20);
        } else {
            slidingTextOffset = (int)(SLIDING_TEXT_AREA_WIDTH * 0.75f / 2);
        }
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

    private void notificationReceive(NotificationData data) {
        if (ModClientConfig.ROUTE_NOTIFICATIONS.get()) {
            Minecraft.getInstance().getToasts().addToast(NavigatorToast.multiline(data.title(), data.text()));
        }
    }

    private void journeyBegin(JourneyBeginData data) {
        setSlidingText(data.infoText());
        setPageJourneyStart();
    }

    private void nextStop(ContinueData data) {
        setTrainDataSubPage(true);
        setPageRouteOverview();
    }

    private void finishJourney(FinishJourneyData data) {
        setSlidingText(data.infoText());
        setPageJourneyCompleted();
    }

    private void announceNextStop(AnnounceNextStopData data) {
        setSlidingText(data.infoText());
        setPageNextConnections();
    }

    private void reachNextStop(ReachNextStopData data) {
        setSlidingText(data.infoText());

        switch (data.transferState()) {
            case CONNECTION_MISSED:
                setPageConnectionMissed();
                break;
            case DEFAULT:
                setPageTransfer();
                break;
            default:
                break;
        }
    }

    private void journeyInterrupt(JourneyInterruptData data) {
        setSlidingText(data.text());
        setPageJourneyInterrupted(data);
    }

    private void narratorAnnouncement(String text) {
        if (ModClientConfig.ROUTE_NARRATOR.get()) {
            NarratorChatListener.INSTANCE.narrator.say(text, true);
        }
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
            Optional<StationEntry> station = getListener().nextStation();          
            if (station.isPresent()) {
                this.messageLabel = MultiLineLabel.create(shadowlessFont, Utils.translate(keyTransfer,
                    station.get().getTrain().trainName(),
                    station.get().getTrain().scheduleTitle(),
                    station.get().getInfo().platform()
                ), SLIDING_TEXT_AREA_WIDTH - (15 + ModGuiIcons.ICON_SIZE));
            }
            fadeIn(null);
        });
    }

    private void setPageConnectionMissed() {
        fadeOut(() -> {
            currentPage = Page.JOURNEY_INTERRUPTED;
            Optional<StationEntry> station = getListener().previousSation();
            if (station.isPresent()) {
                Component text = Utils.translate(keyConnectionMissedPageText, station.get().getStationName());
                this.messageLabel = MultiLineLabel.create(shadowlessFont, text, SLIDING_TEXT_AREA_WIDTH - 10);
                interruptedText = Utils.translate(keyConnectionMissed);
            }
            fadeIn(null);
        });
    }

    private void setPageJourneyInterrupted(JourneyInterruptData data) {
        fadeOut(() -> {
            currentPage = Page.JOURNEY_INTERRUPTED;
            this.messageLabel = MultiLineLabel.create(shadowlessFont, data.text(), SLIDING_TEXT_AREA_WIDTH - 10);
            interruptedText = Utils.text(data.title().getString());
            fadeIn(null);
        });
    }

    private void setPageJourneyCompleted() {
        fadeOut(() -> {
            currentPage = Page.JOURNEY_END;
            Optional<StationEntry> station = getListener().previousSation();
            if (station.isPresent()) {
                this.messageLabel = MultiLineLabel.create(shadowlessFont, Utils.translate(keyAfterJourney, station.get().getStationName()), SLIDING_TEXT_AREA_WIDTH - 10);
            }
            fadeIn(null);
        });
    }

    private void setPageNextConnections() {
        long id = InstanceManager.registerClientNextConnectionsResponseAction((connections, time) -> {
            this.connections = connections;
            this.connectionsRefreshTime = time;
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
        NetworkManager.sendToServer(new NextConnectionsRequestPacket(id, getListener().currentStation().getTrain().trainId(), getListener().currentStation().getStationName(), getListener().currentStation().getCurrentTicks() + ModClientConfig.TRANSFER_TIME.get()));
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
                setSlidingText(Utils.translate(keyTrainDetails,
                    getListener().currentStation().getTrain().trainName(),
                    getListener().currentStation().getTrain().scheduleTitle()
                ));
                trainDataSubPageTime = 0;
                break;
            case 1:
                long id = InstanceManager.registerClientTrainDataResponseAction((data, time) -> {
                    setSlidingText(Utils.translate(keyTrainSpeed,
                        (int)Math.abs(Math.round(data.speed() * 20 * 3.6F))
                    ));
                    trainDataSubPageTime = 0;
                });
                NetworkManager.sendToServer(new TrainDataRequestPacket(id, getListener().currentStation().getTrain().trainId()));
                break;
        }
    }
    //#endregion

    //#region RENDERING
    @Override
    public void render(ForgeIngameGui gui, PoseStack poseStack, int width, int height, float partialTicks) {
        OverlayPosition pos = ModClientConfig.ROUTE_OVERLAY_POSITION.get();
        final int x = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.BOTTOM_LEFT ? 8 : (int)(width - GUI_WIDTH * getUIScale() - 10);
        final int y = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.TOP_RIGHT ? 8 : (int)(height - GUI_HEIGHT * getUIScale() - 10);

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
        GuiUtils.blit(GUI, poseStack, x, y, 0, getListener().getCurrentState().important() ? 138 : 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
        
        GuiComponent.drawString(poseStack, shadowlessFont, title, x + 6, y + 4, 0x4F4F4F);
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyOptionsText, new KeybindComponent(keyKeybindOptions).withStyle(ChatFormatting.BOLD)), x + 6, y + GUI_HEIGHT - 2 - shadowlessFont.lineHeight, 0x4F4F4F);
        
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + Constants.TIME_SHIFT) % DragonLibConstants.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
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
                case JOURNEY_INTERRUPTED:
                    renderPageJourneyInterrupted(poseStack, x, y + 40, fadePercentage, fontAlpha);
                    break;
                case JOURNEY_END:
                    renderPageJourneyCompleted(poseStack, x, y + 40, fadePercentage, fontAlpha);
                    break;
                case ROUTE_OVERVIEW:
                    final int[] yOffset = new int[] { y + 40 - 1 };
                    for (int i = getListener().getIndex(); i < Math.min(getListener().getIndex() + MAX_STATION_PER_PAGE, getListener().getListeningRoute().getStationCount(true)); i++) {
                        final int k = i;
                        yOffset[0] += renderRouteOverview(poseStack, k, x, yOffset[0], alpha, fontAlpha);
                    }                    
                    break;                    
                default:
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
    }

    public void renderSlidingText(PoseStack poseStack, int x, int y) {
        startStencil(poseStack, x + 3, y + 16, 220, 21);
        poseStack.pushPose();
        poseStack.scale(1.0f / 0.75f, 1.0f / 0.75f, 1.0f / 0.75f);
        GuiComponent.drawCenteredString(poseStack, shadowlessFont, slidingText, (int)((x + 3) + slidingTextOffset), y + 14, 0xFF9900);
        poseStack.popPose();
        endStencil();
    }

    public int renderRouteOverview(PoseStack poseStack, int index, int x, int y, float alphaPercentage, int fontAlpha) {
        RenderSystem.setShaderTexture(0, GUI);
        RenderSystem.setShaderColor(1, 1, 1, alphaPercentage);        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        Optional<StationEntry> stationOptional = getListener().getEntryAt(index);
        //Optional<StationEntry> lastStation = index > getListener().getIndex() ? getListener().getEntryAt(index - 1) : Optional.empty();
        Optional<StationEntry> nextStation = getListener().getEntryAt(index + 1);

        if (!stationOptional.isPresent()) {
            return y;
        }
        StationEntry station = stationOptional.get();

        boolean reachable = station.reachable(false);

        // Icon
        int dY = index <= 0 ? 0 : ROUTE_LINE_HEIGHT;
        final int transferY = ROUTE_LINE_HEIGHT * 3;
        switch (station.getTag()) {
            case PART_START:
                dY = ROUTE_LINE_HEIGHT * 2;
                break;
            case START:
                dY = ROUTE_LINE_HEIGHT * 4;
                break;
            default:
                break;
        }        
        GuiUtils.blit(GUI, poseStack, x + 75, y, 226, dY, 7, ROUTE_LINE_HEIGHT, 256, 256);
        if (index >= getListener().getIndex() + MAX_STATION_PER_PAGE - 1 && station.getTag() != StationTag.END) {            
            GuiUtils.blit(GUI, poseStack, x + 75, y + ROUTE_LINE_HEIGHT, 226, ROUTE_LINE_HEIGHT, 7, ROUTE_LINE_HEIGHT, 256, 256);
        }

        // time display
        if (station.isTrainCanceled()) {
            GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyTrainCanceled), x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, DELAYED | fontAlpha);  
        } else {
            long timeDiff = station.getDifferenceTime();
            MutableComponent timeText = Utils.text(TimeUtils.parseTime((int)(station.getScheduleTime() + Constants.TIME_SHIFT), ModClientConfig.TIME_FORMAT.get())).withStyle(reachable ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH);
            
            float scale = shadowlessFont.width(timeText) >= 30 ? 0.7F : 1;
            poseStack.pushPose();
            poseStack.scale(scale, 1, 1);
            GuiComponent.drawString(poseStack, shadowlessFont, timeText, (int)((x + 10) / scale), y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, reachable ? (index <= getListener().getIndex() ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha);
            poseStack.popPose();
            
            if (station.reachable(false) && station.shouldRenderRealtime()) {
                MutableComponent realtimeText = Utils.text(TimeUtils.parseTime((int)(station.getEstimatedTimeWithThreshold() + Constants.TIME_SHIFT), ModClientConfig.TIME_FORMAT.get())).withStyle(reachable ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH);
                
                float realtimeScale = shadowlessFont.width(realtimeText) >= 30 ? 0.7F : 1;
                poseStack.pushPose();
                poseStack.scale(realtimeScale, 1, 1);                
                GuiComponent.drawString(poseStack, shadowlessFont, realtimeText, (int)((x + 40) / realtimeScale), y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, timeDiff < ModClientConfig.DEVIATION_THRESHOLD.get() && reachable ? ON_TIME | fontAlpha : DELAYED | fontAlpha);
                poseStack.popPose();
            }
        }

        // station name display
        Component platformText = Utils.text(station.getUpdatedInfo().platform());
        int platformTextWidth = shadowlessFont.width(platformText);

        final int maxStationNameWidth = SLIDING_TEXT_AREA_WIDTH - platformTextWidth - 105;
        MutableComponent stationText = Utils.text(station.getStationName());
        if (index == getListener().getIndex()) stationText = stationText.withStyle(ChatFormatting.BOLD);
        if (!reachable) stationText = stationText.withStyle(ChatFormatting.STRIKETHROUGH);
        if (shadowlessFont.width(stationText) > maxStationNameWidth) {
            stationText = Utils.text(shadowlessFont.substrByWidth(stationText, maxStationNameWidth).getString()).append(Utils.text("...")).withStyle(stationText.getStyle());
        }

        GuiComponent.drawString(poseStack, shadowlessFont, stationText, x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, reachable ? (index <= getListener().getIndex() ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha);
        GuiComponent.drawString(poseStack, shadowlessFont, platformText, x + SLIDING_TEXT_AREA_WIDTH - platformTextWidth, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, reachable && !station.stationInfoChanged() ? (index <= getListener().getIndex() ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha);
        
        // render transfer
        if (station.getTag() == StationTag.PART_END) {
            y += ROUTE_LINE_HEIGHT;
            RenderSystem.setShaderColor(1, 1, 1, alphaPercentage);        
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            GuiUtils.blit(GUI, poseStack, x + 75, y, 226, transferY, 7, ROUTE_LINE_HEIGHT, 256, 256);
            if (nextStation.isPresent() && !nextStation.get().reachable(true)) {
                if (nextStation.get().isDeparted() || nextStation.get().isTrainCanceled()) {
                    ModGuiIcons.CROSS.render(poseStack, x + 10, y + ROUTE_LINE_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
                    GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(nextStation.get().isTrainCanceled() ? keyConnectionCanceled : keyConnectionMissed).withStyle(ChatFormatting.BOLD), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, DELAYED | fontAlpha);
                } else {
                    ModGuiIcons.WARN.render(poseStack, x + 10, y + ROUTE_LINE_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
                    GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyConnectionEndangered).withStyle(ChatFormatting.BOLD), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, COLOR_WARN | fontAlpha);
                }
            } else {
                GuiComponent.drawString(poseStack, shadowlessFont, Utils.text(TimeUtils.parseDurationShort((int)(getListener().getTransferTime(index)))).withStyle(ChatFormatting.ITALIC), x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, 0xDBDBDB | fontAlpha);
                GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyScheduleTransfer).withStyle(ChatFormatting.ITALIC), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, 0xDBDBDB | fontAlpha);
            }
                    
            return ROUTE_LINE_HEIGHT * 2;
        }
        return ROUTE_LINE_HEIGHT;
        
    }

    public void renderNextConnections(PoseStack poseStack, int x, int y, float alphaPercentage, int fontAlpha, StationEntry station) {
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyNextConnections).withStyle(ChatFormatting.BOLD), x + 10, y + 4, 0xFFFFFF | fontAlpha);

        SimpleTrainConnection[] conns = connections.toArray(SimpleTrainConnection[]::new);
        for (int i = connectionsSubPageIndex * CONNECTION_ENTRIES_PER_PAGE, k = 0; i < Math.min((connectionsSubPageIndex + 1) * CONNECTION_ENTRIES_PER_PAGE, connections.size()); i++, k++) {
            MutableComponent time = Utils.text(TimeUtils.parseTime((int)((connectionsRefreshTime + conns[i].ticks() + Constants.TIME_SHIFT) % DragonLibConstants.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get()));
            MutableComponent platform = Utils.text(conns[i].stationDetails().platform());

            int x1 = x + 10;
            int x2 = x + 55;
            int x3 = x + 100;
            int x4 = x + SLIDING_TEXT_AREA_WIDTH - shadowlessFont.width(platform);            

            final int maxTrainNameWidth = x3 - x2 - 5;
            MutableComponent trainName = Utils.text(conns[i].trainName());
            if (shadowlessFont.width(trainName) > maxTrainNameWidth) {
                trainName = Utils.text(shadowlessFont.substrByWidth(trainName, maxTrainNameWidth).getString()).append(Utils.text("..."));
            }

            final int maxDestinationWidth = x4 - x3 - 5;
            MutableComponent destination = Utils.text(conns[i].scheduleTitle());
            if (shadowlessFont.width(destination) > maxDestinationWidth) {
                destination = Utils.text(shadowlessFont.substrByWidth(destination, maxDestinationWidth).getString()).append(Utils.text("..."));
            }

            GuiComponent.drawString(poseStack, shadowlessFont, time, x1, y + 15 + 12 * k, 0xDBDBDB | fontAlpha);
            GuiComponent.drawString(poseStack, shadowlessFont, trainName, x2, y + 15 + 12 * k, 0xDBDBDB | fontAlpha);
            GuiComponent.drawString(poseStack, shadowlessFont, destination, x3, y + 15 + 12 * k, 0xDBDBDB | fontAlpha);
            GuiComponent.drawString(poseStack, shadowlessFont, platform, x4, y + 15 + 12 * k, 0xDBDBDB | fontAlpha);
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
        y += 3 + renderRouteOverview(poseStack, getListener().getIndex(), x, y - 3, alphaPercentage, fontAlpha);
        GuiComponent.fill(poseStack, x + 3, y, x + 3 + SLIDING_TEXT_AREA_WIDTH, y + 1, 0xDBDBDB | fontAlpha);
        
        // Title
        ModGuiIcons.TIME.render(poseStack, x + 10, y + 3);
        long time = getListener().currentStation().getEstimatedTimeWithThreshold() - level.getDayTime();
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyDepartureIn).append(" ").append(time > 0 ? Utils.text(TimeUtils.parseTime((int)(time % 24000), TimeFormat.HOURS_24)) : Utils.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, 0xFFFFFF | fontAlpha);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        final int detailsLineHeight = 12;
        //StationEntry station = taggedRoute[0];
        StationEntry endStation = getListener().lastStation();

        Component platformText = Utils.text(endStation.getInfo().platform());
        int platformTextWidth = shadowlessFont.width(platformText);
        final int maxStationNameWidth = SLIDING_TEXT_AREA_WIDTH - platformTextWidth - 10 - 5;
        MutableComponent stationText = Utils.text(TimeUtils.parseTime((int)(endStation.getEstimatedTimeWithThreshold() + Constants.TIME_SHIFT % 24000), ModClientConfig.TIME_FORMAT.get())).append(Utils.text(" " + endStation.getStationName()));
        if (shadowlessFont.width(stationText) > maxStationNameWidth) {
            stationText = Utils.text(shadowlessFont.substrByWidth(stationText, maxStationNameWidth).getString()).append(Utils.text("...")).withStyle(stationText.getStyle());
        }

        ModGuiIcons.TARGET.render(poseStack, x + 10, y + shadowlessFont.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiComponent.drawString(poseStack, shadowlessFont, stationText, x + 15 + ModGuiIcons.ICON_SIZE, y, 0xDBDBDB | fontAlpha);
        GuiComponent.drawString(poseStack, shadowlessFont, platformText, x + SLIDING_TEXT_AREA_WIDTH - platformTextWidth, y, endStation.stationInfoChanged() ? DELAYED | fontAlpha : 0xDBDBDB | fontAlpha);
        ModGuiIcons.INFO.render(poseStack, x + 10, y + detailsLineHeight + shadowlessFont.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.text(String.format("%s %s | %s",
            getListener().getListeningRoute().getTransferCount(),
            Utils.translate(keyTransferCount).getString(),
            TimeUtils.parseDurationShort(getListener().getListeningRoute().getTotalDuration())
        )), x + 15 + ModGuiIcons.ICON_SIZE, y + detailsLineHeight, 0xDBDBDB | fontAlpha);
    }

    public void renderPageTransfer(PoseStack poseStack, int x, int y, float alphaPercentage, int fontAlpha, StationEntry station) {
        y += 3 + renderRouteOverview(poseStack, getListener().getIndex(), x, y - 3, alphaPercentage, fontAlpha);
        GuiComponent.fill(poseStack, x + 3, y, x + 3 + SLIDING_TEXT_AREA_WIDTH, y + 1, 0xDBDBDB | fontAlpha);
        
        // Title
        ModGuiIcons.WALK.render(poseStack, x + 10, y + 3);        
        long transferTime = getListener().currentStation().getEstimatedTimeWithThreshold() - level.getDayTime();//getListener().getTransferTime(getListener().getIndex());
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyScheduleTransfer).append(" ").append(transferTime > 0 ? Utils.text(TimeUtils.parseTime((int)(transferTime % 24000), TimeFormat.HOURS_24)) : Utils.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, 0xFFFFFF | fontAlpha);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(poseStack, x + 15 + ModGuiIcons.ICON_SIZE, y, 12, 0xDBDBDB | fontAlpha);
    }

    public void renderPageJourneyInterrupted(PoseStack poseStack, int x, int y, float alphaPercentage, int fontAlpha) {
        // Title
        ModGuiIcons.CROSS.render(poseStack, x + 10, y + 3);
        GuiComponent.drawString(poseStack, shadowlessFont, interruptedText.withStyle(ChatFormatting.BOLD), x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, DELAYED | fontAlpha);
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
        NONE,
        ROUTE_OVERVIEW,
        NEXT_CONNECTIONS,
        JOURNEY_START,
        JOURNEY_END,
        TRANSFER,
        JOURNEY_INTERRUPTED;
    }
}
