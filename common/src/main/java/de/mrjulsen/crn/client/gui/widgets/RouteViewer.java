package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.widgets.skins.CRNFlatButtonRenderer;
import de.mrjulsen.crn.client.journey.JourneyTracker;
import de.mrjulsen.crn.client.gui.Animator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.crn.data.settings.RecentSearchQueries;
import de.mrjulsen.crn.data.settings.UserSettings;
import de.mrjulsen.crn.data.settings.RecentSearchQueries.RecentSearchQuery;
import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.crn.network.packets.NavigatePacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.Orientation;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class RouteViewer extends DLGuiComponent {

    private final MutableComponent searchingText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.searching");
    private final MutableComponent noConnectionsText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.no_connections");
    private final MutableComponent notSearchedText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.not_searched");
    private final MutableComponent txtRecentSearchQueries = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.recently_searched").withStyle(ChatFormatting.BOLD);
    private final MutableComponent txtRecentSearchQueryPins = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.recently_searched_pins").withStyle(ChatFormatting.BOLD);

    public final BooleanProperty displayRecentSearchQueries = new BooleanProperty(false);

    private final List<RouteJourney> routes = new ArrayList<>();
    private final List<JourneyTracker> trackers = new ArrayList<>();

    private final DLPanel contentPanel;
    private final DLScrollBar scrollbar;
    private UserSettings settings;
    private final Animator animator;

    private int angle = 0;

    private double animPercentage = 0;
    private double renderOffsetX = 0;
    private boolean animationStarted = false;

    private boolean hasSearched = false;
    private boolean isLoading = false;
    private int contentHeight = 0;

    public RouteViewer(int x, int y, int w, int h) {
        super(x, y, w, h);
        this.animator = addComponent(new Animator());

        contentPanel = addComponent(new DLPanel(0, 0, width(), height()));
        contentPanel.anchor.set2(EAlign.values());
        contentPanel.inputConsumptionPolicy.set((type) -> false);

        scrollbar = addComponent(new DLScrollBar(width() - 5, 0, 5, height(), Orientation.VERTICAL));
        scrollbar.componentRenderer.set(ModernScrollbarComponentRenderer.INSTANCE);
        scrollbar.anchor.set2(EAlign.TOP, EAlign.BOTTOM, EAlign.RIGHT);
        scrollbar.scrollerSize.set(0);
        scrollbar.screenSize.set(height());
        scrollbar.scrollSteps.set(10);
        scrollbar.max.set(0);
        scrollbar.inputConsumptionPolicy.set((type) -> true);
        scrollbar.addEventListener(DLScrollBar.ValueChangedEvent.class, (s, e) -> {
            contentPanel.setScrollOffsetY(e.value());
            return false;
        });

        addEventListener(DLGuiStandardEvents.ScrollEvent.class, scrollbar::invokeEvent);

        DLContextMenu recetlySearchedMenu = new DLContextMenu((pX, pY) -> {
            List<DLContextMenu.ItemEntry> entries = new ArrayList<>();
            entries.add(new DLContextMenu.ItemEntry(TextUtils.text("Clear"), DLSprite.empty(), true, () -> {
                settings.recentSearchQueries.getValue().clearQueries();
                clear();
            }, null));
            return entries;
        });

        DLContextMenu routesViewMenu = new DLContextMenu((pX, pY) -> {
            List<DLContextMenu.ItemEntry> entries = new ArrayList<>();
            entries.add(new DLContextMenu.ItemEntry(TextUtils.text("Clear"), DLSprite.empty(), true, () -> {
                clear();
            }, null));
            entries.add(DLContextMenu.ItemEntry.SEPARATOR);
            entries.add(new DLContextMenu.ItemEntry(TextUtils.text("Refresh"), DLSprite.empty(), true, () -> {
            }, null));
            return entries;
        });

        addEventListener(DLGuiStandardEvents.RightClickEvent.class, (src, event) -> {
            if (this.hasSearched) {
                routesViewMenu.open(getWindowManager(), (int)getWindowManager().mouseXOnScreen(), (int)getWindowManager().mouseYOnScreen());
            } else {
                recetlySearchedMenu.open(getWindowManager(), (int)getWindowManager().mouseXOnScreen(), (int)getWindowManager().mouseYOnScreen());
            }
            return false;
        });

        addEventListener(DLGuiStandardEvents.ComponentPosAndSizeChanged.class, (src, event) -> {
            loadList();
            return false;
        });
    }

    public void search(String start, String end, Runnable andThen) {
        clear();
        this.isLoading = true;
        settings.recentSearchQueries.getValue().add(new RecentSearchQuery(start, end));
        contentPanel.clearComponents();
        this.animPercentage = 0;
        this.renderOffsetX = -50;

        settings.clientSave(() -> {
            animator.start(10, (poseStack, current, total, percentage) -> {
                this.animPercentage = Math.pow(1D - percentage, 4);
                this.renderOffsetX = -(50 * animPercentage);
            }, null, () -> {
                this.renderOffsetX = 0;
                ModNetworkManager.NAVIGATE.send(NetworkDirection.toServer(), new NavigatePacketData.Request(start, end, Minecraft.getInstance().player.getUUID()), (response) -> {
                    this.routes.clear();
                    this.routes.addAll(response.getData());
                    animator.start(10, (poseStack, current, total, percentage) -> {
                        this.animPercentage = Math.pow(percentage, 4);
                        this.renderOffsetX = (50 * animPercentage);
                    }, null, () -> {
                        this.animationStarted = false;
                        this.animPercentage = 0;
                        this.renderOffsetX = 0;

                        loadList();
                        andThen.run();
                        hasSearched = true;
                        this.isLoading = false;
                    });
                }, andThen::run);
            });
            this.animationStarted = true;
        });

    }

    public void updateSettings(UserSettings settings) {
        if (this.isLoading) {
            return;
        }
        this.settings = settings;
        loadList();
    }

    @Override
    public void close() throws Exception {
        stopTracking();
        super.close();
    }

    private void startTracking() {
        stopTracking();
        for (RouteJourney route : routes) {
            JourneyTracker tracker = new JourneyTracker(route);
            tracker.start();
            trackers.add(tracker);
        }
    }

    private void stopTracking() {
        trackers.forEach(JourneyTracker::close);
        trackers.clear();
    }

    void loadList() {
        contentPanel.clearComponents();
        contentHeight = 0;
        startTracking();
        if (this.displayRecentSearchQueries.get() && !this.hasSearched && !this.isLoading && this.routes.isEmpty() && settings != null) {
            DLPanel panel = contentPanel.addComponent(new DLPanel(0, 0, contentPanel.width(), contentPanel.height()));
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.fillCrossAxis.set(true);
            flowLayout.wrap.set(false);
            flowLayout.flowDirection.set(FlowLayout.Direction.VERTICAL);
            flowLayout.padding.set(new Padding(0, 10, 0, 10));
            panel.layout.set(flowLayout);
            panel.inputConsumptionPolicy.set(c -> false);
            panel.anchor.set(EAlign.values());

            RecentSearchQueries recent = settings.recentSearchQueries.getValue();
            boolean hasPins = recent.hasPins();
            int availableSpace = contentPanel.height() - Label.HEIGHT.get() * (hasPins ? 2 : 1);
            int maxEntries = availableSpace / RecentSearchQueryButton.getHeight();
            int maxPinnedEntries = Math.min(maxEntries - 1, recent.pinnedSize());
            int maxRecentEntries = maxEntries - maxPinnedEntries;

            if (hasPins) {
                panel.addComponent(new Label(0, 0, txtRecentSearchQueryPins));
                RecentSearchQuery[] pins = recent.getAllPins();
                for (int i = 0; i < Math.min(pins.length, maxPinnedEntries); i++) {
                    RecentSearchQuery query = pins[i];
                    RecentSearchQueryButton btn = panel.addComponent(new RecentSearchQueryButton(this, 0, 0, 1, query));
                }
            }

            panel.addComponent(new Label(0, 0, txtRecentSearchQueries));
            RecentSearchQuery[] queries = recent.getAll();
            for (int i = 0; i < Math.min(queries.length, maxRecentEntries); i++) {
                RecentSearchQuery query = queries[i];
                RecentSearchQueryButton btn = panel.addComponent(new RecentSearchQueryButton(this, 0, 0, 1, query));
            }
        } else if (this.routes.isEmpty()) {
            DLButton btn = new DLButton(contentPanel.width() / 2 - 10, contentPanel.height() / 2 + 25, 20, 20);
            btn.anchor.set2();
            btn.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
            btn.text.set(TextUtils.EMPTY);
            btn.icon.set(ModGuiIcons.X.getAsSprite(ModGuiIcons.ICON_SIZE, ModGuiIcons.ICON_SIZE));
            contentPanel.addComponent(btn);
            btn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                clear();
                return false;
            });
        } else if (!this.routes.isEmpty()) {
            contentHeight = 5;
            for (RouteJourney route : routes) {
                RouteWidget w = contentPanel.addComponent(new RouteWidget(10, contentHeight, route));
                contentHeight += (w.height() + 3);
            }
            contentHeight += 3;
        }
        scrollbar.max.set(contentHeight);
    }


    public UserSettings getUserSettings() {
        return settings;
    }

    public void clear() {
        stopTracking();
        routes.clear();
        scrollbar.max.set(0);
        animator.stop();
        animPercentage = 0;
        renderOffsetX = 0;
        animationStarted = false;
        hasSearched = false;
        isLoading = false;
        contentHeight = 0;
        loadList();
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(renderOffsetX, 0, 0);

        float frameTime = Minecraft.getInstance().getFrameTime();
        angle += 6 * frameTime;
        if (angle > 360) {
            angle = 0;
        }
        double offsetX = Math.sin(Math.toRadians(angle)) * 5;
        double offsetY = Math.cos(Math.toRadians(angle)) * 5;

        if (!this.isLoading) {
            if (this.hasSearched && routes.isEmpty()) {
                GuiUtils.drawString(graphics, graphics.defaultFont(), width() / 2, height() / 2 + 15 - graphics.defaultFont().lineHeight - 10, noConnectionsText, DLColor.WHITE, ETextAlignment.CENTER, false);
                AllIcons.I_ACTIVE.render(graphics.graphics(), (int)(width() / 2 - 8), (int)(height() / 2 - 15 - graphics.defaultFont().lineHeight - 10));
            } else if (settings != null && this.displayRecentSearchQueries.get() && !this.hasSearched && !settings.recentSearchQueries.getValue().isEmpty()) {
                //GuiUtils.drawString(graphics, graphics.defaultFont(), 10, 10, txtRecentSearchQueries, DLColor.WHITE, ETextAlignment.LEFT, true);
                if (this.settings == null) {
                    GuiUtils.drawString(graphics, graphics.defaultFont(), width() / 2, height() / 2 + 15 - graphics.defaultFont().lineHeight, Constants.TEXT_LOADING, DLColor.WHITE, ETextAlignment.CENTER, false);
                    AllIcons.I_MTD_SCAN.render(graphics.graphics(), (int)(width() / 2 - 8 + offsetX), (int)(height() / 2 - 15 - graphics.defaultFont().lineHeight + offsetY));
                }
            } else if (!this.hasSearched) {
                GuiUtils.drawString(graphics, graphics.defaultFont(), width() / 2, height() / 2 + 15 - graphics.defaultFont().lineHeight, notSearchedText, DLColor.WHITE, ETextAlignment.CENTER, false);
                ModGuiIcons.INFO.render(graphics, (int)(width() / 2 - 8), (int)(height() / 2 - 15 - graphics.defaultFont().lineHeight));
            }
        } else {
            if (animationStarted) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GuiUtils.setTint(DLColor.of(animator.isRunning() ? (float)(1D - animPercentage) : 1f, 1f, 1f, 1f));
                CreateDynamicWidgets.renderShadow(graphics, width() / 2 - graphics.defaultFont().width(searchingText) / 2 - 10, (int)(height() / 2 - 25 - graphics.defaultFont().lineHeight), graphics.defaultFont().width(searchingText) + 20, 55);
                GuiUtils.drawString(graphics, graphics.defaultFont(), width() / 2, height() / 2 + 15 - graphics.defaultFont().lineHeight, searchingText, DLColor.of((float)(1D - animPercentage), 1f, 1f, 1f), ETextAlignment.CENTER, false);
                AllIcons.I_MTD_SCAN.render(graphics.graphics(), (int)(width() / 2 - 8 + offsetX), (int)(height() / 2 - 15 - graphics.defaultFont().lineHeight + offsetY));
            }
        }

        if (!isLoading && scrollbar.canScroll() && scrollbar.value.get() > 0) {
            GuiUtils.fillGradient(graphics, 0, 0, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.TOP);
        }
        if (!isLoading && scrollbar.canScroll() && scrollbar.value.get() < scrollbar.max.get()) {
            GuiUtils.fillGradient(graphics, 0, height() - 10, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.BOTTOM);
        }
        graphics.poseStack().popPose();
    }



    private static class Label extends DLGuiComponent {

        public static final Supplier<Integer> HEIGHT = () -> Minecraft.getInstance().font.lineHeight + 15;
        private final Component text;

        public Label(int x, int y, Component text) {
            super(x, y, 1, HEIGHT.get());
            this.text = text;
            this.inputConsumptionPolicy.set(a -> false);
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            GuiUtils.drawString(graphics, Minecraft.getInstance().font, 0, 10, text, DLColor.WHITE, ETextAlignment.LEFT, true);
        }
    }

}
