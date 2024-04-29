package de.mrjulsen.crn.client.gui.overlay;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.ModGuiUtils;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.NavigatorToast;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleTrainConnection;
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
import de.mrjulsen.crn.network.packets.cts.NextConnectionsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket;
import de.mrjulsen.crn.registry.ModItems;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLOverlayScreen;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils.TimeFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class RouteDetailsOverlayScreen extends DLOverlayScreen implements IJourneyListenerClient {

    private static final ResourceLocation GUI = new ResourceLocation(ExampleMod.MOD_ID, "textures/gui/overview.png");
    private static final Component title = TextUtils.translate("gui.createrailwaysnavigator.route_overview.title");
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

    private Component slidingText = TextUtils.empty();
    private float slidingTextOffset = 0;
    private int slidingTextWidth = 0;
    
    private MultiLineLabel messageLabel;
    private MutableComponent interruptedText;

    private LerpedFloat xPos;
    private LerpedFloat yPos;
    
    private static final String keyTrainDetails = "gui.createrailwaysnavigator.route_overview.train_details";
    private static final String keyTrainSpeed = "gui.createrailwaysnavigator.route_overview.train_speed";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyTransferWithPlatform = "gui.createrailwaysnavigator.route_overview.transfer_with_platform";
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
            if (getListener().lastNarratorText() != null) {
                narratorAnnouncement(getListener().lastNarratorText());
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
        if (Screen.hasControlDown() && ModKeys.KEY_OVERLAY_SETTINGS.isDown() && Minecraft.getInstance().player.getInventory().hasAnyOf(Set.of(ModItems.NAVIGATOR.get()))) {
            ClientWrapper.showRouteOverlaySettingsGui(this);
        }

        xPos.tickChaser();
        yPos.tickChaser();

        //tickSlidingText(2);
        
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

    public void tickSlidingText(float delta) {
        // Sliding text
        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * 0.75f) {
            slidingTextOffset -= delta;
            if (slidingTextOffset < -(slidingTextWidth / 2)) {
                slidingTextOffset = (int)((SLIDING_TEXT_AREA_WIDTH + slidingTextWidth / 2) + 20);                
            }
        }
    }

    //#region FUNCTIONS

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

    private void startStencil(Graphics graphics, int x, int y, int w, int h) {
        UIRenderHelper.swapAndBlitColor(Minecraft.getInstance().getMainRenderTarget(), UIRenderHelper.framebuffer);
        ModGuiUtils.startStencil(graphics, x, y, w, h);
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
            StationEntry station = getListener().currentStation();          
            if (station != null) {
                this.messageLabel = MultiLineLabel.create(shadowlessFont,
                station.getInfo().platform() == null || station.getInfo().platform().isBlank() ?
                TextUtils.translate(keyTransfer,
                    station.getTrain().trainName(),
                    station.getTrain().scheduleTitle()
                ) : 
                TextUtils.translate(keyTransferWithPlatform,
                    station.getTrain().trainName(),
                    station.getTrain().scheduleTitle(),
                    station.getInfo().platform()
                ), SLIDING_TEXT_AREA_WIDTH - (15 + ModGuiIcons.ICON_SIZE));
            }
            fadeIn(null);
        });
    }

    private void setPageConnectionMissed() {
        fadeOut(() -> {
            currentPage = Page.JOURNEY_INTERRUPTED;
            StationEntry station = getListener().lastStation();
            if (station != null) {
                Component text = TextUtils.translate(keyConnectionMissedPageText, station.getStationName());
                this.messageLabel = MultiLineLabel.create(shadowlessFont, text, SLIDING_TEXT_AREA_WIDTH - 10);
                interruptedText = TextUtils.translate(keyConnectionMissed);
            }
            fadeIn(null);
        });
    }

    private void setPageJourneyInterrupted(JourneyInterruptData data) {
        fadeOut(() -> {
            currentPage = Page.JOURNEY_INTERRUPTED;
            this.messageLabel = MultiLineLabel.create(shadowlessFont, data.text(), SLIDING_TEXT_AREA_WIDTH - 10);
            interruptedText = TextUtils.text(data.title().getString());
            fadeIn(null);
        });
    }

    private void setPageJourneyCompleted() {
        fadeOut(() -> {
            currentPage = Page.JOURNEY_END;
            StationEntry station = getListener().currentStation();
            if (station != null) {
                this.messageLabel = MultiLineLabel.create(shadowlessFont, TextUtils.translate(keyAfterJourney, station.getStationName()), SLIDING_TEXT_AREA_WIDTH - 10);
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
        ExampleMod.net().CHANNEL.sendToServer(new NextConnectionsRequestPacket(id, getListener().currentStation().getTrain().trainId(), getListener().currentStation().getStationName(), getListener().currentStation().getCurrentTicks() + ModClientConfig.TRANSFER_TIME.get()));
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
                setSlidingText(TextUtils.translate(keyTrainDetails,
                    getListener().currentStation().getTrain().trainName(),
                    getListener().currentStation().getTrain().scheduleTitle()
                ));
                trainDataSubPageTime = 0;
                break;
            case 1:
                long id = InstanceManager.registerClientTrainDataResponseAction((data, time) -> {
                    setSlidingText(TextUtils.translate(keyTrainSpeed,
                        ModUtils.calcSpeedString(data.speed(), ModClientConfig.SPEED_UNIT.get())
                    ));
                    trainDataSubPageTime = 0;
                });
                ExampleMod.net().CHANNEL.sendToServer(new TrainDataRequestPacket(id, getListener().currentStation().getTrain().trainId(), false));
                break;
        }
    }
    //#endregion

    //#region RENDERING
    @Override
    public void render(Graphics graphics, float partialTicks, int width, int height) {
        width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        partialTicks = Minecraft.getInstance().getFrameTime();
        OverlayPosition pos = ModClientConfig.ROUTE_OVERLAY_POSITION.get();
        final int x = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.BOTTOM_LEFT ? 8 : (int)(width - GUI_WIDTH * getUIScale() - 10);
        final int y = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.TOP_RIGHT ? 8 : (int)(height - GUI_HEIGHT * getUIScale() - 10);

        xPos.chase(x, 0.2f, Chaser.EXP);
        yPos.chase(y, 0.2f, Chaser.EXP);

        graphics.poseStack().pushPose();
        graphics.poseStack().translate((int)xPos.getValue(partialTicks), (int)yPos.getValue(partialTicks), 0);
        renderInternal(graphics, 0, 0, width, height, partialTicks);
        graphics.poseStack().popPose();

        tickSlidingText(2 * Minecraft.getInstance().getDeltaFrameTime());
    }

    private void renderInternal(Graphics graphics, int x, int y, int width, int height, float partialTicks) {
        graphics.poseStack().pushPose();
        float fadePercentage = this.fading ? Mth.clamp((float)(Util.getMillis() - this.fadeStart) / 500.0F, 0.0F, 1.0F) : 1.0F;
        float alpha = fadeInvert ? Mth.clamp(1.0f - fadePercentage, 0, 1) : Mth.clamp(fadePercentage, 0, 1);
        int fontAlpha = Mth.ceil(alpha * 255.0F) << 24; // <color> | fontAlpha

        graphics.poseStack().scale(getUIScale(), getUIScale(), getUIScale());
        RenderSystem.setShaderTexture(0, GUI);
        GuiUtils.drawTexture(GUI, graphics, x, y, GUI_WIDTH, GUI_HEIGHT, 0, getListener().getCurrentState().important() ? 138 : 0, 256, 256);
        
        GuiUtils.drawString(graphics, shadowlessFont, x + 6, y + 4, title, 0x4F4F4F, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, shadowlessFont, x + 6, y + GUI_HEIGHT - 2 - shadowlessFont.lineHeight, TextUtils.translate(keyOptionsText, TextUtils.translate(InputConstants.getKey(Minecraft.ON_OSX ? InputConstants.KEY_LWIN : InputConstants.KEY_LCONTROL, 0).getName()).append(" + ").append(new KeybindComponent(keyKeybindOptions)).withStyle(ChatFormatting.BOLD)), 0x4F4F4F, EAlignment.LEFT, false);
        
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        GuiUtils.drawString(graphics, shadowlessFont, x + GUI_WIDTH - 4 - shadowlessFont.width(timeString), y + 4, timeString, 0x4F4F4F, EAlignment.LEFT, false);
        
        // Test
        renderSlidingText(graphics, x, y + 2);

        startStencil(graphics, x + 3, y + 40, 220, 62);
        graphics.poseStack().pushPose();

        graphics.poseStack().translate((fadeInvert ? 0 : -20) + fadePercentage * 20, 0, 0);
        if (alpha > 0.1f && (fontAlpha & -67108864) != 0) {
            
            switch (currentPage) {
                case JOURNEY_START:
                    renderPageJourneyStart(graphics, x, y + 40, fadePercentage, fontAlpha);
                    break;
                case NEXT_CONNECTIONS:
                    renderNextConnections(graphics, x, y + 40, fadePercentage, fontAlpha, null);
                    break;
                case TRANSFER:
                    renderPageTransfer(graphics, x, y + 40, fadePercentage, fontAlpha, null);
                    break;
                case JOURNEY_INTERRUPTED:
                    renderPageJourneyInterrupted(graphics, x, y + 40, fadePercentage, fontAlpha);
                    break;
                case JOURNEY_END:
                    renderPageJourneyCompleted(graphics, x, y + 40, fadePercentage, fontAlpha);
                    break;
                case ROUTE_OVERVIEW:
                    final int[] yOffset = new int[] { y + 40 - 1 };
                    for (int i = getListener().getIndex(); i < Math.min(getListener().getIndex() + MAX_STATION_PER_PAGE, getListener().getListeningRoute().getStationCount(true)); i++) {
                        final int k = i;
                        yOffset[0] += renderRouteOverview(graphics, k, x, yOffset[0], alpha, fontAlpha);
                    }                    
                    break;                    
                default:
                    break;
            }
        }

        graphics.poseStack().popPose();
        endStencil();
        graphics.poseStack().popPose();

        if (fadePercentage >= 1.0f) {
            fading = false;
            if (fadeDoneAction != null) {
                fadeDoneAction.run();
            }
        }
    }

    public void renderSlidingText(Graphics graphics, int x, int y) {
        startStencil(graphics, x + 3, y + 16, 220, 21);
        graphics.poseStack().pushPose();        
        graphics.poseStack().scale(1.0f / 0.75f, 1.0f / 0.75f, 1.0f / 0.75f);
        GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 3) + slidingTextOffset), y + 14, slidingText, 0xFF9900, EAlignment.CENTER, false);
        graphics.poseStack().popPose();
        endStencil();
    }

    public int renderRouteOverview(Graphics graphics, int index, int x, int y, float alphaPercentage, int fontAlpha) {
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
        GuiUtils.drawTexture(GUI, graphics, x + 75, y, 7, ROUTE_LINE_HEIGHT, 226, dY, 256, 256);
        if (index >= getListener().getIndex() + MAX_STATION_PER_PAGE - 1 && station.getTag() != StationTag.END) {            
            GuiUtils.drawTexture(GUI, graphics, x + 75, y + ROUTE_LINE_HEIGHT, 7, ROUTE_LINE_HEIGHT, 226, ROUTE_LINE_HEIGHT, 256, 256);
        }

        // time display
        if (station.isTrainCanceled()) {
            GuiUtils.drawString(graphics, shadowlessFont, x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, TextUtils.translate(keyTrainCanceled), DELAYED | fontAlpha, EAlignment.LEFT, false);
        } else {
            long timeDiff = station.getDifferenceTime();
            MutableComponent timeText = TextUtils.text(TimeUtils.parseTime((int)((station.getScheduleTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get())).withStyle(reachable ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH);
            
            float scale = shadowlessFont.width(timeText) >= 30 ? 0.7F : 1;
            graphics.poseStack().pushPose();
            graphics.poseStack().scale(scale, 1, 1);
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 10) / scale), y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, timeText, reachable ? (index <= getListener().getIndex() ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha, EAlignment.LEFT, false);
            graphics.poseStack().popPose();
            
            if (station.reachable(false) && station.shouldRenderRealtime()) {
                MutableComponent realtimeText = TextUtils.text(TimeUtils.parseTime((int)((station.getEstimatedTimeWithThreshold() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get())).withStyle(reachable ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH);
                
                float realtimeScale = shadowlessFont.width(realtimeText) >= 30 ? 0.7F : 1;
                graphics.poseStack().pushPose();
                graphics.poseStack().scale(realtimeScale, 1, 1);                
                GuiUtils.drawString(graphics, shadowlessFont, (int)((x + 40) / realtimeScale), y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, realtimeText, timeDiff < ModClientConfig.DEVIATION_THRESHOLD.get() && reachable ? ON_TIME | fontAlpha : DELAYED | fontAlpha, EAlignment.LEFT, false);
                graphics.poseStack().popPose();
            }
        }

        // station name display
        Component platformText = TextUtils.text(station.getUpdatedInfo().platform());
        int platformTextWidth = shadowlessFont.width(platformText);

        final int maxStationNameWidth = SLIDING_TEXT_AREA_WIDTH - platformTextWidth - 105;
        MutableComponent stationText = TextUtils.text(station.getStationName());
        if (index == getListener().getIndex()) stationText = stationText.withStyle(ChatFormatting.BOLD);
        if (!reachable) stationText = stationText.withStyle(ChatFormatting.STRIKETHROUGH);
        if (shadowlessFont.width(stationText) > maxStationNameWidth) {
            stationText = TextUtils.text(shadowlessFont.substrByWidth(stationText, maxStationNameWidth).getString()).append(TextUtils.text("...")).withStyle(stationText.getStyle());
        }

        GuiUtils.drawString(graphics, shadowlessFont, x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, stationText, reachable ? (index <= getListener().getIndex() ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, shadowlessFont, x + SLIDING_TEXT_AREA_WIDTH - platformTextWidth, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, platformText, reachable && !station.stationInfoChanged() ? (index <= getListener().getIndex() ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha, EAlignment.LEFT, false);
        
        // render transfer
        if (station.getTag() == StationTag.PART_END) {
            y += ROUTE_LINE_HEIGHT;
            RenderSystem.setShaderColor(1, 1, 1, alphaPercentage);        
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            GuiUtils.drawTexture(GUI, graphics, x + 75, y, 7, ROUTE_LINE_HEIGHT,  226, transferY,256, 256);
            if (nextStation.isPresent() && !nextStation.get().reachable(true)) {
                if (nextStation.get().isDeparted() || nextStation.get().isTrainCanceled()) {
                    ModGuiIcons.CROSS.render(graphics, x + 10, y + ROUTE_LINE_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
                    GuiUtils.drawString(graphics, shadowlessFont, x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, TextUtils.translate(nextStation.get().isTrainCanceled() ? keyConnectionCanceled : keyConnectionMissed).withStyle(ChatFormatting.BOLD), DELAYED | fontAlpha, EAlignment.LEFT, false);
                } else {
                    ModGuiIcons.WARN.render(graphics, x + 10, y + ROUTE_LINE_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
                    GuiUtils.drawString(graphics, shadowlessFont, x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, TextUtils.translate(keyConnectionEndangered).withStyle(ChatFormatting.BOLD), COLOR_WARN | fontAlpha, EAlignment.LEFT, false);
                }
            } else {
                GuiUtils.drawString(graphics, shadowlessFont, x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, TextUtils.text(TimeUtils.parseDurationShort((int)(getListener().getTransferTime(index)))).withStyle(ChatFormatting.ITALIC), 0xDBDBDB | fontAlpha, EAlignment.LEFT, false);
                GuiUtils.drawString(graphics, shadowlessFont, x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, TextUtils.translate(keyScheduleTransfer).withStyle(ChatFormatting.ITALIC), 0xDBDBDB | fontAlpha, EAlignment.LEFT, false);
            }
                    
            return ROUTE_LINE_HEIGHT * 2;
        }
        return ROUTE_LINE_HEIGHT;
        
    }

    public void renderNextConnections(Graphics graphics, int x, int y, float alphaPercentage, int fontAlpha, StationEntry station) {
        GuiUtils.drawString(graphics, shadowlessFont, x + 10, y + 4, TextUtils.translate(keyNextConnections).withStyle(ChatFormatting.BOLD), 0xFFFFFF | fontAlpha, EAlignment.LEFT, false);

        SimpleTrainConnection[] conns = connections.toArray(SimpleTrainConnection[]::new);
        for (int i = connectionsSubPageIndex * CONNECTION_ENTRIES_PER_PAGE, k = 0; i < Math.min((connectionsSubPageIndex + 1) * CONNECTION_ENTRIES_PER_PAGE, connections.size()); i++, k++) {
            MutableComponent time = TextUtils.text(TimeUtils.parseTime((int)((connectionsRefreshTime + conns[i].ticks() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get()));
            MutableComponent platform = TextUtils.text(conns[i].stationDetails().platform());

            int x1 = x + 10;
            int x2 = x + 55;
            int x3 = x + 100;
            int x4 = x + SLIDING_TEXT_AREA_WIDTH - shadowlessFont.width(platform);            

            final int maxTrainNameWidth = x3 - x2 - 5;
            MutableComponent trainName = TextUtils.text(conns[i].trainName());
            if (shadowlessFont.width(trainName) > maxTrainNameWidth) {
                trainName = TextUtils.text(shadowlessFont.substrByWidth(trainName, maxTrainNameWidth).getString()).append(TextUtils.text("..."));
            }

            final int maxDestinationWidth = x4 - x3 - 5;
            MutableComponent destination = TextUtils.text(conns[i].scheduleTitle());
            if (shadowlessFont.width(destination) > maxDestinationWidth) {
                destination = TextUtils.text(shadowlessFont.substrByWidth(destination, maxDestinationWidth).getString()).append(TextUtils.text("..."));
            }

            GuiUtils.drawString(graphics, shadowlessFont, x1, y + 15 + 12 * k, time, 0xDBDBDB | fontAlpha, EAlignment.LEFT, false);
            GuiUtils.drawString(graphics, shadowlessFont, x2, y + 15 + 12 * k, trainName, 0xDBDBDB | fontAlpha, EAlignment.LEFT, false);
            GuiUtils.drawString(graphics, shadowlessFont, x3, y + 15 + 12 * k, destination, 0xDBDBDB | fontAlpha, EAlignment.LEFT, false);
            GuiUtils.drawString(graphics, shadowlessFont, x4, y + 15 + 12 * k, platform, 0xDBDBDB | fontAlpha, EAlignment.LEFT, false);
        }

        // page
        final int dotSize = 4;
        final int dotY = y + 62 - 10;
        final int startX = x + GUI_WIDTH / 2 - connectionsSubPagesCount * dotSize - dotSize;

        for (int i = 0; i < connectionsSubPagesCount; i++) {
            int s = dotSize + (i == connectionsSubPageIndex ? 2 : 0);
            int dX = startX + i * dotSize * 3 - (i == connectionsSubPageIndex ? 1 : 0);
            int dY = dotY - (i == connectionsSubPageIndex ? 1 : 0);
            GuiUtils.fill(graphics, dX, dY, s, s, i == connectionsSubPageIndex ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha);
            GuiUtils.fill(graphics, dX + 1, dY + 1, s - 2, s - 2, i == connectionsSubPageIndex ? 0xAAAAAA | fontAlpha : 0x888888 | fontAlpha);
        }

    }

    public void renderPageJourneyStart(Graphics graphics, int x, int y, float alphaPercentage, int fontAlpha) {
        y += 3 + renderRouteOverview(graphics, getListener().getIndex(), x, y - 3, alphaPercentage, fontAlpha);
        GuiUtils.fill(graphics, x + 3, y, SLIDING_TEXT_AREA_WIDTH, 1, 0xDBDBDB | fontAlpha);
        
        // Title
        ModGuiIcons.TIME.render(graphics, x + 10, y + 3);
        long time = getListener().currentStation().getEstimatedTimeWithThreshold() - level.getDayTime();
        GuiUtils.drawString(graphics, shadowlessFont, x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, TextUtils.translate(keyDepartureIn).append(" ").append(time > 0 ? TextUtils.text(TimeUtils.parseTime((int)(time % DragonLib.TICKS_PER_DAY), TimeFormat.HOURS_24)) : TextUtils.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), 0xFFFFFF | fontAlpha, EAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        final int detailsLineHeight = 12;
        //StationEntry station = taggedRoute[0];
        StationEntry endStation = getListener().lastStation();

        Component platformText = TextUtils.text(endStation.getInfo().platform());
        int platformTextWidth = shadowlessFont.width(platformText);
        final int maxStationNameWidth = SLIDING_TEXT_AREA_WIDTH - platformTextWidth - 10 - 5;
        MutableComponent stationText = TextUtils.text(TimeUtils.parseTime((int)((endStation.getEstimatedTimeWithThreshold() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get())).append(TextUtils.text(" " + endStation.getStationName()));
        if (shadowlessFont.width(stationText) > maxStationNameWidth) {
            stationText = TextUtils.text(shadowlessFont.substrByWidth(stationText, maxStationNameWidth).getString()).append(TextUtils.text("...")).withStyle(stationText.getStyle());
        }

        ModGuiIcons.TARGET.render(graphics, x + 10, y + shadowlessFont.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiUtils.drawString(graphics, shadowlessFont, x + 15 + ModGuiIcons.ICON_SIZE, y, stationText, 0xDBDBDB | fontAlpha,  EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, shadowlessFont, x + SLIDING_TEXT_AREA_WIDTH - platformTextWidth, y, platformText, endStation.stationInfoChanged() ? DELAYED | fontAlpha : 0xDBDBDB | fontAlpha, EAlignment.LEFT, false);
        ModGuiIcons.INFO.render(graphics, x + 10, y + detailsLineHeight + shadowlessFont.lineHeight / 2 - ModGuiIcons.ICON_SIZE / 2);
        GuiUtils.drawString(graphics, shadowlessFont, x + 15 + ModGuiIcons.ICON_SIZE, y + detailsLineHeight, TextUtils.text(String.format("%s %s | %s",
            getListener().getListeningRoute().getTransferCount(),
            TextUtils.translate(keyTransferCount).getString(),
            TimeUtils.parseDurationShort(getListener().getListeningRoute().getTotalDuration())
        )), 0xDBDBDB | fontAlpha, EAlignment.LEFT, false);
    }

    public void renderPageTransfer(Graphics graphics, int x, int y, float alphaPercentage, int fontAlpha, StationEntry station) {
        y += 3 + renderRouteOverview(graphics, getListener().getIndex(), x, y - 3, alphaPercentage, fontAlpha);
        GuiUtils.fill(graphics, x + 3, y, SLIDING_TEXT_AREA_WIDTH, 1, 0xDBDBDB | fontAlpha);
        
        // Title
        ModGuiIcons.WALK.render(graphics, x + 10, y + 3);        
        long transferTime = getListener().currentStation().getEstimatedTimeWithThreshold() - level.getDayTime();//getListener().getTransferTime(getListener().getIndex());
        GuiUtils.drawString(graphics, shadowlessFont, x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, TextUtils.translate(keyScheduleTransfer).append(" ").append(transferTime > 0 ? TextUtils.text(TimeUtils.parseTime((int)(transferTime % DragonLib.TICKS_PER_DAY), TimeFormat.HOURS_24)) : TextUtils.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), 0xFFFFFF | fontAlpha, EAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(graphics.poseStack(), x + 15 + ModGuiIcons.ICON_SIZE, y, 12, 0xDBDBDB | fontAlpha);
    }

    public void renderPageJourneyInterrupted(Graphics graphics, int x, int y, float alphaPercentage, int fontAlpha) {
        // Title
        ModGuiIcons.CROSS.render(graphics, x + 10, y + 3);
        GuiUtils.drawString(graphics, shadowlessFont, x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, interruptedText.withStyle(ChatFormatting.BOLD), DELAYED | fontAlpha, EAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(graphics.poseStack(), x + 10, y, 10, 0xDBDBDB | fontAlpha);
    }

    public void renderPageJourneyCompleted(Graphics graphics, int x, int y, float alphaPercentage, int fontAlpha) {
        // Title
        ModGuiIcons.CHECK.render(graphics, x + 10, y + 3);
        GuiUtils.drawString(graphics, shadowlessFont, x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, TextUtils.translate(keyJourneyCompleted).withStyle(ChatFormatting.BOLD), ON_TIME | fontAlpha, EAlignment.LEFT, false);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        this.messageLabel.renderLeftAligned(graphics.poseStack(), x + 10, y, 10, 0xDBDBDB | fontAlpha);
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
