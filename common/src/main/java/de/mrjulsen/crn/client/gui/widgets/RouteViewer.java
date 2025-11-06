package de.mrjulsen.crn.client.gui.widgets;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.Animator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.storage.RecentSearchQueries.RecentSearchQuery;
import de.mrjulsen.crn.network.packets.pain.NavigatePacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.render.FlatButtonRenderer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.math.Size;
import net.fabricmc.fabric.mixin.object.builder.DefaultAttributeRegistryAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class RouteViewer extends DLGuiComponent {

    private final MutableComponent searchingText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.searching");
    private final MutableComponent noConnectionsText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.no_connections");
    private final MutableComponent notSearchedText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.not_searched");
    private final MutableComponent txtRecentSearchQueries = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.recently_searched").withStyle(ChatFormatting.BOLD);


    private final Screen parent;
    private final DLScrollBar scrollBar;
    private boolean shouldDisplayRecentSearchQueries;
    private UserSettings userSettings;
    private int contentHeight = 0;
    private int angle = 0;

    private final List<ClientRoute> routes = new ArrayList<>();
    private final Consumer<List<ClientRoute>> onUpdateRoutes;
    private DLButton closeBtn;

    // flags
    private boolean loadingRoutes = false;
    private boolean hasSearched = false;

    private final Animator animator = new Animator();
    private double animPercentage = 0;
    private double renderOffsetX = 0;
    private boolean animationStarted = false;

    public RouteViewer(Screen parent, int x, int y, int width, int height, DLScrollBar scrollBar, Consumer<List<ClientRoute>> onUpdateRoutes) {
        super(x, y, width, height);
        this.parent = parent;
        this.scrollBar = scrollBar;
        this.onUpdateRoutes = onUpdateRoutes;
        scrollBar.scrollerSize.set(0);
        scrollBar.screenSize.set(height());
        scrollBar.maxSize.set(Size.of(width, height));
        scrollBar.scrollSteps.set(10);
        scrollBar.addEventListener(DLScrollBar.ValueChangedEvent.class, (s, e) -> {
            setScrollOffsetY(e.value());
            return false;
        });

        setupList();
    }

    public void enableRecentlySearchedList(UserSettings settings) {
        this.shouldDisplayRecentSearchQueries = true;
        this.userSettings = settings;
        refresh(settings);
    }

    public Screen getParent() {
        return parent;
    }

    public List<ClientRoute> getRoutes() {
        return ImmutableList.copyOf(routes);
    }

    public void setRoutes(Collection<ClientRoute> routes, boolean closeCurrent) {
        if (closeCurrent) clear();
        if (routes != null) {
            this.routes.addAll(routes);
            DLUtils.doIfNotNull(onUpdateRoutes, x -> x.accept(getRoutes()));
            this.hasSearched = true;
        }
        setupList();
    }

    public void search(String from, String to, UserSettings settings, Consumer<RouteViewer> andThen) {
        this.loadingRoutes = true;
        this.userSettings = settings;
        userSettings.recentSearchQueries.getValue().add(new RecentSearchQuery(from, to));
        clear();
        
        userSettings.clientSave(() -> {
            animator.start(10, (poseStack, current, total, percentage) -> {
                this.animPercentage = Math.pow(1D - percentage, 4);
                this.renderOffsetX = -(50 * animPercentage);
            }, null, () -> {
                this.renderOffsetX = 0;

                ModNetworkManager.NAVIGATE.send(NetworkDirection.toServer(), new NavigatePacketData.Request(from, to, Minecraft.getInstance().player.getUUID()), (response) -> {
                    List<ClientRoute> routeList = response.getData();
                    animator.start(10, (poseStack, current, total, percentage) -> {
                        this.animPercentage = Math.pow(percentage, 4);
                        this.renderOffsetX = (50 * animPercentage);
                    }, null, () -> {
                        this.animationStarted = false;
                        this.animPercentage = 0;
                        this.renderOffsetX = 0;
                        setRoutes(routeList, true);
                        this.loadingRoutes = false;
                        DLUtils.doIfNotNull(andThen, x -> x.accept(this));            
                        if (this.routes.isEmpty()) {
                            closeBtn = addComponent(new DLButton(width() / 2 - 18 / 2, height() / 2 + 20));
                            closeBtn.icon.set(ModGuiIcons.X.getAsSprite(16, 16));
                            closeBtn.text.set(TextUtils.empty());
                            closeBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                                this.hasSearched = false;
                                refresh(userSettings);
                                return false;
                            });
                        }
                    });
                }, () -> {});
            });
            this.animationStarted = true;
        });        
    }

    @Override
    public void clearComponents() {
        super.clearComponents();
        closeBtn = null;
    }

    @Override
    public void tick() {        
        super.tick();
        animator.tick();
    }

    public void refresh(UserSettings settings) {
        if (this.loadingRoutes) {
            return;
        }
        this.userSettings = settings;
        clearComponents();
        setupList();
    }

    private void setupList() {
        clearComponents();
        this.contentHeight = 0;
        if (this.shouldDisplayRecentSearchQueries && !this.hasSearched && !this.loadingRoutes && this.routes.isEmpty() && userSettings != null) {
            contentHeight = 20 + Minecraft.getInstance().font.lineHeight;
            for (RecentSearchQuery query : userSettings.recentSearchQueries.getValue().getAll()) {
                RecentSearchQueryButton btn = addComponent(new RecentSearchQueryButton(this, x() + 10, y() + contentHeight, width() - 20, query));
                contentHeight += btn.height();
            }
            contentHeight += 10;
        } else if (!this.routes.isEmpty()) {            
            contentHeight = 5;
            for (int i = 0; i < routes.size(); i++) {
                RouteWidget widget = new RouteWidget(this, routes.get(i), x() + 10, y() + contentHeight);
                addComponent(widget);
                contentHeight += (RouteWidget.HEIGHT + 3);
            }
            contentHeight += 2;
        }
        scrollBar.maxSize.set(Size.of(width(), contentHeight));
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        animator.renderMainLayer(graphics, mouseX, mouseY, renderBounds);
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(renderOffsetX, 0, 0);
        
        float frameTime = Minecraft.getInstance().getFrameTime();
        angle += 6 * frameTime;
        if (angle > 360) {
            angle = 0;
        }
        double offsetX = Math.sin(Math.toRadians(angle)) * 5;
        double offsetY = Math.cos(Math.toRadians(angle)) * 5;

        if (!this.loadingRoutes) {
            if (this.hasSearched && routes.isEmpty()) {
                GuiUtils.drawString(graphics, graphics.defaultFont(), width() / 2, height() / 2 + 15 - graphics.defaultFont().lineHeight - 10, noConnectionsText, DLColor.WHITE, ETextAlignment.CENTER, false);
                AllIcons.I_ACTIVE.render(graphics.graphics(), (int)(width() / 2 - 8), (int)(height() / 2 - 15 - graphics.defaultFont().lineHeight - 10));
            } else if (this.shouldDisplayRecentSearchQueries && !this.hasSearched && !userSettings.recentSearchQueries.getValue().isEmpty()) {
                GuiUtils.drawString(graphics, graphics.defaultFont(), 10, 10, txtRecentSearchQueries, DLColor.WHITE, ETextAlignment.LEFT, true);
                if (this.userSettings == null) {
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
                GuiUtils.setTint(DLColor.of(1f, 1f, 1f, animator.isRunning() ? (float)(1D - animPercentage) : 1f));
                CreateDynamicWidgets.renderShadow(graphics, width() / 2 - graphics.defaultFont().width(searchingText) / 2 - 10, (int)(height() / 2 - 25 - graphics.defaultFont().lineHeight), graphics.defaultFont().width(searchingText) + 20, 55);
                GuiUtils.drawString(graphics, graphics.defaultFont(), width() / 2, height() / 2 + 15 - graphics.defaultFont().lineHeight, searchingText, DLColor.of((float)(1D - animPercentage), 1f, 1f, 1f), ETextAlignment.CENTER, false);
                AllIcons.I_MTD_SCAN.render(graphics.graphics(), (int)(width() / 2 - 8 + offsetX), (int)(height() / 2 - 15 - graphics.defaultFont().lineHeight + offsetY));
            }
        }
        
        if (scrollBar.value.get() > 0) {
            GuiUtils.fillGradient(graphics, 0, 0, width(), 10, DLColor.fromInt(0x77000000), DLColor.BLACK, EAlign.TOP);
        }
        if (scrollBar.value.get() < scrollBar.max.get()) {
            GuiUtils.fillGradient(graphics, 0, height() - 10, width(), 10, DLColor.fromInt(0x00000000), DLColor.WHITE, EAlign.BOTTOM);
        }
        graphics.poseStack().popPose();
    }

    @Override
    public void renderFrontLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {        
        if (closeBtn != null) {
            if (closeBtn.isSelected()) {
                GuiUtils.drawTooltip(graphics, null, (int)mouseX, (int)mouseY, List.of(TextUtils.TEXT_CLOSE), 200);
            }
        }
    }

    public void clear() {
        this.routes.forEach(ClientRoute::close);
        this.routes.clear();
        clearComponents();
    }



    private static class RecentSearchQueryButton extends DLButton {

        private final Component text;
        private final Component subText;

        public RecentSearchQueryButton(RouteViewer viewer, int x, int y, int width, RecentSearchQuery query) {
            super(x, y, width, 12);
            addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                viewer.search(query.getStartStation(), query.getDestinationStation(), viewer.userSettings, null);
                return false;
            });
            this.subText = TextUtils.truncateWithEllipsis(Minecraft.getInstance().font, TextUtils.text(DragonLib.DATE_FORMAT.format(query.getCreationTime())).withStyle(ChatFormatting.GRAY), (int)((float)(width - 6) / 0.75f));
            this.text = TextUtils.truncateWithEllipsis(Minecraft.getInstance().font, TextUtils.text(String.format("%s \u2192 %s", query.getStartStation(), query.getDestinationStation())), (int)((float)(width - 6 - Minecraft.getInstance().font.width(subText) - 5) / 0.75f));
            componentRenderer.set(FlatButtonRenderer.INSTANCE);
            backgroundTint.set(DLColor.TRANSPARENT);
            
            /*
            setMenu(new DLContextMenu(() -> GuiAreaDefinition.of(this), () -> new DLContextMenuItem.Builder()
                .add(new ContextMenuItemData(Constants.TEXT_SEARCH, Sprite.empty(), true, (b) -> onPress.onPress(b), null))
                .addSeparator()
                .add(new ContextMenuItemData(Constants.TEXT_REMOVE, Sprite.empty(), true, (b) -> {
                    DLUtils.doIfNotNull(viewer.userSettings, a -> {
                        a.recentSearchQueries.getValue().remove(query);
                        a.clientSave(() -> {
                            viewer.hasSearched = false;
                            viewer.refresh(viewer.userSettings);
                        });
                    });
                }, null))
            ));
            */
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(x() + 3, y() + 3, 0);
            graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
            GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 0, text, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
            graphics.poseStack().popPose();
            
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(x() + width() - 3, y() + 3, 0);
            graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
            GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 0, subText, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.RIGHT, false);
            graphics.poseStack().popPose();
            
        }
        
    }



    public boolean isLoading() {
        return loadingRoutes;
    }
}
