package de.mrjulsen.crn.client.gui.overlay;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.UIRenderHelper;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.data.DeparturePrediction;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleTrainConnection;
import de.mrjulsen.crn.data.SimpleTrainSchedule;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.data.SimpleRoute.StationEntry;
import de.mrjulsen.crn.data.SimpleRoute.StationTag;
import de.mrjulsen.crn.data.SimpleRoute.TaggedStationEntry;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.NextConnectionsRequestPacket;
import de.mrjulsen.crn.network.packets.cts.RealtimeRequestPacket;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket;
import de.mrjulsen.crn.util.GuiUtils;
import de.mrjulsen.crn.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
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
    private static final float SCALE = 0.75f;

    private static final int ON_TIME = 0x1AEA5F;
    private static final int DELAYED = 0xFF4242;
    private static final float FADE_TIME = 5;

    private static final int INFO_BEFORE_NEXT_STOP = 500;
    private static final int DELAY_TRESHOLD = 500;

    private long fadeStart = 0L;
    private boolean fading = false;
    private boolean fadeInvert = true;
    private Runnable fadeDoneAction;
    
    private static final int ROUTE_LINE_HEIGHT = 14;

    private final int x, y;
    private final Level level;
    
    private static final int MAX_STATION_PER_PAGE = 4;
    private final TaggedStationEntry[] taggedRoute;
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

    
    private static final String keyJourneyBegins = "gui.createrailwaysnavigator.route_overview.journey_begins";
    private static final String keyTrainDetails = "gui.createrailwaysnavigator.route_overview.train_details";
    private static final String keyTrainSpeed = "gui.createrailwaysnavigator.route_overview.train_speed";
    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyAfterJourney = "gui.createrailwaysnavigator.route_overview.after_journey";

    @SuppressWarnings("resource")
    public RouteDetailsOverlayScreen(int x, int y, Level level, int lastRefreshedTime, SimpleRoute route) {
        this.x = x;
        this.y = y;
        this.level = level;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);
        this.taggedRoute = route.getRoutePartsTagged();

        //setSlidingText(new TextComponent("Nächste Station: Salzingen HBF. Ausstieg: Rechts"));
        setSlidingText(concat(
            new TranslatableComponent(keyJourneyBegins,
                route.getParts().stream().findFirst().get().getTrainName(),
                route.getParts().stream().findFirst().get().getScheduleTitle(),
                Utils.parseTime((int)(route.getStartStation().getTicks() % Constants.TICKS_PER_DAY)),
                3
            ),
            new TranslatableComponent(keyNextStop,
                route.getStartStation().getStationName(),
                Utils.parseTime((int)(route.getStartStation().getTicks() % Constants.TICKS_PER_DAY))
            )
        ));
        //setSlidingText(new TextComponent("Information zu RE1 nach Reudnitz Hbf über Neu-Donauwörth, Abfahrt 01:35 Uhr, heute circa 20 Minuten später. Grund dafür ist ein Drache auf der Strecke."));
        /*setPageNextConnections(List.of(
            new SimpleTrainConnection("RE 1", UUID.randomUUID(), null, 3287, "Neu-Donauwörth"),
            new SimpleTrainConnection("RB 30", UUID.randomUUID(), null, 123, "Kristallsee"),
            new SimpleTrainConnection("IC 1234", UUID.randomUUID(), null, 2345, "Reudnitz HBF"),
            new SimpleTrainConnection("RE1", UUID.randomUUID(), null, 3287, "Salzingen HBF"),
            new SimpleTrainConnection("RB15", UUID.randomUUID(), null, 123, "Ölingen"),
            new SimpleTrainConnection("RE1", UUID.randomUUID(), null, 3287, "Salzingen HBF"),
            new SimpleTrainConnection("RE9", UUID.randomUUID(), null, 123, "Pattyhausen"),
            new SimpleTrainConnection("RB7", UUID.randomUUID(), null, 3287, "Oberbrückheim")
        ));
        */
        setPageRouteOverview();
        
        setSlidingText(new TranslatableComponent(keyJourneyBegins,
            currentStation().train().trainName(),
            currentStation().train().scheduleTitle(),
            Utils.parseTime(currentStation().station().getEstimatedTimeWithTreshold()),
            3 // TODO: platform
        ));
    }

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public void tick() {
        
        if (currentState == State.WHILE_TRAVELING) {
            trainDataSubPageTime++;
            if ((slidingTextWidth <= SLIDING_TEXT_AREA_WIDTH * SCALE - 20 && trainDataSubPageTime > TIME_PER_TRAIN_DATA_SUBPAGE) || slidingTextOffset < -(slidingTextWidth / 2)) {
                setTrainDataSubPage(false);
            }
        }

        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * SCALE - 20) {
            slidingTextOffset--;
            if (slidingTextOffset < -(slidingTextWidth / 2)) {
                slidingTextOffset = (int)((SLIDING_TEXT_AREA_WIDTH * SCALE + slidingTextWidth / 2) + 20);                
            }
        }

        switch (currentPage) {
            case NEXT_CONNECTIONS:
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

        if (currentState != State.AFTER_JOURNEY) {
            realTimeRefreshTimer++;
            if (realTimeRefreshTimer > REALTIME_REFRESH_TIME) {
                realTimeRefreshTimer = 0;
                requestRealtimeData();
            }
        }
    }

    private void nextStop() {
        if (stationIndex + 1 >= taggedRoute.length) {
            finishJourney();
            return;
        }

        stationIndex++;
        setTrainDataSubPage(true);
        setPageRouteOverview();
        currentState = State.WHILE_TRAVELING;
    }

    private void finishJourney() {
        setSlidingText(new TranslatableComponent(keyAfterJourney,
            taggedRoute[taggedRoute.length - 1].station().getStationName()
        ));
        currentState = State.AFTER_JOURNEY;
    }

    private void announceNextStop() {
        setSlidingText(new TranslatableComponent(keyNextStop,
            currentStation().station().getStationName(),
            Utils.parseTime( currentStation().station().getEstimatedTimeWithTreshold())
        ));
        currentState = State.BEFORE_NEXT_STOP;

        //NarratorChatListener.INSTANCE.narrator.say(String.format("Nächster Halt: %s", currentStation().station().getStationName()), true);

        setPageNextConnections();
    }

    private void reachNextStop() {
        setSlidingText(new TextComponent(currentStation().station().getStationName()));
        //NarratorChatListener.INSTANCE.narrator.say(slidingText.getString(), true);
        currentState = State.WHILE_NEXT_STOP;
    }

    private TaggedStationEntry currentStation() {
        return taggedRoute[stationIndex];
    }

    // effects
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

    // Switch pages
    private void setPageRouteOverview() {
        fadeOut(() -> {
            currentPage = Page.ROUTE_OVERVIEW;
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

    private void requestRealtimeData() {
        final Collection<UUID> ids = Arrays.stream(taggedRoute).map(x -> x.train().trainId()).distinct().toList();
        long id = InstanceManager.registerClientRealtimeResponseAction((predictions, time) -> {
            Map<UUID, List<SimpleDeparturePrediction>> predMap = predictions.stream().collect(Collectors.groupingBy(SimpleDeparturePrediction::id));

            for (int i = stationIndex; i < taggedRoute.length; i++) {
                TaggedStationEntry e = taggedRoute[i];
                List<SimpleDeparturePrediction> preds = predMap.get(e.train().trainId());
                int newTime = preds.stream().filter(x -> x.station().equals(e.station().getStationName())).mapToInt(x -> x.ticks()).findFirst().orElse(0);

                if (i == stationIndex || time + newTime > taggedRoute[stationIndex].station().getEstimatedTime()) {
                    e.station().updateRealtimeData(newTime, time);
                };
            }
            
            if (currentState != State.BEFORE_JOURNEY) {
                if (currentState != State.BEFORE_NEXT_STOP && currentState != State.WHILE_NEXT_STOP && time >= taggedRoute[stationIndex].station().getEstimatedTime() - INFO_BEFORE_NEXT_STOP) {                    
                    announceNextStop();
                }
                    
                if (currentState != State.WHILE_TRAVELING && !predMap.get(currentStation().train().trainId()).get(0).station().equals(currentStation().station().getStationName())) {                    
                    if (currentStation().tag() == StationTag.END) {
                        finishJourney();
                    } else {
                        nextStop();
                    }
                }
            }

            if (currentState != State.WHILE_NEXT_STOP && time >= taggedRoute[stationIndex].station().getEstimatedTime()) {                    
                reachNextStop();
            }            
        });
        NetworkManager.sendToServer(new RealtimeRequestPacket(id, ids));
    }

    @Override
    public void render(ForgeIngameGui gui, PoseStack poseStack, int width, int height, float partialTicks) {
        float fadePercentage = this.fading ? Mth.clamp((float)(Util.getMillis() - this.fadeStart) / 500.0F, 0.0F, 1.0F) : 1.0F;
        float alpha = fadeInvert ? Mth.clamp(1.0f - fadePercentage, 0, 1) : Mth.clamp(fadePercentage, 0, 1);
        int fontAlpha = Mth.ceil(alpha * 255.0F) << 24; // <color> | fontAlpha

        poseStack.scale(SCALE, SCALE, SCALE);
        RenderSystem.setShaderTexture(0, GUI);
        GuiComponent.blit(poseStack, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
        
        GuiComponent.drawString(poseStack, shadowlessFont, title, x + 6, y + 4, 0x4F4F4F);
        
        String timeString = Utils.parseTime((int)(level.getDayTime() % Constants.TICKS_PER_DAY));
        GuiComponent.drawString(poseStack, shadowlessFont, timeString, x + GUI_WIDTH - 4 - shadowlessFont.width(timeString), y + 4, 0x4F4F4F);
        
        // Test
        renderSlidingText(poseStack);

        startStencil(poseStack, x + 3, y + 40, 220, 62);
        poseStack.pushPose();
        poseStack.translate((fadeInvert ? 0 : -20) + fadePercentage * 20, 0, 0);
        if (alpha > 0.1f && (fontAlpha & -67108864) != 0) {
            
            switch (currentPage) {
                case NEXT_CONNECTIONS:
                    renderConnections(poseStack, y + 40, fadePercentage, fontAlpha, null);
                    break;
                case ROUTE_OVERVIEW:
                default:
                    final int[] yOffset = new int[] { y + 40 - 1 };
                    for (int i = stationIndex; i < Math.min(stationIndex + MAX_STATION_PER_PAGE, taggedRoute.length); i++) {
                        final int k = i;
                        yOffset[0] += renderRouteOverview(poseStack, k, yOffset[0], alpha, fontAlpha, taggedRoute[i]);
                    }                    
                    break;
            }
        }
        poseStack.popPose();
        endStencil();

        poseStack.scale(1f / SCALE, 1f / SCALE, 1f / SCALE);

        if (fadePercentage >= 1.0f) {
            fading = false;
            if (fadeDoneAction != null) {
                fadeDoneAction.run();
            }
        }
    }

    private void startStencil(PoseStack poseStack, int x, int y, int w, int h) {
        UIRenderHelper.swapAndBlitColor(Minecraft.getInstance().getMainRenderTarget(), UIRenderHelper.framebuffer);
        GuiUtils.startStencil(poseStack, x, y, w, h);
    }

    private void endStencil() {
        GuiUtils.endStencil();
        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, Minecraft.getInstance().getMainRenderTarget());
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

    private void setSlidingText(Component component) {
        slidingText = component;
        slidingTextWidth = shadowlessFont.width(component);

        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * SCALE - 20) {
            slidingTextOffset = (int)((SLIDING_TEXT_AREA_WIDTH * SCALE + slidingTextWidth / 2) + 20);
        } else {
            slidingTextOffset = (int)(SLIDING_TEXT_AREA_WIDTH * SCALE / 2);
        }
    }

    public void renderSlidingText(PoseStack poseStack) {
        startStencil(poseStack, x + 3, y + 16, 220, 21);
        poseStack.pushPose();
        poseStack.scale(1f / SCALE, 1f / SCALE, 1f / SCALE);
        GuiComponent.drawCenteredString(poseStack, shadowlessFont, slidingText, (int)((x + 3) * SCALE + slidingTextOffset), y + 14, 0xFF9900);
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.popPose();
        endStencil();
    }

    public int renderRouteOverview(PoseStack poseStack, int index, int y, float alphaPercentage, int fontAlpha, TaggedStationEntry station) {
        RenderSystem.setShaderTexture(0, GUI);
        RenderSystem.setShaderColor(1, 1, 1, alphaPercentage);        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

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

        long timeDiff = station.station().getCurrentRefreshTime() + station.station().getCurrentTicks() - station.station().getRefreshTime() - station.station().getTicks();

        // text
        GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(Utils.parseTime((int)(station.station().getRefreshTime() + station.station().getTicks()))), x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, index <= 0 ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha);
        GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(Utils.parseTime((int)(station.station().getEstimatedTimeWithTreshold()))), x + 40, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, timeDiff < DELAY_TRESHOLD ? ON_TIME | fontAlpha : DELAYED | fontAlpha);
        //GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(String.valueOf((int)(station.station().getRefreshTime() + station.station().getTicks()))), x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, index <= 0 ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha);
        //GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(String.valueOf((int)(station.station().getEstimatedTimeWithTreshold()))), x + 40, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, timeDiff < DELAY_TRESHOLD ? ON_TIME | fontAlpha : DELAYED | fontAlpha);
        
        GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(station.station().getStationName()).withStyle(index == 0 ? ChatFormatting.BOLD : ChatFormatting.RESET), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, index <= 0 ? 0xFFFFFF | fontAlpha : 0xDBDBDB | fontAlpha);
        
        // render transfer
        if (station.tag() == StationTag.PART_END) {
            y += ROUTE_LINE_HEIGHT;
            int transferTime = -1;
            if (stationIndex + index + 1 < taggedRoute.length) {
                transferTime = taggedRoute[stationIndex + index + 1].station().getTicks() - taggedRoute[stationIndex + index].station().getTicks();
            }
            RenderSystem.setShaderTexture(0, GUI);
            GuiComponent.blit(poseStack, x + 75, y, 226, transferY, 7, ROUTE_LINE_HEIGHT, 256, 256);
            GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(Utils.parseDurationShort(transferTime)).withStyle(ChatFormatting.ITALIC), x + 10, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, 0xDBDBDB | fontAlpha);
            GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent("Umstieg").withStyle(ChatFormatting.ITALIC), x + 90, y + ROUTE_LINE_HEIGHT - 2 - shadowlessFont.lineHeight / 2, 0xDBDBDB | fontAlpha);
                    
            return ROUTE_LINE_HEIGHT * 2;
        }

        return ROUTE_LINE_HEIGHT;
        
    }

    public void renderConnections(PoseStack poseStack, int y, float alphaPercentage, int fontAlpha, StationEntry station) {
        GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent("Nächste Anschlüsse:").withStyle(ChatFormatting.BOLD), x + 10, y + 4, 0xFFFFFF | fontAlpha);

        SimpleTrainConnection[] conns = connections.toArray(SimpleTrainConnection[]::new);
        for (int i = connectionsSubPageIndex * CONNECTION_ENTRIES_PER_PAGE, k = 0; i < Math.min((connectionsSubPageIndex + 1) * CONNECTION_ENTRIES_PER_PAGE, connections.size()); i++, k++) {
            GuiComponent.drawString(poseStack, shadowlessFont, new TextComponent(Utils.parseTime((int)(conns[i].ticks() % Constants.TICKS_PER_DAY))), x + 10, y + 15 + 12 * k, 0xDBDBDB | fontAlpha);
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

    protected static enum Page {
        ROUTE_OVERVIEW,
        NEXT_CONNECTIONS;
    }

    protected static enum State {
        BEFORE_JOURNEY,
        WHILE_TRAVELING,
        BEFORE_NEXT_STOP,
        WHILE_NEXT_STOP,
        BEFORE_TRANSFER,
        WHILE_TRANSFER,
        AFTER_JOURNEY;
    }
}
