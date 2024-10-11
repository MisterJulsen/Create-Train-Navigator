package de.mrjulsen.crn.client.gui.overlay;

import java.util.Set;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ModGuiUtils;
import de.mrjulsen.crn.client.gui.overlay.pages.AbstractRouteDetailsPage;
import de.mrjulsen.crn.client.gui.overlay.pages.ConnectionMissedPage;
import de.mrjulsen.crn.client.gui.overlay.pages.JourneyCompletedPage;
import de.mrjulsen.crn.client.gui.overlay.pages.NextConnectionsPage;
import de.mrjulsen.crn.client.gui.overlay.pages.RouteOverviewPage;
import de.mrjulsen.crn.client.gui.overlay.pages.TrainCancelledInfo;
import de.mrjulsen.crn.client.gui.overlay.pages.TransferPage;
import de.mrjulsen.crn.client.gui.overlay.pages.WelcomePage;
import de.mrjulsen.crn.client.gui.screen.RouteOverlaySettingsScreen;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.TransferConnection;
import de.mrjulsen.crn.registry.ModItems;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLOverlayScreen;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class RouteDetailsOverlay extends DLOverlayScreen {

    private static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/overview.png");
    private static final Component title = TextUtils.translate("gui.createrailwaysnavigator.route_overview.title");
    private static final int GUI_WIDTH = 226;
    private static final int GUI_HEIGHT = 118;
    private static final int SLIDING_TEXT_AREA_WIDTH = 220;

    private final Level level;    

    private Component slidingText = TextUtils.empty();
    private float slidingTextOffset = 0;
    private int slidingTextWidth = 0;

    private LerpedFloat xPos;
    private LerpedFloat yPos;
    
    private static final String keyTrainDetails = "gui.createrailwaysnavigator.route_overview.train_details";
    private static final String keyTransfer = "gui.createrailwaysnavigator.route_overview.transfer";
    private static final String keyTransferWithPlatform = "gui.createrailwaysnavigator.route_overview.transfer_with_platform";
    private static final String keyAfterJourney = "gui.createrailwaysnavigator.route_overview.after_journey";
    private static final String keyOptionsText = "gui.createrailwaysnavigator.route_overview.options";
    private static final String keyKeybindOptions = "key.createrailwaysnavigator.route_overlay_options";    
    private static final String keyJourneyBegins = "gui.createrailwaysnavigator.route_overview.journey_begins";
    private static final String keyJourneyBeginsWithPlatform = "gui.createrailwaysnavigator.route_overview.journey_begins_with_platform";
    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyConnectionMissedInfo = "gui.createrailwaysnavigator.route_overview.connection_missed_info";
    private static final String keyTrainCancelledInfo = "gui.createrailwaysnavigator.route_overview.train_cancelled_info";


    private final Font font = Minecraft.getInstance().font;
    private final ClientRoute route;
    private boolean journeyCompleted = false;

    private AbstractRouteDetailsPage currentPage;

    public RouteDetailsOverlay(Level level, ClientRoute route, int width, int height) {
        this.level = level;
        this.route = route;        
        route.addListener();

        {
            currentPage = new WelcomePage(this.route);
            String terminus = route.getStart().getDisplayTitle();
            StationInfo info = route.getStart().getRealTimeStationTag().info();
            setSlidingText(info.platform().isEmpty() ? ELanguage.translate(keyJourneyBegins, route.getStart().getTrainDisplayName(), terminus, TimeUtils.parseTime(route.getStart().getScheduledDepartureTime(), ModClientConfig.TIME_FORMAT.get())) : ELanguage.translate(keyJourneyBeginsWithPlatform, route.getStart().getTrainDisplayName(), terminus, TimeUtils.parseTime(route.getStart().getScheduledDepartureTime(), ModClientConfig.TIME_FORMAT.get()), info.platform()));
        }

        xPos = LerpedFloat.linear().startWithValue(width / 2 - (ModClientConfig.OVERLAY_SCALE.get() * (GUI_WIDTH / 2)));
        yPos = LerpedFloat.linear().startWithValue(height / 2 - (ModClientConfig.OVERLAY_SCALE.get() * (GUI_HEIGHT / 2)));

        if (route.isClosed()) return;

        route.listen(ClientRoute.EVENT_DEPARTURE_FROM_ANY_STOP, this, x -> {
            currentPage = new RouteOverviewPage(this.route);
            String terminus = x.part().getNextStop().getTerminusText();
            setSlidingText(ELanguage.translate(keyTrainDetails, x.part().getNextStop().getTrainDisplayName(), terminus == null || terminus.isEmpty() ? x.part().getNextStop().getScheduleTitle() : terminus));
        });
        route.listen(ClientRoute.EVENT_FIRST_STOP_STATION_CHANGED, this, x -> {
            setSlidingText(x.trainStop().getRealTimeStationTag().info().platform().isEmpty() ? ELanguage.translate(keyJourneyBegins) : ELanguage.translate(keyJourneyBeginsWithPlatform, x.trainStop().getRealTimeStationTag().info().platform()));
        });
        route.listen(ClientRoute.EVENT_ARRIVAL_AT_ANY_STOP, this, x -> {
            setSlidingText(TextUtils.text(x.trainStop().getClientTag().tagName()));
        });
        route.listen(ClientRoute.EVENT_ANY_STOP_ANNOUNCED, this, x -> {
            NextConnectionsPage page = new NextConnectionsPage(this.route, null);
            if (page.hasConnections()) {
                currentPage = page;
            }
        });
        route.listen(ClientRoute.EVENT_ANNOUNCE_STOPOVER, this, x -> {
            setSlidingText(ELanguage.translate(keyNextStop, x.trainStop().getClientTag().tagName()));
        });
        route.listen(ClientRoute.EVENT_ANNOUNCE_LAST_STOP, this, x -> {
            setSlidingText(ELanguage.translate(keyNextStop, x.trainStop().getClientTag().tagName()));
        });
        route.listen(ClientRoute.EVENT_ANNOUNCE_TRANSFER_ARRIVAL_STATION, this, x -> {
            if (x.connection().isConnectionMissed()) {
                connectionMissed();
                return;
            }
            setSlidingText(ELanguage.translate(keyNextStop, x.trainStop().getClientTag().tagName()).append("   ***   ").append(getTransferSlidingText(x.connection())));
            currentPage = new TransferPage(this.route, x.connection());
        });        
        route.listen(ClientRoute.EVENT_PART_CHANGED, this, x -> {
            if (x.connection().isConnectionMissed()) {
                connectionMissed();
            }
        });
        route.listen(ClientRoute.EVENT_DEPARTURE_FROM_TRANSFER_ARRIVAL_STATION, this, x -> {
            setSlidingText(TextUtils.text(x.connection().getArrivalStation().getClientTag().tagName()).append("   ***   ").append(getTransferSlidingText(x.connection())));
            currentPage = new TransferPage(this.route, x.connection());
        });
        route.listen(ClientRoute.EVENT_ARRIVAL_AT_LAST_STOP, this, x -> {
            setSlidingText(ELanguage.translate(keyAfterJourney, x.trainStop().getClientTag().tagName()));
            currentPage = new JourneyCompletedPage(this.route, () -> currentPage = new NextConnectionsPage(route, () -> {} /*InstanceManager::removeRouteOverlay*/));
            route.close();
        });
        route.listen(ClientRoute.EVENT_DEPARTURE_FROM_LAST_STOP, this, x -> {
            if (journeyCompleted) {
                return;
            }
            setSlidingText(ELanguage.translate(keyAfterJourney, x.trainStop().getClientTag().tagName()));
            currentPage = new JourneyCompletedPage(this.route, () -> currentPage = new NextConnectionsPage(route, () -> {} /*InstanceManager::removeRouteOverlay*/));
            route.close();
        });
        route.listen(ClientRoute.EVENT_ANY_TRANSFER_MISSED, this, x -> {
            connectionMissed();
        });
        route.listen(ClientRoute.EVENT_ANY_TRAIN_CANCELLED, this, x -> {
            trainCancelled(x.part().getLastStop().getTrainDisplayName());
        });
    }

    private Component getTransferSlidingText(TransferConnection connection) {        
        StationInfo info = connection.getDepartureStation().getRealTimeStationTag().info();
        String terminus = connection.getDepartureStation().getDisplayTitle();
        return (info == null || info.platform().isBlank() ? ELanguage.translate(keyTransfer, connection.getDepartureStation().getTrainDisplayName(), terminus) : ELanguage.translate(keyTransferWithPlatform, connection.getDepartureStation().getTrainDisplayName(), terminus, info.platform()));
    }

    private void connectionMissed() {
        setSlidingText(ELanguage.translate(keyConnectionMissedInfo));
        currentPage = new ConnectionMissedPage(this.route);
        route.close();
    }

    private void trainCancelled(String trainName) {
        setSlidingText(ELanguage.translate(keyTrainCancelledInfo, trainName));
        currentPage = new TrainCancelledInfo(this.route, trainName);
        route.close();
    }

    private float getUIScale() {
        return (float)ModClientConfig.OVERLAY_SCALE.get().doubleValue();
    }

    @Override
    public void onClose() {
        route.close();
        journeyCompleted = true;
    }


    @SuppressWarnings("resource")
    @Override
    public void tick() {
        if (Screen.hasControlDown() && ModKeys.KEY_OVERLAY_SETTINGS.isDown() && Minecraft.getInstance().player.getInventory().hasAnyOf(Set.of(ModItems.NAVIGATOR.get()))) {
            DLScreen.setScreen(new RouteOverlaySettingsScreen(this));
        }

        xPos.tickChaser();
        yPos.tickChaser();

        DLUtils.doIfNotNull(currentPage, x -> x.tick());
    }

    protected void tickSlidingText(float delta) {
        // Sliding text
        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * 0.75f) {
            slidingTextOffset -= delta;
            if (slidingTextOffset < -(slidingTextWidth / 2)) {
                slidingTextOffset = (int)((SLIDING_TEXT_AREA_WIDTH + slidingTextWidth / 2) + 20);                
            }
        }
    }

    //#region FUNCTIONS

    private void startStencil(Graphics graphics, int x, int y, int w, int h) {
        UIRenderHelper.swapAndBlitColor(Minecraft.getInstance().getMainRenderTarget(), UIRenderHelper.framebuffer);
        ModGuiUtils.startStencil(graphics, x, y, w, h);
    }

    private void endStencil() {
        ModGuiUtils.endStencil();
        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, Minecraft.getInstance().getMainRenderTarget());
    }

    private void setSlidingText(Component component) {
        slidingText = component;
        slidingTextWidth = font.width(component);

        if (slidingTextWidth > SLIDING_TEXT_AREA_WIDTH * 0.75f) {
            slidingTextOffset = (int)((SLIDING_TEXT_AREA_WIDTH + slidingTextWidth / 2) + 20);
        } else {
            slidingTextOffset = (int)(SLIDING_TEXT_AREA_WIDTH * 0.75f / 2);
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
        renderInternal(graphics, 0, 0, width, height, partialTicks, (int)xPos.getValue(partialTicks), (int)yPos.getValue(partialTicks));
        graphics.poseStack().popPose();

        tickSlidingText(2 * Minecraft.getInstance().getDeltaFrameTime());
    }

    public void renderSlidingText(Graphics graphics, int x, int y, int transX, int transY) {
        startStencil(graphics, x + 3, y + 14, 220, 21);
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(1.0f / 0.75f, 1.0f / 0.75f, 1.0f / 0.75f);
        GuiUtils.drawString(graphics, font, (int)((x + 3) + slidingTextOffset), y + 14, slidingText, 0xFF9900, EAlignment.CENTER, false);
        graphics.poseStack().popPose();
        endStencil();
    }

    private void renderInternal(Graphics graphics, int x, int y, int width, int height, float partialTicks, int transX, int transY) {
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(getUIScale(), getUIScale(), getUIScale());
        RenderSystem.setShaderTexture(0, GUI);
        GuiUtils.drawTexture(GUI, graphics, x, y, GUI_WIDTH, GUI_HEIGHT, 0, currentPage != null && currentPage.isImportant() ? 138 : 0, 256, 256);
        
        GuiUtils.drawString(graphics, font, x + 6, y + 4, title, 0x4F4F4F, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, x + 6, y + GUI_HEIGHT - 2 - font.lineHeight, TextUtils.translate(keyOptionsText, TextUtils.translate(InputConstants.getKey(Minecraft.ON_OSX ? InputConstants.KEY_LWIN : InputConstants.KEY_LCONTROL, 0).getName()).append(" + ").append(new KeybindComponent(keyKeybindOptions)).withStyle(ChatFormatting.BOLD)), 0x4F4F4F, EAlignment.LEFT, false);
        
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        GuiUtils.drawString(graphics, font, x + GUI_WIDTH - 4 - font.width(timeString), y + 4, timeString, 0x4F4F4F, EAlignment.LEFT, false);
        
        renderSlidingText(graphics, x, y + 2, transX, transY);

        startStencil(graphics, x + 3, y + 40, 220, 62);
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(3, 40, 0);
        DLUtils.doIfNotNull(currentPage, a -> {
            a.renderBackLayer(graphics, 0, 0, partialTicks);
            a.renderMainLayer(graphics, 0, 0, partialTicks);
        });
        graphics.poseStack().popPose();
        endStencil();
        DLUtils.doIfNotNull(currentPage, a -> a.renderFrontLayer(graphics, 0, 0, partialTicks));
        if (CreateRailwaysNavigator.isDebug()) GuiUtils.drawString(graphics, font, 5, GUI_HEIGHT + 10, "State: " + route.getState() + ", " + route.getCurrentPartIndex() + ", " + route.getCurrentPart().getNextStop().getClientTag().tagName(), 0xFFFF0000, EAlignment.LEFT, false);
        graphics.poseStack().popPose();
    }

    public ClientRoute getRoute() {
        return route;
    }
}
