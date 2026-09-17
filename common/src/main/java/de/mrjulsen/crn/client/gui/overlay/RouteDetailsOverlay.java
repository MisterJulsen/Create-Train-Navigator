package de.mrjulsen.crn.client.gui.overlay;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import de.mrjulsen.crn.client.ClientWrapper;
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
import de.mrjulsen.crn.client.journey.JourneyPhase;
import de.mrjulsen.crn.client.journey.JourneyTracker;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.settings.SavedRoutesManager;
import de.mrjulsen.crn.core.navigator.route.RouteCall;
import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.crn.core.navigator.route.RouteLeg;
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
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RouteDetailsOverlay extends DLWindow {

    private static final Component title = TextUtils.translate("gui.createrailwaysnavigator.route_overview.title");
    private static final int GUI_WIDTH = 226;
    private static final int GUI_HEIGHT = 118;
    private static final int SCROLL_AREA_HEIGHT = 25;

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

    private static final String keyNotificationJourneyBeginsTitle = "gui.createrailwaysnavigator.route_overview.notification.journey_begins.title";
    private static final String keyNotificationJourneyBegins = "gui.createrailwaysnavigator.route_overview.notification.journey_begins";
    private static final String keyNotificationJourneyBeginsWithPlatform = "gui.createrailwaysnavigator.route_overview.notification.journey_begins_with_platform";
    private static final String keyNotificationJourneyCompletedTitle = "gui.createrailwaysnavigator.route_overview.notification.journey_completed.title";
    private static final String keyNotificationJourneyCompleted = "gui.createrailwaysnavigator.route_overview.notification.journey_completed";
    private static final String keyNotificationConnectionMissedTitle = "gui.createrailwaysnavigator.route_overview.notification.connection_missed.title";
    private static final String keyNotificationConnectionMissed = "gui.createrailwaysnavigator.route_overview.notification.connection_missed";
    private static final String keyNotificationTrainCancelledTitle = "gui.createrailwaysnavigator.route_overview.notification.train_cancelled.title";
    private static final String keyNotificationTrainCancelled = "gui.createrailwaysnavigator.route_overview.notification.train_cancelled";

    private final LerpedFloat xPos;
    private final LerpedFloat yPos;

    private final Font font = Minecraft.getInstance().font;
    private final JourneyTracker tracker;

    private final DLPanel contentPanel;
    private final SlidingTextComponent slidingTextComponent;
    private AbstractRouteDetailsPage currentPage;
    private boolean showNotifications = true;

    public RouteDetailsOverlay(DLWindowManager manager, RouteJourney route, int width, int height) {
        super(manager);
        this.tracker = new JourneyTracker(route);

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
            DLWindow.openWindow(mgr -> new RouteDetailsWindow(mgr, tracker.journey()));
            return false;
        });

        addEventListener(DLGuiStandardEvents.ComponentPosAndSizeChanged.class, (s, e) -> {
            contentPanel.setPosition(3, dy);
            contentPanel.setSize(width() - 6, height() - dy - FooterSize.DEFAULT.size() - 1);
            optionsPanel.setPosition(3, height() - 2 - optionsPanelSize);
            optionsPanel.setSize(width() - 6, optionsPanelSize);
            return false;
        });

        applyPhase(tracker.phase());

        tracker.addListener(new JourneyTracker.Listener() {
            @Override
            public void onPhaseChanged(JourneyPhase phase, JourneyTracker source) {
                applyPhase(phase);
            }

            @Override
            public void onNextCallChanged(RouteCall call, JourneyTracker source) {
                if (source.phase() == JourneyPhase.RIDING) {
                    setSlidingText(CustomLanguage.translate(keyNextStop, call.station().displayName()));
                }
            }
        });
        tracker.start();
    }

    private void applyPhase(JourneyPhase phase) {
        switch (phase) {
            case BEFORE_DEPARTURE -> {
                setPage(new WelcomePage(tracker));
                setSlidingText(journeyBeginsText());
                notify(CustomLanguage.translate(keyNotificationJourneyBeginsTitle, tracker.journey().destination().displayName()), journeyBeginsNotification());
            }
            case RIDING -> {
                RouteLeg leg = tracker.currentLeg();
                setPage(new RouteOverviewPage(tracker));
                setSlidingText(CustomLanguage.translate(keyTrainDetails, leg.displayName(), leg.destinationText()));
            }
            case TRANSFERRING -> {
                RouteLeg leg = tracker.currentLeg();
                tracker.currentTransfer().ifPresent(transfer -> setPage(new TransferPage(tracker, transfer, leg)));
                setSlidingText(transferText(leg));
            }
            case COMPLETED -> {
                setSlidingText(CustomLanguage.translate(keyAfterJourney, tracker.journey().destination().displayName()));
                setPage(new JourneyCompletedPage(tracker, this::showNextConnections));
                notify(CustomLanguage.translate(keyNotificationJourneyCompletedTitle), CustomLanguage.translate(keyNotificationJourneyCompleted));
                forgetSavedRoute();
            }
            case CONNECTION_MISSED -> {
                setSlidingText(CustomLanguage.translate(keyConnectionMissedInfo));
                setPage(new ConnectionMissedPage(tracker));
                RouteLeg missedLeg = tracker.currentLeg();
                notify(CustomLanguage.translate(keyNotificationConnectionMissedTitle), CustomLanguage.translate(keyNotificationConnectionMissed, missedLeg.displayName(), missedLeg.destinationText()));
            }
            case TRAIN_CANCELLED -> {
                RouteLeg cancelledLeg = tracker.currentLeg();
                String trainName = cancelledLeg.displayName();
                setSlidingText(CustomLanguage.translate(keyTrainCancelledInfo, trainName));
                setPage(new TrainCancelledInfo(tracker, trainName));
                notify(CustomLanguage.translate(keyNotificationTrainCancelledTitle, trainName), CustomLanguage.translate(keyNotificationTrainCancelled, trainName, cancelledLeg.destinationText()));
            }
        }
    }

    private void notify(Component title, Component description) {
        if (showNotifications) {
            ClientWrapper.sendCRNNotification(title, description);
        }
    }

    public boolean shouldShowNotifications() {
        return showNotifications;
    }

    public void setShowNotifications(boolean value) {
        this.showNotifications = value;
    }

    private void showNextConnections() {
        NextConnectionsPage page = new NextConnectionsPage(tracker, () -> {});
        if (page.hasConnections()) {
            setPage(page);
        }
    }

    private void forgetSavedRoute() {
        if (SavedRoutesManager.isSaved(tracker.journey())) {
            SavedRoutesManager.removeRoute(tracker.journey());
            SavedRoutesManager.push(true, null);
        }
    }

    private Component journeyBeginsText() {
        RouteLeg leg = tracker.journey().firstLeg();
        String platform = leg.boarding().platform();
        String departureTimeText = clockTime(leg.boarding().scheduled().departure());
        return platform == null || platform.isBlank()
            ? CustomLanguage.translate(keyJourneyBegins, leg.displayName(), leg.destinationText(), departureTimeText)
            : CustomLanguage.translate(keyJourneyBeginsWithPlatform, leg.displayName(), leg.destinationText(), departureTimeText, platform);
    }

    private Component journeyBeginsNotification() {
        RouteLeg leg = tracker.journey().firstLeg();
        String platform = leg.boarding().platform();
        String departureTimeText = clockTime(leg.boarding().scheduled().departure());
        return platform == null || platform.isBlank()
            ? CustomLanguage.translate(keyNotificationJourneyBegins, leg.displayName(), leg.destinationText(), departureTimeText)
            : CustomLanguage.translate(keyNotificationJourneyBeginsWithPlatform, leg.displayName(), leg.destinationText(), departureTimeText, platform);
    }

    private Component transferText(RouteLeg connectingLeg) {
        String platform = connectingLeg.boarding().platform();
        return platform == null || platform.isBlank()
            ? CustomLanguage.translate(keyTransfer, connectingLeg.displayName(), connectingLeg.destinationText())
            : CustomLanguage.translate(keyTransferWithPlatform, connectingLeg.displayName(), connectingLeg.destinationText(), platform);
    }

    private static String clockTime(long ticks) {
        return new DLTime(ticks, VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME, DLTime.defaultTimeSystem());
    }

    public void setPage(AbstractRouteDetailsPage page) {
        contentPanel.clearComponents();
        page.layoutContraint.set(BorderLayout.BorderPosition.CENTER);
        contentPanel.addComponent(page);
        currentPage = page;
    }

    @Override
    public void close() {
        tracker.close();
    }

    @Override
    public void tick() {
        if (Screen.hasControlDown() && ModKeys.KEY_OVERLAY_SETTINGS.isDown()) {
            DLWindow.openWindow(mgr -> new RouteOverlaySettingsWindow(mgr, this));
        }
        xPos.tickChaser();
        yPos.tickChaser();
    }

    private void setSlidingText(Component component) {
        slidingTextComponent.text.set(component);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {

        OverlayPosition pos = ModClientConfig.ROUTE_OVERLAY_POSITION.get();
        final int x = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.BOTTOM_LEFT ? 8 : (int)(getWindowManager().getScreenWidth() - width() * scale.get() - 10);
        final int y = pos == OverlayPosition.TOP_LEFT || pos == OverlayPosition.TOP_RIGHT ? 8 : (int)(getWindowManager().getScreenHeight() - height() * scale.get() - 10);

        xPos.chase(x, 0.2f, Chaser.EXP);
        yPos.chase(y, 0.2f, Chaser.EXP);

        setPosition(xPos.getValue(Minecraft.getInstance().getFrameTime()), yPos.getValue(Minecraft.getInstance().getFrameTime()));

        boolean important = currentPage != null && currentPage.isImportant();
        CreateDynamicWidgets.renderWindow(graphics, 0, 0, width(), height(), important ? ContainerColor.GOLD : ContainerColor.BLUE, important ? BarColor.GOLD : BarColor.GRAY, FooterSize.DEFAULT.size(), FooterSize.DEFAULT.size(), false);
        CreateDynamicWidgets.renderContainer(graphics, 1, FooterSize.DEFAULT.size() - 1, width() - 2, SCROLL_AREA_HEIGHT, ContainerColor.GRAY);
        int dy = FooterSize.DEFAULT.size() + SCROLL_AREA_HEIGHT - 2;
        CreateDynamicWidgets.renderContainer(graphics, 1, dy, width() - 2, height() - dy - FooterSize.DEFAULT.size() + 1, important ? ContainerColor.GOLD : ContainerColor.BLUE);
        GuiUtils.drawString(graphics, font, 6, 4, title, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);
        Component timeText = TextUtils.text(new DLTime(Minecraft.getInstance().level, DLTime.defaultTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME, DLTime.defaultTimeSystem()));
        GuiUtils.drawString(graphics, font, width() - 6, 4, timeText, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.RIGHT, false);

        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, height() - 2 - graphics.defaultFont().lineHeight, TextUtils.truncateWithEllipsis(graphics.defaultFont(), TextUtils.translate(keyOptionsText, TextUtils.translate(InputConstants.getKey(Minecraft.ON_OSX ? InputConstants.KEY_LWIN : InputConstants.KEY_LCONTROL, 0).getName()).append(" + ").append(TextUtils.keybind(keyKeybindOptions)).withStyle(ChatFormatting.BOLD)), width() - 50), DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);
    }

    public RouteJourney getRoute() {
        return tracker.journey();
    }
}
