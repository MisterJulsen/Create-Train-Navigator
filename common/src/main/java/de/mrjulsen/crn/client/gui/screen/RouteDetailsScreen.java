package de.mrjulsen.crn.client.gui.screen;

import java.util.UUID;

import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.overlay.RouteDetailsOverlayScreen;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.ExpandButton;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleRoute.SimpleRoutePart;
import de.mrjulsen.crn.data.SimpleRoute.StationEntry;
import de.mrjulsen.crn.data.SimpleRoute.StationTag;
import de.mrjulsen.crn.event.listeners.IJourneyListenerClient;
import de.mrjulsen.crn.event.listeners.JourneyListener;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.OverlayManager;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.WidgetsCollection;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils.TimeFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class RouteDetailsScreen extends DLScreen implements IJourneyListenerClient {

    private static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/route_details.png");  
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private static final int ENTRIES_START_Y_OFFSET = 18;
    private static final int ENTRY_WIDTH = 220;
    private static final int ENTRY_TIME_X = 28;
    private static final int ENTRY_DEST_X = 66;

    
    private final int AREA_X = 16;
    private final int AREA_Y = 53;
    private final int AREA_W = 220;
    private final int AREA_H = 157;

    private int guiLeft, guiTop;    
    private int scrollMax = 0;

    // Controls
    private DLCreateIconButton backButton;    
    private DLCreateIconButton saveButton;    
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);
    private final ExpandButton[] expandButtons;
    private final WidgetsCollection expandButtonCollection = new WidgetsCollection();

    // Data
    private final SimpleRoute route;
    private final Screen lastScreen;
    private final Font font;
    private final Font shadowlessFont;
    private final Level level;

    // Tooltips
    private final MutableComponent textDeparture = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.departure");
    private final MutableComponent textTransferIn = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.next_transfer_time");
    private final MutableComponent transferText = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.transfer");
    private final MutableComponent textJourneyCompleted = ELanguage.translate("gui.createrailwaysnavigator.route_overview.journey_completed");
    private final MutableComponent timeNowText = ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".time.now");    
    private final MutableComponent textConnectionEndangered = ELanguage.translate("gui.createrailwaysnavigator.route_overview.connection_endangered").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD);
    private final MutableComponent textConnectionMissed = ELanguage.translate("gui.createrailwaysnavigator.route_overview.connection_missed").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
    private final MutableComponent textTrainCanceled = ELanguage.translate("gui.createrailwaysnavigator.route_overview.train_canceled").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
    private final MutableComponent textSaveRoute = TextUtils.translate("gui.createrailwaysnavigator.route_details.save_route");
    private final String keyTrainCancellationReason = "gui.createrailwaysnavigator.route_overview.train_cancellation_info";

    private final UUID clientId = UUID.randomUUID();

    @SuppressWarnings("resource")
    public RouteDetailsScreen(Screen lastScreen, Level level, SimpleRoute route, UUID listenerId) {
        super(TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".route_details.title"));
        this.lastScreen = lastScreen;
        this.route = route;
        this.font = Minecraft.getInstance().font;  
        this.shadowlessFont = new NoShadowFontWrapper(font);    
        this.level = level;
        JourneyListenerManager.getInstance().get(listenerId, this);

        int count = route.getParts().size();
        expandButtons = new ExpandButton[count];
        for (int i = 0; i < count; i++) {
            expandButtons[i] = new ExpandButton(0, 0, false, (btn) -> {});
            expandButtonCollection.components.add(expandButtons[i]);
        }
    }

    @Override
    public UUID getJourneyListenerClientId() {
        return clientId;
    }

    public int getCurrentTime() {
        return (int)(level.getDayTime() % DragonLib.TICKS_PER_DAY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);        
        JourneyListenerManager.getInstance().removeClientListenerForAll(this);
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;

        final int fWidth = width;
        final int fHeight = height;

        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIG_BACK) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                onClose();
            }
        });
        addTooltip(DLTooltip.of(Constants.TOOLTIP_GO_BACK).assignedTo(backButton));

        saveButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 21 + DEFAULT_ICON_BUTTON_WIDTH + 4, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.BOOKMARK.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                if (JourneyListenerManager.getInstance().exists(route.getListenerId())) {
                    InstanceManager.setRouteOverlay(OverlayManager.add(new RouteDetailsOverlayScreen(level, route, fWidth, fHeight)));
                }
            }
        });
        addTooltip(DLTooltip.of(textSaveRoute).assignedTo(saveButton));
    }

    @Override
    public void tick() {
        super.tick();
		scroll.tickChaser();

        saveButton.visible = JourneyListenerManager.getInstance().exists(route.getListenerId());
    }

    private Pair<MutableComponent, MutableComponent> getStationInfo(StationEntry station) {
        final boolean reachable = station.reachable(false);
        MutableComponent timeText = TextUtils.text(TimeUtils.parseTime((int)((route.getRefreshTime() + DragonLib.DAYTIME_SHIFT) % 24000 + station.getTicks()), ModClientConfig.TIME_FORMAT.get()));
        MutableComponent stationText = TextUtils.text(station.getStationName());
        if (!reachable) {
            timeText = timeText.withStyle(ChatFormatting.RED).withStyle(ChatFormatting.STRIKETHROUGH);
            stationText = stationText.withStyle(ChatFormatting.RED).withStyle(ChatFormatting.STRIKETHROUGH);
        }

        return Pair.of(timeText, stationText);
    } 

    private int renderRouteStart(Graphics graphics, int x, int y, StationEntry stop) {
        final int HEIGHT = 30;
        final int V = 48;

        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, x, y, 0, V, ENTRY_WIDTH, HEIGHT);

        Pair<MutableComponent, MutableComponent> text = getStationInfo(stop);
        float scale = shadowlessFont.width(text.getFirst()) > 30 ? 0.75f : 1;
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, 1, 1);
        int pY = y + 15;
        if (stop.shouldRenderRealtime() && stop.reachable(false)) {
            pY -= (stop.shouldRenderRealtime() ? 5 : 0);
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + ENTRY_TIME_X) / scale), pY + 10, TextUtils.text(TimeUtils.parseTime((int)(stop.getEstimatedTimeWithThreshold() % 24000 + DragonLib.DAYTIME_SHIFT), ModClientConfig.TIME_FORMAT.get())), stop.getDifferenceTime() > ModClientConfig.DEVIATION_THRESHOLD.get() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        }
        GuiUtils.drawString(graphics, shadowlessFont, (int)((x + ENTRY_TIME_X) / scale), pY, text.getFirst(), 0xFFFFFF, EAlignment.LEFT, false);
        graphics.poseStack().popPose();

        Component platformText = TextUtils.text(stop.getUpdatedInfo().platform());
        GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_DEST_X + 129 - shadowlessFont.width(platformText), y + 15, platformText, stop.stationInfoChanged() ? Constants.COLOR_DELAYED : 0xFFFFFF, EAlignment.LEFT, false);

        MutableComponent name = text.getSecond();
        int maxTextWidth = 135 - 12 - shadowlessFont.width(platformText);  
        if (shadowlessFont.width(name) > maxTextWidth) {
            name = TextUtils.text(shadowlessFont.substrByWidth(name, maxTextWidth - 3).getString()).append(Constants.ELLIPSIS_STRING);
        }
        GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_DEST_X, y + 15, name, 0xFFFFFF, EAlignment.LEFT, false);

        return HEIGHT;
    }

    private int renderTrainDetails(Graphics graphics, int x, int y, SimpleRoutePart part) {
        final int HEIGHT = 43;
        final int V = 99;
        final float scale = 0.75f;
        final float mul = 1 / scale;

        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, x, y, 0, V, ENTRY_WIDTH, HEIGHT);
        part.getTrainIcon().render(TrainIconType.ENGINE, graphics.graphics(), x + ENTRY_DEST_X, y + 7);

        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, scale);
        GuiUtils.drawString(graphics, shadowlessFont, (int)((x + ENTRY_DEST_X + 24) / scale), (int)((y + 7) / scale), TextUtils.text(String.format("%s (%s)", part.getTrainName(), part.getTrainID().toString().split("-")[0])), 0xDBDBDB, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, shadowlessFont, (int)((x + ENTRY_DEST_X + 24) / scale), (int)((y + 17) / scale), TextUtils.text(String.format("→ %s", part.getScheduleTitle())), 0xDBDBDB, EAlignment.LEFT, false);
        graphics.poseStack().scale(mul, mul, mul);
        graphics.poseStack().popPose();

        return HEIGHT;
    }

    private int renderStop(Graphics graphics, int x, int y, StationEntry stop) {
        final int HEIGHT = 21;
        final int V = 78;

        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, x, y, 0, V, ENTRY_WIDTH, HEIGHT);

        Pair<MutableComponent, MutableComponent> text = getStationInfo(stop);
        float scale = shadowlessFont.width(text.getFirst()) > 30 ? 0.75f : 1;
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, 1, 1);
        int pY = y + 6;
        if (stop.shouldRenderRealtime() && stop.reachable(false)) {
            pY -= (stop.shouldRenderRealtime() ? 5 : 0);
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + ENTRY_TIME_X) / scale), pY + 10, TextUtils.text(TimeUtils.parseTime((int)(stop.getEstimatedTimeWithThreshold() % 24000 + DragonLib.DAYTIME_SHIFT), ModClientConfig.TIME_FORMAT.get())), stop.getDifferenceTime() > ModClientConfig.DEVIATION_THRESHOLD.get() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        }
        GuiUtils.drawString(graphics, shadowlessFont, (int)((x + ENTRY_TIME_X) / scale), pY, text.getFirst(), 0xFFFFFF, EAlignment.LEFT, false);
        graphics.poseStack().popPose();

        Component platformText = TextUtils.text(stop.getUpdatedInfo().platform());
        GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_DEST_X + 129 - shadowlessFont.width(platformText), y + 6, platformText, stop.stationInfoChanged() ? Constants.COLOR_DELAYED : 0xFFFFFF, EAlignment.LEFT, false);

        MutableComponent name = text.getSecond();
        int maxTextWidth = 135 - 12 - shadowlessFont.width(platformText);  
        if (shadowlessFont.width(name) > maxTextWidth) {
            name = TextUtils.text(shadowlessFont.substrByWidth(name, maxTextWidth - 3).getString()).append(Constants.ELLIPSIS_STRING);
        }
        GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_DEST_X, y + 6, name, 0xFFFFFF, EAlignment.LEFT, false);

        return HEIGHT;
    }

    private int renderRouteEnd(Graphics graphics, int x, int y, StationEntry stop) {
        final int HEIGHT = 44;
        final int V = 142;

        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, x, y, 0, V, ENTRY_WIDTH, HEIGHT);
        
        Pair<MutableComponent, MutableComponent> text = getStationInfo(stop);
        float scale = shadowlessFont.width(text.getFirst()) > 30 ? 0.75f : 1;
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, 1, 1);
        int pY = y + 21;
        if (stop.shouldRenderRealtime() && stop.reachable(false)) {
            pY -= (stop.shouldRenderRealtime() ? 5 : 0);
            GuiUtils.drawString(graphics, shadowlessFont, (int)((x + ENTRY_TIME_X) / scale), pY + 10, TextUtils.text(TimeUtils.parseTime((int)(stop.getEstimatedTimeWithThreshold() % 24000 + DragonLib.DAYTIME_SHIFT), ModClientConfig.TIME_FORMAT.get())), stop.getDifferenceTime() > ModClientConfig.DEVIATION_THRESHOLD.get() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME, EAlignment.LEFT, false);
        }
        
        GuiUtils.drawString(graphics, shadowlessFont, (int)((x + ENTRY_TIME_X) / scale), pY, text.getFirst(), 0xFFFFFF, EAlignment.LEFT, false);
        graphics.poseStack().popPose();

        Component platformText = TextUtils.text(stop.getUpdatedInfo().platform());
        GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_DEST_X + 129 - shadowlessFont.width(platformText), y + 21, platformText, stop.stationInfoChanged() ? Constants.COLOR_DELAYED : 0xFFFFFF, EAlignment.LEFT, false);
        
        MutableComponent name = text.getSecond();
        int maxTextWidth = 135 - 12 - shadowlessFont.width(platformText);  
        if (shadowlessFont.width(name) > maxTextWidth) {
            name = TextUtils.text(shadowlessFont.substrByWidth(name, maxTextWidth - 3).getString()).append(Constants.ELLIPSIS_STRING);
        }
        GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_DEST_X, y + 21, name, 0xFFFFFF, EAlignment.LEFT, false);

        return HEIGHT;
    }

    private void renderHeadline(Graphics graphics, int pMouseX, int pMouseY) {
        Component titleInfo = TextUtils.empty();
        Component headline = TextUtils.empty();
        JourneyListener listener = JourneyListenerManager.getInstance().get(route.getListenerId(), null);

        // Title
        if (!route.isValid()) {
            titleInfo = TextUtils.translate(keyTrainCancellationReason, route.getInvalidationTrainName()).withStyle(ChatFormatting.RED);
            headline = textTrainCanceled;
        } else if (listener != null && listener.getIndex() > 0) {
            titleInfo = textTransferIn;
            long arrivalTime = listener.currentStation().getParent().getEndStation().getEstimatedTimeWithThreshold();
            int time = (int)(arrivalTime % 24000 - getCurrentTime());
            headline = time < 0 || listener.currentStation().getTag() == StationTag.PART_START ? timeNowText : TextUtils.text(TimeUtils.parseTime(time, TimeFormat.HOURS_24));
        } else if (listener == null) {
            titleInfo = TextUtils.empty();
            headline = textJourneyCompleted.withStyle(ChatFormatting.GREEN);            
        } else {            
            titleInfo = textDeparture;
            int departureTicks = route.getStartStation().getTicks();
            int departureTime = (int)(route.getRefreshTime() % 24000 + departureTicks);
            headline = departureTime - getCurrentTime() < 0 ? timeNowText : TextUtils.text(TimeUtils.parseTime(departureTime - getCurrentTime(), TimeFormat.HOURS_24));
        }

        GuiUtils.drawString(graphics, font, guiLeft + GUI_WIDTH / 2, guiTop + 19, titleInfo, 0xFFFFFF, EAlignment.CENTER, false);
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(2, 2, 2);
        GuiUtils.drawString(graphics, font, (guiLeft + GUI_WIDTH / 2) / 2, (guiTop + 31) / 2, headline, 0xFFFFFF, EAlignment.CENTER, false);
        graphics.poseStack().popPose();
    }

    private int renderTransfer(Graphics graphics, int x, int y, long a, long b, StationEntry nextStation) {
        final int HEIGHT = 24;
        final int V = 186;

        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, x, y, 0, V, ENTRY_WIDTH, HEIGHT);

        long time = -1;
        if (a < 0 || b < 0) {
            time = -1;
        } else {
            time = b - a;
        }

        if (nextStation != null && !nextStation.reachable(true)) {
            if (nextStation.isTrainCanceled()) {                
                ModGuiIcons.CROSS.render(graphics, x + ENTRY_TIME_X, y + 13 - ModGuiIcons.ICON_SIZE / 2);
                GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_TIME_X + ModGuiIcons.ICON_SIZE + 4, y + 8, textTrainCanceled, 0xFFFFFF, EAlignment.LEFT, false);
            } else if (nextStation.isDeparted()) {                
                ModGuiIcons.CROSS.render(graphics, x + ENTRY_TIME_X, y + 13 - ModGuiIcons.ICON_SIZE / 2);
                GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_TIME_X + ModGuiIcons.ICON_SIZE + 4, y + 8, textConnectionMissed, 0xFFFFFF, EAlignment.LEFT, false);
            } else {
                ModGuiIcons.WARN.render(graphics, x + ENTRY_TIME_X, y + 13 - ModGuiIcons.ICON_SIZE / 2);
                GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_TIME_X + ModGuiIcons.ICON_SIZE + 4, y + 8, textConnectionEndangered, 0xFFFFFF, EAlignment.LEFT, false);
            }
        } else {
            GuiUtils.drawString(graphics, shadowlessFont, x + ENTRY_TIME_X, y + 8, TextUtils.text(transferText.getString() + " " + (time < 0 ? "" : "(" + TimeUtils.parseDuration(time) + ")")), 0xFFFFFF, EAlignment.LEFT, false);
        }

        return HEIGHT;
    }

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) { 
        pPartialTick = Minecraft.getInstance().getFrameTime();
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderScreenBackground(graphics);
        GuiUtils.drawTexture(GUI, graphics, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        
        /*
        for (Widget widget : this.renderables)
            widget.render(graphics, pMouseX, pMouseY, pPartialTick);
            */

        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);
        
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + 19, guiTop + 4, title, 0x4F4F4F, EAlignment.LEFT, false);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, TextUtils.text(timeString), 0x4F4F4F, EAlignment.LEFT, false);
        
        renderHeadline(graphics, pMouseX, pMouseY);

        GuiUtils.enableScissor(graphics, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
        graphics.poseStack().translate(0, scrollOffset, 0);

        int yOffs = guiTop + 45;
        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, guiLeft + 16, yOffs, 22, ENTRIES_START_Y_OFFSET, 0, 48, 22, 1, 256, 256);
        yOffs +=  + ENTRIES_START_Y_OFFSET;
        SimpleRoutePart[] partsArray = route.getParts().toArray(SimpleRoutePart[]::new);
        for (int i = 0; i < partsArray.length; i++) {
            SimpleRoutePart part = partsArray[i];

            yOffs += renderRouteStart(graphics, guiLeft + 16, yOffs, part.getStartStation());
            yOffs += renderTrainDetails(graphics, guiLeft + 16, yOffs, part);

            ExpandButton btn = expandButtons[i];
            btn.active = part.getStopovers().size() > 0;

            if (btn.active) {
                btn.setX(guiLeft + 78);
                btn.setY(yOffs - 14);

                btn.render(graphics.graphics(), pMouseX, (int)(pMouseY - scrollOffset), pPartialTick);
            }

            if (btn.isExpanded()) {
                for (StationEntry stop : part.getStopovers()) {
                    yOffs += renderStop(graphics, guiLeft + 16, yOffs, stop);
                }
            }

            yOffs += renderRouteEnd(graphics, guiLeft + 16, yOffs, part.getEndStation());

            if (i < partsArray.length - 1) {
                StationEntry currentStation = part.getEndStation();
                StationEntry nextStation = partsArray[i + 1].getStartStation();
                long a = currentStation.shouldRenderRealtime() ? currentStation.getCurrentTime() : currentStation.getScheduleTime();
                long b = nextStation.shouldRenderRealtime() ? nextStation.getCurrentTime() : nextStation.getScheduleTime();
                yOffs += renderTransfer(graphics, guiLeft + 16, yOffs, a, b, nextStation);
            }
        }
        scrollMax = yOffs - guiTop - 45;        
        GuiUtils.drawTexture(Constants.GUI_WIDGETS, graphics, guiLeft + 16, yOffs, 22, AREA_H, 0, 48, 22, 1, 256, 256);
        
        GuiUtils.disableScissor(graphics);
        GuiUtils.fillGradient(graphics, guiLeft + AREA_X, guiTop + AREA_Y, 0, AREA_W, 10, 0x77000000, 0x00000000);
        GuiUtils.fillGradient(graphics, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, 0, AREA_W, 10, 0x00000000, 0x77000000);

        // Scrollbar
        double maxHeight = scrollMax;
        double aH = AREA_H + 1;
        if (aH / maxHeight < 1) {
            int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
            int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

            GuiUtils.fill(graphics, guiLeft + AREA_X + AREA_W - 3, startY, 3, scrollerHeight, 0x7FFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        
		float chaseTarget = scroll.getChaseTarget();
		float max = -AREA_H;
        max += scrollMax;

		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = MathUtils.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		float scrollOffset = scroll.getValue();

        if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
            expandButtonCollection.performForEach(x -> x.active, x -> x.mouseClicked(pMouseX, pMouseY + scrollOffset, pButton));
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
    
}
