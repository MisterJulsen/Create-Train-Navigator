package de.mrjulsen.crn.client.gui.overlay;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.overlay.pages.AbstractRouteDetailsPage;
import de.mrjulsen.crn.client.gui.overlay.pages.ConnectionMissedPage;
import de.mrjulsen.crn.client.gui.overlay.pages.JourneyCompletedPage;
import de.mrjulsen.crn.client.gui.overlay.pages.NextConnectionsPage;
import de.mrjulsen.crn.client.gui.overlay.pages.RouteOverviewPage;
import de.mrjulsen.crn.client.gui.overlay.pages.TrainCancelledInfo;
import de.mrjulsen.crn.client.gui.overlay.pages.TransferPage;
import de.mrjulsen.crn.client.gui.overlay.pages.WelcomePage;
import de.mrjulsen.crn.client.gui.widgets.skins.CRNFlatButtonRenderer;
import de.mrjulsen.crn.client.gui.windows.RouteDetailsWindow;
import de.mrjulsen.crn.client.gui.windows.RouteOverlaySettingsWindow;
import de.mrjulsen.crn.client.input.ModKeys;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.StationTag.StationInfo;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.TransferConnection;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.BorderLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.TableLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.TableLayout.ColumnSizeMode;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class RouteDetailsOverlay extends DLWindow {

    private static final Component title = TextUtils.translate("gui.createrailwaysnavigator.route_overview.title");
    private static final int GUI_WIDTH = 226;
    private static final int GUI_HEIGHT = 118;
    private static final int SCROLL_AREA_HEIGHT = 25;
    private static final int SLIDING_TEXT_AREA_WIDTH = 220;

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

    private final DLPanel contentPanel;
    private final SlidingTextComponent slidingTextComponent;
    private AbstractRouteDetailsPage currentPage;

    public RouteDetailsOverlay(DLWindowManager manager, Level level, ClientRoute route, int width, int height) {
        super(manager);
        this.route = route;
        route.addListener();

        scale.set(ModClientConfig.OVERLAY_SCALE.get());
        setSize(GUI_WIDTH, GUI_HEIGHT);

        windowSpawnPosition.set(WindowPosition.CENTER);
        xPos = LerpedFloat.linear().startWithValue(manager.getScreenWidth() / 2 - (scale.get() * (width() / 2)));
        yPos = LerpedFloat.linear().startWithValue(manager.getScreenHeight() / 2 - (scale.get() * (height() / 2)));
        
        final int dy = FooterSize.DEFAULT.size() + SCROLL_AREA_HEIGHT;
        contentPanel = addComponent(new DLPanel(3, dy, width() - 6, height() - dy - FooterSize.DEFAULT.size() - 1));
        contentPanel.layout.set(new BorderLayout(0, 0));

        slidingTextComponent = addComponent(new SlidingTextComponent(3, FooterSize.DEFAULT.size() + 1, width() - 6));

        final int optionsPanelSize = 11;
        DLPanel optionsPanel = addComponent(new DLPanel(3, height() - 2 - optionsPanelSize, width() - 6, optionsPanelSize));
        TableLayout layout = new TableLayout();
        layout.addColumn("void", 1, ColumnSizeMode.PERCENTAGE);
        layout.addColumn("settings", 1, ColumnSizeMode.AUTO);
        layout.addColumn("popout", 1, ColumnSizeMode.AUTO);
        layout.addColumn("close", 1, ColumnSizeMode.AUTO);
        optionsPanel.layout.set(layout);
        
        
        DLButton closeBtn = optionsPanel.addComponent(new DLButton(0, 0, 16, optionsPanelSize));    
        closeBtn.layoutContraint.set("close");    
        closeBtn.icon.set(ModGuiIcons.X_SMALL.getAsSprite(16, 16));
        closeBtn.text.set(TextUtils.empty());
        closeBtn.textColor.set(DragonLib.VANILLA_UI_FONT_COLOR);
        closeBtn.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
        closeBtn.tooltip.set(new DLTooltip(List.of(TextUtils.TEXT_CLOSE), width()));
        closeBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });

        DLButton settingsBtn = optionsPanel.addComponent(new DLButton(0, 0, 16, optionsPanelSize));
        settingsBtn.layoutContraint.set("settings");
        settingsBtn.icon.set(ModGuiIcons.SETTINGS_SMALL.getAsSprite(16, 16));
        settingsBtn.text.set(TextUtils.empty());
        settingsBtn.textColor.set(DragonLib.VANILLA_UI_FONT_COLOR);
        settingsBtn.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
        settingsBtn.tooltip.set(new DLTooltip(List.of(TextUtils.text("Settings")), width()));
        settingsBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            DLWindow.openWindow(mgr -> new RouteOverlaySettingsWindow(mgr, this));
            return false;
        });
        

        DLButton popoutBtn = optionsPanel.addComponent(new DLButton(0, 0, 16, optionsPanelSize));
        popoutBtn.layoutContraint.set("popout");
        popoutBtn.icon.set(ModGuiIcons.POP_OUT.getAsSprite(16, 16));
        popoutBtn.text.set(TextUtils.empty());
        popoutBtn.textColor.set(DragonLib.VANILLA_UI_FONT_COLOR);
        popoutBtn.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
        popoutBtn.tooltip.set(new DLTooltip(List.of(TextUtils.text("Show route details")), width()));
        popoutBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            DLWindow.openWindow(mgr -> new RouteDetailsWindow(mgr, route));
            return false;
        });

        
        addEventListener(DLGuiStandardEvents.ComponentPosAndSizeChanged.class, (s, e) -> {
            contentPanel.setPosition(3, dy);
            contentPanel.setSize(width() - 6, height() - dy - FooterSize.DEFAULT.size() - 1);
            optionsPanel.setPosition(3, height() - 2 - optionsPanelSize);
            optionsPanel.setSize(width() - 6, optionsPanelSize);
            return false;

        });

        {
            setPage(new WelcomePage(this.route));
            String terminus = route.getStart().getDisplayTitle();
            StationInfo info = route.getStart().getRealTimeStationTag().info();
            String departureTimeText = DLTime.fromTicks(route.getStart().getScheduledDepartureTime(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME);
            setSlidingText(info.platform().isEmpty() ? CustomLanguage.translate(keyJourneyBegins, route.getStart().getTrainDisplayName(), terminus, departureTimeText) : CustomLanguage.translate(keyJourneyBeginsWithPlatform, route.getStart().getTrainDisplayName(), terminus, departureTimeText, info.platform()));
        }

        if (route.isClosed()) return;

        route.listen(ClientRoute.EVENT_DEPARTURE_FROM_ANY_STOP, this, x -> {
            setPage(new RouteOverviewPage(this.route));
            String terminus = x.part().getNextStop().getTerminusText();
            setSlidingText(CustomLanguage.translate(keyTrainDetails, x.part().getNextStop().getTrainDisplayName(), terminus == null || terminus.isEmpty() ? x.part().getNextStop().getScheduleTitle() : terminus));
        });
        route.listen(ClientRoute.EVENT_FIRST_STOP_STATION_CHANGED, this, x -> {
            setSlidingText(x.trainStop().getRealTimeStationTag().info().platform().isEmpty() ? CustomLanguage.translate(keyJourneyBegins) : CustomLanguage.translate(keyJourneyBeginsWithPlatform, x.trainStop().getRealTimeStationTag().info().platform()));
        });
        route.listen(ClientRoute.EVENT_ARRIVAL_AT_ANY_STOP, this, x -> {
            setSlidingText(TextUtils.text(x.trainStop().getRealTimeStationTag().tagName()));
        });
        route.listen(ClientRoute.EVENT_ANY_STOP_ANNOUNCED, this, x -> {
            NextConnectionsPage page = new NextConnectionsPage(this.route, null);
            if (page.hasConnections()) {
                setPage(page);
            }
        });
        route.listen(ClientRoute.EVENT_ANNOUNCE_STOPOVER, this, x -> {
            setSlidingText(CustomLanguage.translate(keyNextStop, x.trainStop().getRealTimeStationTag().tagName()));
        });
        route.listen(ClientRoute.EVENT_ANNOUNCE_LAST_STOP, this, x -> {
            setSlidingText(CustomLanguage.translate(keyNextStop, x.trainStop().getRealTimeStationTag().tagName()));
        });
        route.listen(ClientRoute.EVENT_ANNOUNCE_TRANSFER_ARRIVAL_STATION, this, x -> {
            if (x.connection().isConnectionMissed()) {
                connectionMissed();
                return;
            }
            setSlidingText(CustomLanguage.translate(keyNextStop, x.trainStop().getRealTimeStationTag().tagName()).append("   ***   ").append(getTransferSlidingText(x.connection())));
            setPage(new TransferPage(this.route, x.connection()));
        });        
        route.listen(ClientRoute.EVENT_PART_CHANGED, this, x -> {
            if (x.connection().isConnectionMissed()) {
                connectionMissed();
            }
        });
        route.listen(ClientRoute.EVENT_DEPARTURE_FROM_TRANSFER_ARRIVAL_STATION, this, x -> {
            setSlidingText(TextUtils.text(x.connection().getArrivalStation().getRealTimeStationTag().tagName()).append("   ***   ").append(getTransferSlidingText(x.connection())));
            setPage(new TransferPage(this.route, x.connection()));
        });
        route.listen(ClientRoute.EVENT_ARRIVAL_AT_LAST_STOP, this, x -> {
            setSlidingText(CustomLanguage.translate(keyAfterJourney, x.trainStop().getRealTimeStationTag().tagName()));
            setPage(new JourneyCompletedPage(this.route, () -> setPage(new NextConnectionsPage(route, () -> {} /*InstanceManager::removeRouteOverlay*/))));
            route.close();
        });
        route.listen(ClientRoute.EVENT_DEPARTURE_FROM_LAST_STOP, this, x -> {
            if (journeyCompleted) {
                return;
            }
            setSlidingText(CustomLanguage.translate(keyAfterJourney, x.trainStop().getRealTimeStationTag().tagName()));
            setPage(new JourneyCompletedPage(this.route, () -> setPage(new NextConnectionsPage(route, () -> {} /*InstanceManager::removeRouteOverlay*/))));
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
        return (info == null || info.platform().isBlank() ? CustomLanguage.translate(keyTransfer, connection.getDepartureStation().getTrainDisplayName(), terminus) : CustomLanguage.translate(keyTransferWithPlatform, connection.getDepartureStation().getTrainDisplayName(), terminus, info.platform()));
    }

    private void connectionMissed() {
        setSlidingText(CustomLanguage.translate(keyConnectionMissedInfo));
        setPage(new ConnectionMissedPage(this.route));
        route.close();
    }

    private void trainCancelled(String trainName) {
        setSlidingText(CustomLanguage.translate(keyTrainCancelledInfo, trainName));
        setPage(new TrainCancelledInfo(this.route, trainName));
        route.close();
    }

    public void setPage(AbstractRouteDetailsPage page) {
        contentPanel.clearComponents();
        page.layoutContraint.set(BorderLayout.BorderPosition.CENTER);
        contentPanel.addComponent(page);
        currentPage = page;
    }

    @Override
    public void close() {
        route.close();
        journeyCompleted = true;
    }

    @Override
    public void tick() {
        if (Screen.hasControlDown() && ModKeys.KEY_OVERLAY_SETTINGS.isDown()) {
            DLWindow.openWindow(mgr -> new RouteOverlaySettingsWindow(mgr, this));
        }
        xPos.tickChaser();
        yPos.tickChaser();
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

    private void setSlidingText(Component component) {
        slidingTextComponent.text.set(component);
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
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        
        OverlayPosition pos = ModClientConfig.ROUTE_OVERLAY_POSITION.get();
        final int x = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.BOTTOM_LEFT ? 8 : (int)(getWindowManager().getScreenWidth() - width() * scale.get() - 10);
        final int y = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.TOP_RIGHT ? 8 : (int)(getWindowManager().getScreenHeight() - height() * scale.get() - 10);
        
        xPos.chase(x, 0.2f, Chaser.EXP);
        yPos.chase(y, 0.2f, Chaser.EXP);

        setPosition(xPos.getValue(Minecraft.getInstance().getFrameTime()), yPos.getValue(Minecraft.getInstance().getFrameTime()));

        CreateDynamicWidgets.renderWindow(graphics, 0, 0, width(), height(), currentPage.isImportant() ? ContainerColor.GOLD : ContainerColor.BLUE, currentPage.isImportant() ? BarColor.GOLD : BarColor.GRAY, FooterSize.DEFAULT.size(), FooterSize.DEFAULT.size(), false);
        CreateDynamicWidgets.renderContainer(graphics, 1, FooterSize.DEFAULT.size() - 1, width() - 2, SCROLL_AREA_HEIGHT, ContainerColor.GRAY);
        int dy = FooterSize.DEFAULT.size() + SCROLL_AREA_HEIGHT - 2;
        CreateDynamicWidgets.renderContainer(graphics, 1, dy, width() - 2, height() - dy - FooterSize.DEFAULT.size() + 1, currentPage.isImportant() ? ContainerColor.GOLD : ContainerColor.BLUE);
        GuiUtils.drawString(graphics, font, 6, 4, title, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);
        Component timeText = TextUtils.text(DLTime.fromLevelTime(Minecraft.getInstance().level, new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME));
        GuiUtils.drawString(graphics, font, width() - 6, 4, timeText, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.RIGHT, false);

        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, height() - 2 - graphics.defaultFont().lineHeight, TextUtils.truncateWithEllipsis(graphics.defaultFont(), TextUtils.translate(keyOptionsText, TextUtils.translate(InputConstants.getKey(Minecraft.ON_OSX ? InputConstants.KEY_LWIN : InputConstants.KEY_LCONTROL, 0).getName()).append(" + ").append(TextUtils.keybind(keyKeybindOptions)).withStyle(ChatFormatting.BOLD)), width() - 50), DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);
    }

    public ClientRoute getRoute() {
        return route;
    }
}
