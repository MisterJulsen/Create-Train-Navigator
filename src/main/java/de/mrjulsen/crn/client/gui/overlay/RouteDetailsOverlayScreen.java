package de.mrjulsen.crn.client.gui.overlay;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
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
import de.mrjulsen.crn.data.SimpleRoute.StationEntry;
import de.mrjulsen.crn.data.SimpleRoute.StationTag;
import de.mrjulsen.crn.data.SimpleRoute.TaggedStationEntry;
import de.mrjulsen.crn.event.listeners.JourneyListener;
import de.mrjulsen.crn.event.listeners.JourneyListener.AnnounceNextStopData;
import de.mrjulsen.crn.event.listeners.JourneyListener.ContinueData;
import de.mrjulsen.crn.event.listeners.JourneyListener.FinishJourneyData;
import de.mrjulsen.crn.event.listeners.JourneyListener.JourneyBeginEvent;
import de.mrjulsen.crn.event.listeners.JourneyListener.ReachNextStopData;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.NextConnectionsRequestPacket;
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

    private Page currentPage = Page.ROUTE_OVERVIEW;

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

    private final Font shadowlessFont;

    private Component slidingText;
    private float slidingTextOffset = 0;
    private int slidingTextWidth = 0;
    
    private MultiLineLabel messageLabel;

    private LerpedFloat xPos = LerpedFloat.linear().startWithValue(0);
    private LerpedFloat yPos = LerpedFloat.linear().startWithValue(0);
    
    private static final String keyTrainDetails = "gui.createrailwaysnavigator.route_overview.train_details";
    private static final String keyTrainSpeed = "gui.createrailwaysnavigator.route_overview.train_speed";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyTransferCount = "gui.createrailwaysnavigator.navigator.route_entry.transfer";
    private static final String keyAfterJourney = "gui.createrailwaysnavigator.route_overview.after_journey";
    private static final String keyJourneyCompleted = "gui.createrailwaysnavigator.route_overview.journey_completed";
    private static final String keyNextConnections = "gui.createrailwaysnavigator.route_overview.next_connections";
    private static final String keyScheduleTransfer = "gui.createrailwaysnavigator.route_overview.schedule_transfer";
    private static final String keyConnectionEndangered = "gui.createrailwaysnavigator.route_overview.connection_endangered";
    private static final String keyConnectionMissed = "gui.createrailwaysnavigator.route_overview.connection_missed";
    private static final String keyConnectionMissedPageText = "gui.createrailwaysnavigator.route_overview.journey_interrupted_info";
    private static final String keyDepartureIn = "gui.createrailwaysnavigator.route_details.departure";
    private static final String keyTimeNow = "gui.createrailwaysnavigator.time.now";
    private static final String keyOptionsText = "gui.createrailwaysnavigator.route_overview.options";
    private static final String keyKeybindOptions = "key.createrailwaysnavigator.route_overlay_options";

    private final JourneyListener listener;



    @SuppressWarnings("resource")
    public RouteDetailsOverlayScreen(Level level, int lastRefreshedTime, SimpleRoute route) {
        this.level = level;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);

        this.listener = JourneyListener.listenTo(route)
            .onNarratorAnnounce(this::narratorAnnouncement)
            .onAnnounceNextStop(this::announceNextStop)
            .onContinueWithJourneyAfterStop(this::nextStop)
            .onFinishJourney(this::finishJourney)
            .onReachNextStop(this::reachNextStop)
            .onJourneyBegin(this::journeyBegin)
            .start();
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
        if (listener.getCurrentState() == JourneyListener.State.WHILE_TRAVELING) {
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
        listener.tick();
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

        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * getUIScale()) {
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

    private void journeyBegin(JourneyBeginEvent data) {
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
        if (data.connectionMissed()) {
            setPageJourneyInterrupted();
        } else if (data.isTransfer()) {
            setPageTransfer();
        }
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
            Optional<TaggedStationEntry> station = listener.nextStation();          
            if (station.isPresent()) {
                this.messageLabel = MultiLineLabel.create(shadowlessFont, new TranslatableComponent(keyTransfer,
                    station.get().train().trainName(),
                    station.get().train().scheduleTitle(),
                    station.get().station().getInfo().platform()
                ), SLIDING_TEXT_AREA_WIDTH - (15 + ModGuiIcons.ICON_SIZE));
            }
            fadeIn(null);
        });
    }

    private void setPageJourneyInterrupted() {
        fadeOut(() -> {
            currentPage = Page.CONNECTION_MISSED;
            Optional<TaggedStationEntry> station = listener.previousSation();
            if (station.isPresent()) {
                this.messageLabel = MultiLineLabel.create(shadowlessFont, new TranslatableComponent(keyConnectionMissedPageText, station.get().station().getStationName()), SLIDING_TEXT_AREA_WIDTH - 10);
            }
            fadeIn(null);
        });
    }

    private void setPageJourneyCompleted() {
        fadeOut(() -> {
            currentPage = Page.JOURNEY_END;
            Optional<TaggedStationEntry> station = listener.previousSation();
            if (station.isPresent()) {
                this.messageLabel = MultiLineLabel.create(shadowlessFont, new TranslatableComponent(keyAfterJourney, station.get().station().getStationName()), SLIDING_TEXT_AREA_WIDTH - 10);
            }
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
        NetworkManager.sendToServer(new NextConnectionsRequestPacket(id, listener.currentStation().train().trainId(), listener.currentStation().station().getStationName(), listener.currentStation().station().getTicks()));
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
                    listener.currentStation().train().trainName(),
                    listener.currentStation().train().scheduleTitle()
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
                NetworkManager.sendToServer(new TrainDataRequestPacket(id, listener.currentStation().train().trainId()));
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
        GuiComponent.blit(poseStack, x, y, 0, listener.getCurrentState().important() ? 138 : 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
        
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
                    for (int i = listener.getIndex(); i < Math.min(listener.getIndex() + MAX_STATION_PER_PAGE, listener.getStationCount()); i++) {
                        final int k = i;
                        yOffset[0] += renderRouteOverview(poseStack, k, x, yOffset[0], alpha, fontAlpha);
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

        Optional<TaggedStationEntry> stationOptional = listener.getEntryAt(index);
        Optional<TaggedStationEntry> lastStation = index > listener.getIndex() ? listener.getEntryAt(index - 1) : Optional.empty();
        Optional<TaggedStationEntry> nextStation = listener.getEntryAt(index + 1);

        if (!stationOptional.isPresent()) {
            return y;
        }
        TaggedStationEntry station = stationOptional.get();

        boolean reachable = station.reachable();

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
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.text(TimeUtils.parseTime((int)(station.station().getScheduleTime() + Constants.TIME_SHIFT), TimeFormat.HOURS_24)).withStyle(reachable ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, reachable ? (index <= listener.getIndex() ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha);
        
        if (station.station().getEstimatedTimeWithThreshold() > 0 && reachable && (!lastStation.isPresent() || lastStation.get().station().getEstimatedTime() < station.station().getEstimatedTime()) && (station.train().trainId().equals(station.train().trainId()) || station.station().getEstimatedTime() + ModClientConfig.TRANSFER_TIME.get() + ModClientConfig.EARLY_ARRIVAL_THRESHOLD.get() > station.station().getScheduleTime())) {
            GuiComponent.drawString(poseStack, shadowlessFont, Utils.text(TimeUtils.parseTime((int)(station.station().getEstimatedTimeWithThreshold() + Constants.TIME_SHIFT), TimeFormat.HOURS_24)).withStyle(reachable ? ChatFormatting.RESET : ChatFormatting.STRIKETHROUGH), x + 40, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, timeDiff < ModClientConfig.DEVIATION_THRESHOLD.get() && reachable ? ON_TIME | fontAlpha : DELAYED | fontAlpha);
        }

        // station name display
        Component platformText = Utils.text(station.station().getInfo().platform());
        int platformTextWidth = shadowlessFont.width(platformText);

        final int maxStationNameWidth = SLIDING_TEXT_AREA_WIDTH - platformTextWidth - 105;
        MutableComponent stationText = Utils.text(station.station().getStationName());
        if (index == listener.getIndex()) stationText = stationText.withStyle(ChatFormatting.BOLD);
        if (!reachable) stationText = stationText.withStyle(ChatFormatting.STRIKETHROUGH);
        if (shadowlessFont.width(stationText) > maxStationNameWidth) {
            stationText = Utils.text(shadowlessFont.substrByWidth(stationText, maxStationNameWidth).getString()).append(Utils.text("...")).withStyle(stationText.getStyle());
        }

        GuiComponent.drawString(poseStack, shadowlessFont, stationText, x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, reachable ? (index <= 0 ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha);
        GuiComponent.drawString(poseStack, shadowlessFont, platformText, x + SLIDING_TEXT_AREA_WIDTH - platformTextWidth, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, reachable ? (index <= 0 ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha) : DELAYED | fontAlpha);
        
        // render transfer
        if (station.tag() == StationTag.PART_END) {
            y += ROUTE_LINE_HEIGHT;
            RenderSystem.setShaderTexture(0, GUI);
            GuiComponent.blit(poseStack, x + 75, y, 226, transferY, 7, ROUTE_LINE_HEIGHT, 256, 256);
            if (nextStation.isPresent() && nextStation.get().willMissStop()) {
                if (nextStation.get().isDeparted()) {
                    ModGuiIcons.CROSS.render(poseStack, x + 10, y + ROUTE_LINE_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
                    GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyConnectionMissed).withStyle(ChatFormatting.BOLD), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, DELAYED | fontAlpha);
                } else {
                    ModGuiIcons.WARN.render(poseStack, x + 10, y + ROUTE_LINE_HEIGHT - 2 - ModGuiIcons.ICON_SIZE / 2);
                    GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyConnectionEndangered).withStyle(ChatFormatting.BOLD), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, COLOR_WARN | fontAlpha);
                }
            } else {
                GuiComponent.drawString(poseStack, shadowlessFont, Utils.text(TimeUtils.parseDurationShort((int)(listener.getTransferTime(index)))).withStyle(ChatFormatting.ITALIC), x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, 0xDBDBDB | fontAlpha);
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
        y += 3 + renderRouteOverview(poseStack, listener.getIndex(), x, y - 3, alphaPercentage, fontAlpha);
        GuiComponent.fill(poseStack, x + 3, y, x + 3 + SLIDING_TEXT_AREA_WIDTH, y + 1, 0xDBDBDB | fontAlpha);
        
        // Title
        ModGuiIcons.TIME.render(poseStack, x + 10, y + 3);
        long time = listener.currentStation().station().getEstimatedTimeWithThreshold() - level.getDayTime();
        GuiComponent.drawString(poseStack, shadowlessFont, Utils.translate(keyDepartureIn).append(" ").append(time > 0 ? Utils.text(TimeUtils.parseTime((int)(time % 24000), TimeFormat.HOURS_24)) : Utils.translate(keyTimeNow)).withStyle(ChatFormatting.BOLD), x + 15 + ModGuiIcons.ICON_SIZE, y + 3 + ModGuiIcons.ICON_SIZE / 2 - shadowlessFont.lineHeight / 2, 0xFFFFFF | fontAlpha);
        y += 5 + ModGuiIcons.ICON_SIZE;
        
        // Details
        final int detailsLineHeight = 12;
        //StationEntry station = taggedRoute[0].station();
        StationEntry endStation = listener.lastStation().station();

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
            listener.getTransferCount(),
            Utils.translate(keyTransferCount).getString(),
            TimeUtils.parseDurationShort(listener.getTotalDuration())
        )), x + 15 + ModGuiIcons.ICON_SIZE, y + detailsLineHeight, 0xDBDBDB | fontAlpha);
    }

    public void renderPageTransfer(PoseStack poseStack, int x, int y, float alphaPercentage, int fontAlpha, StationEntry station) {
        y += 3 + renderRouteOverview(poseStack, listener.getIndex(), x, y - 3, alphaPercentage, fontAlpha);
        GuiComponent.fill(poseStack, x + 3, y, x + 3 + SLIDING_TEXT_AREA_WIDTH, y + 1, 0xDBDBDB | fontAlpha);
        
        // Title
        ModGuiIcons.WALK.render(poseStack, x + 10, y + 3);
        
        //Optional<TaggedStationEntry> nextStation = listener.nextStation();
        long transferTime = listener.getTransferTime(listener.getIndex());
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
}
