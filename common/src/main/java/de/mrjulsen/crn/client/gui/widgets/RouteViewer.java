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
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.registry.ModAccessorTypes.NavigationData;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem.ContextMenuItemData;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLScrollableWidgetContainer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class RouteViewer extends DLScrollableWidgetContainer implements Closeable {

    private final MutableComponent searchingText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.searching");
    private final MutableComponent noConnectionsText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.no_connections");
    private final MutableComponent notSearchedText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.not_searched");
    private final MutableComponent txtRecentSearchQueries = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".navigator.recently_searched").withStyle(ChatFormatting.BOLD);


    private final Screen parent;
    private final DLAbstractScrollBar<?> scrollBar;
    private boolean shouldDisplayRecentSearchQueries;
    private UserSettings userSettings;
    private int contentHeight = 0;
    private int angle = 0;

    private final List<ClientRoute> routes = new ArrayList<>();
    private final Consumer<List<ClientRoute>> onUpdateRoutes;
    private DLIconButton closeBtn;

    // flags
    private boolean loadingRoutes = false;
    private boolean hasSearched = false;

    private final Animator animator = new Animator();
    private double animPercentage = 0;
    private double renderOffsetX = 0;
    private boolean animationStarted = false;

    public RouteViewer(Screen parent, int x, int y, int width, int height, DLAbstractScrollBar<?> scrollBar, Consumer<List<ClientRoute>> onUpdateRoutes) {
        super(x, y, width, height);
        this.parent = parent;
        this.scrollBar = scrollBar;
        this.onUpdateRoutes = onUpdateRoutes;
        scrollBar.setAutoScrollerSize(true);
        scrollBar.setScreenSize(height());
        scrollBar.setMaxScroll(0);
        scrollBar.withOnValueChanged((sb) -> setYScrollOffset(sb.getScrollValue()));
        scrollBar.setStepSize(10);

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
                DataAccessor.getFromServer(new NavigationData(from, to, Minecraft.getInstance().player.getUUID()), ModAccessorTypes.NAVIGATE, (routeList) -> {   
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
                            closeBtn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.X.getAsSprite(16, 16), x() + width() / 2 - DLIconButton.DEFAULT_BUTTON_WIDTH / 2, y() + height() / 2 + 20, TextUtils.empty(),
                            (btn) -> {
                                this.hasSearched = false;
                                refresh(userSettings);
                            }));
                            closeBtn.setBackColor(0);
                        }
                    });
                });
            });
            this.animationStarted = true;
        });        
    }

    @Override
    public void clearWidgets() {
        super.clearWidgets();
        closeBtn = null;;
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
        clearWidgets();
        setupList();
    }

    private void setupList() {
        clearWidgets();
        this.contentHeight = 0;
        if (this.shouldDisplayRecentSearchQueries && !this.hasSearched && !this.loadingRoutes && this.routes.isEmpty() && userSettings != null) {
            contentHeight = 20 + Minecraft.getInstance().font.lineHeight;
            for (RecentSearchQuery query : userSettings.recentSearchQueries.getValue().getAll()) {
                RecentSearchQueryButton btn = addRenderableWidget(new RecentSearchQueryButton(this, x() + 10, y() + contentHeight, width() - 20, query));
                contentHeight += btn.height();
            }
            contentHeight += 10;
        } else if (!this.routes.isEmpty()) {            
            contentHeight = 5;
            for (int i = 0; i < routes.size(); i++) {
                RouteWidget widget = new RouteWidget(this, routes.get(i), x() + 10, y() + contentHeight);
                addRenderableWidget(widget);
                contentHeight += (RouteWidget.HEIGHT + 3);
            }
            contentHeight += 2;
        }
        scrollBar.setMaxScroll(contentHeight);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        animator.renderMainLayer(graphics, mouseX, mouseY, partialTicks);  

        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(renderOffsetX, 0, 0);
        
        partialTicks = Minecraft.getInstance().getFrameTime();
        angle += 6 * partialTicks;
        if (angle > 360) {
            angle = 0;
        }
        double offsetX = Math.sin(Math.toRadians(angle)) * 5;
        double offsetY = Math.cos(Math.toRadians(angle)) * 5;

        if (!this.loadingRoutes) {
            if (this.hasSearched && routes.isEmpty()) {
                GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 + 15 - font.lineHeight - 10, noConnectionsText, 0xFFFFFF, EAlignment.CENTER, false);
                AllIcons.I_ACTIVE.render(graphics.poseStack(), (int)(x() + width() / 2 - 8), (int)(y() + height() / 2 - 15 - font.lineHeight - 10));
            } else if (this.shouldDisplayRecentSearchQueries && !this.hasSearched && !userSettings.recentSearchQueries.getValue().isEmpty()) {
                GuiUtils.drawString(graphics, font, x() + 10, y() + 10, txtRecentSearchQueries, 0xFFFFFF, EAlignment.LEFT, true);
                if (this.userSettings == null) {
                    GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 + 15 - font.lineHeight, Constants.TEXT_LOADING, 0xFFFFFF, EAlignment.CENTER, false);
                    AllIcons.I_MTD_SCAN.render(graphics.poseStack(), (int)(x() + width() / 2 - 8 + offsetX), (int)(y() + height() / 2 - 15 - font.lineHeight + offsetY));
                }
            } else if (!this.hasSearched) {                
                GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 + 15 - font.lineHeight, notSearchedText, 0xFFFFFF, EAlignment.CENTER, false);
                ModGuiIcons.INFO.render(graphics, (int)(x() + width() / 2 - 8), (int)(y() + height() / 2 - 15 - font.lineHeight));
            }
        } else {
            if (animationStarted) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GuiUtils.setTint(1f, 1f, 1f, animator.isRunning() ? (float)(1D - animPercentage) : 1f);
                CreateDynamicWidgets.renderShadow(graphics, x() + width() / 2 - font.width(searchingText) / 2 - 10, (int)(y() + height() / 2 - 25 - font.lineHeight), font.width(searchingText) + 20, 55);
                GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 + 15 - font.lineHeight, searchingText, ((int)(255F * (float)(1D - animPercentage)) << 24) | 0xFFFFFF, EAlignment.CENTER, false);
                AllIcons.I_MTD_SCAN.render(graphics.poseStack(), (int)(x() + width() / 2 - 8 + offsetX), (int)(y() + height() / 2 - 15 - font.lineHeight + offsetY));
            }
        }
        
        if (scrollBar.getScrollValue() > 0) {
            GuiUtils.fillGradient(graphics, x(), y(), 0, width(), 10, 0x77000000, 0x00000000);
        }
        if (scrollBar.getScrollValue() < scrollBar.getMaxScroll()) {
            GuiUtils.fillGradient(graphics, x(), y() + height() - 10, 0, width(), 10, 0x00000000, 0x77000000);
        }
        graphics.poseStack().popPose();
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);
        if (closeBtn != null) {
            GuiUtils.renderTooltip(parent, closeBtn, List.of(DragonLib.TEXT_CLOSE), 200, graphics, mouseX, mouseY);
        }
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }

    public void clear() {
        this.routes.forEach(ClientRoute::close);
        this.routes.clear();
        clearWidgets();
    }



    private static class RecentSearchQueryButton extends DLButton {

        private final Component text;
        private final Component subText;

        public RecentSearchQueryButton(RouteViewer viewer, int x, int y, int width, RecentSearchQuery query) {
            super(x, y, width, 12, TextUtils.empty(),
            (btn) -> {
                viewer.search(query.getStartStation(), query.getDestinationStation(), viewer.userSettings, null);
            });
            this.subText = GuiUtils.ellipsisString(Minecraft.getInstance().font, TextUtils.text(DragonLib.DATE_FORMAT.format(query.getCreationTime())).withStyle(ChatFormatting.GRAY), (int)((float)(width - 6) / 0.75f));
            this.text = GuiUtils.ellipsisString(Minecraft.getInstance().font, TextUtils.text(String.format("%s \u2192 %s", query.getStartStation(), query.getDestinationStation())), (int)((float)(width - 6 - Minecraft.getInstance().font.width(subText) - 5) / 0.75f));
            
            setRenderStyle(AreaStyle.FLAT);
            setBackColor(0);
            
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
        }

        @Override
        public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderMainLayer(graphics, mouseX, mouseY, partialTick);
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(x() + 3, y() + 3, 0);
            graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
            GuiUtils.drawString(graphics, font, 0, 0, text, DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
            graphics.poseStack().popPose();
            
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(x() + width() - 3, y() + 3, 0);
            graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
            GuiUtils.drawString(graphics, font, 0, 0, subText, DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.RIGHT, false);
            graphics.poseStack().popPose();
            
        }
        
    }



    public boolean isLoading() {
        return loadingRoutes;
    }
}
