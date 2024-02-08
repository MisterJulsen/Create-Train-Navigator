package de.mrjulsen.crn.client.gui.screen;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.ControlCollection;
import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import de.mrjulsen.crn.client.gui.IForegroundRendering;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.ModDestinationSuggestions;
import de.mrjulsen.crn.client.gui.widgets.RouteEntryOverviewWidget;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.NavigationRequestPacket;
import de.mrjulsen.crn.network.packets.cts.NearestStationRequestPacket;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils.TimeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class NavigatorScreen extends Screen implements IForegroundRendering {

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/navigator.png");
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private static final int ENTRIES_START_Y_OFFSET = 10;  
    private static final int ENTRY_SPACING = 4;
    
    private final int AREA_X = 16;
    private final int AREA_Y = 67;        
    private final int AREA_W = 220;
    private final int AREA_H = 143;

    private int guiLeft, guiTop;
    private int angle = 0;    
    
    // Controls
    private IconButton locationButton;
    private IconButton searchButton;   
    private IconButton goToTopButton;
    private IconButton globalSettingsButton;  
    private IconButton searchSettingsButton;  
	private EditBox fromBox;
	private EditBox toBox;    
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);   
	private ModDestinationSuggestions destinationSuggestions;
    private GuiAreaDefinition switchButtonsArea; 
    private final ControlCollection routesCollection = new ControlCollection(); 

    // Data
    private SimpleRoute[] routes;
    private String stationFrom;
    private String stationTo;
    private int lastRefreshedTime;
    private final NavigatorScreen instance;
    private final Level level;
    private final Font shadowlessFont;

    // var
    private boolean isLoadingRoutes = false;
    private boolean generatingRouteEntries = false;

    // Tooltips
    private final TranslatableComponent searchingText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.searching");
    private final TranslatableComponent noConnectionsText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.no_connections");
    private final TranslatableComponent errorTitle = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.error_title");
    private final TranslatableComponent startEndEqualText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.start_end_equal");
    private final TranslatableComponent startEndNullText = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.start_end_null");

    private final TranslatableComponent tooltipSearch = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.search.tooltip");
    private final TranslatableComponent tooltipLocation = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.location.tooltip");
    private final TranslatableComponent tooltipSwitch = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.switch.tooltip");
    private final TranslatableComponent tooltipGlobalSettings = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.global_settings.tooltip");
    private final TranslatableComponent tooltipSearchSettings = new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.search_settings.tooltip");

    @SuppressWarnings("resource")
    public NavigatorScreen(Level level) {
        super(new TranslatableComponent("gui." + ModMain.MOD_ID + ".navigator.title"));
        this.instance = this;
        this.level = level;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font); 
    }

    private void generateRouteEntries() {
        generatingRouteEntries = true;
        routesCollection.components.clear();

        if (routes != null && routes.length > 0) {
            for (int i = 0; i < routes.length; i++) {
                SimpleRoute route = routes[i];
                AbstractWidget w = new RouteEntryOverviewWidget(instance, level, lastRefreshedTime, guiLeft + 26, guiTop + 67 + ENTRIES_START_Y_OFFSET + (i * (RouteEntryOverviewWidget.HEIGHT + ENTRY_SPACING)), route, (btn) -> {});
                routesCollection.components.add(w);
            } 
        }         
        generatingRouteEntries = false;
    }   

    private void clearSuggestions() {
        if (destinationSuggestions != null) {
            destinationSuggestions.getEditBox().setSuggestion("");
        }
        destinationSuggestions = null;
    }

    private void setLastRefreshedTime() {
        lastRefreshedTime = (int)(level.getDayTime()/*% Constants.TICKS_PER_DAY*/);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void switchButtonClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        String fromInput = fromBox.getValue();
        String toInput = toBox.getValue();

        fromBox.setValue(toInput);
        toBox.setValue(fromInput);
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;

        switchButtonsArea = new GuiAreaDefinition(guiLeft + 190, guiTop + 34, 11, 12);

        locationButton = this.addRenderableWidget(new IconButton(guiLeft + 208, guiTop + 20, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.POSITION.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                long id = InstanceManager.registerClientNearestStationResponseAction((result) -> {
                    if (result.aliasName.isPresent()) {
                        fromBox.setValue(result.aliasName.get().getAliasName().get());
                    }
                });
                NetworkManager.sendToServer(new NearestStationRequestPacket(id, minecraft.player.position()));
            }
        });
        searchButton = this.addRenderableWidget(new IconButton(guiLeft + 208, guiTop + 42, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_MTD_SCAN) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                
                if (stationFrom == null || stationTo == null) {
                    Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, errorTitle, startEndNullText));
                    return;
                }

                if (stationFrom.equals(stationTo)) {
                    Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, errorTitle, startEndEqualText));
                    return;
                }

                isLoadingRoutes = true;

                long id = InstanceManager.registerClientNavigationResponseAction((routes, data) -> {
                    instance.routes = routes.toArray(SimpleRoute[]::new);
                    setLastRefreshedTime();
                    generateRouteEntries();
                    isLoadingRoutes = false;
                });
                scroll.chase(0, 0.7f, Chaser.EXP);
                NetworkManager.sendToServer(new NavigationRequestPacket(id, stationFrom, stationTo));
               
            }
        });

        fromBox = new EditBox(font, guiLeft + 50, guiTop + 25, 157, 12, new TextComponent(""));
		fromBox.setBordered(false);
		fromBox.setMaxLength(25);
		fromBox.setTextColor(0xFFFFFF);
        fromBox.setValue(stationFrom);
        fromBox.setResponder(x -> {
            stationFrom = x;
            updateEditorSubwidgets(fromBox);
        });
		addRenderableWidget(fromBox);

        toBox = new EditBox(font, guiLeft + 50, guiTop + 47, 157, 12, new TextComponent(""));
		toBox.setBordered(false);
		toBox.setMaxLength(25);
		toBox.setTextColor(0xFFFFFF);
        toBox.setValue(stationTo);
        toBox.setResponder(x -> {
            stationTo = x;
            updateEditorSubwidgets(toBox);
        });
		addRenderableWidget(toBox);

        goToTopButton = this.addRenderableWidget(new IconButton(guiLeft + GUI_WIDTH - 10, guiTop + AREA_Y, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_PRIORITY_VERY_HIGH) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                scroll.chase(0, 0.7f, Chaser.EXP);
            }
        });

        // Global Options Button
        if (minecraft.player.hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_PERMISSION_LEVEL.get())) {
            globalSettingsButton = this.addRenderableWidget(new IconButton(guiLeft + 43, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.SETTINGS.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    minecraft.setScreen(new GlobalSettingsScreen(level, instance));
                }
            });
        }

        searchSettingsButton = this.addRenderableWidget(new IconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.FILTER.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                minecraft.setScreen(new SearchSettingsScreen(level, instance));
            }
        });

        this.addRenderableWidget(new IconButton(guiLeft + GUI_WIDTH - 42, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_MTD_CLOSE) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                onClose();
            }
        });

        generateRouteEntries();
    }

    @Override
    public void tick() {
        angle += 6;
        if (angle > 360) {
            angle = 0;
        }
        
		scroll.tickChaser();
        
		if (destinationSuggestions != null) {
            destinationSuggestions.tick();

            if (!toBox.canConsumeInput() && !fromBox.canConsumeInput()) {
                clearSuggestions();
            }
        }

        this.goToTopButton.visible = routes != null && scroll.getValue() > 0;

        searchButton.active = !isLoadingRoutes;

        super.tick();
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) { 
        
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderBackground(pPoseStack);
        RenderSystem.setShaderTexture(0, GUI);
        blit(pPoseStack, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        for (Widget widget : this.renderables)
            widget.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        drawString(pPoseStack, shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + Constants.TIME_SHIFT) % Constants.TICKS_PER_DAY), TimeFormat.HOURS_24);
        drawString(pPoseStack, shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);

        if (!isLoadingRoutes && !generatingRouteEntries) {
            if (routes == null || routes.length <= 0) {
                drawCenteredString(pPoseStack, font, noConnectionsText, guiLeft + GUI_WIDTH / 2, guiTop + 32 + GUI_HEIGHT / 2, 0xFFFFFF);
                AllIcons.I_ACTIVE.render(pPoseStack, (int)(guiLeft + GUI_WIDTH / 2 - 8), (int)(guiTop + GUI_HEIGHT / 2));
            } else {
                UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
                ModGuiUtils.startStencil(pPoseStack, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
                pPoseStack.pushPose();
                pPoseStack.translate(0, scrollOffset, 0);

                int start = (int)(Math.abs(scrollOffset + ENTRIES_START_Y_OFFSET) / (ENTRY_SPACING + RouteEntryOverviewWidget.HEIGHT));
                int end = Math.min(routesCollection.components.size(), start + 2 + (int)(AREA_H / (ENTRY_SPACING + RouteEntryOverviewWidget.HEIGHT)));
                for (int i = start; i < end; i++) {
                    routesCollection.components.get(i).render(pPoseStack, (int)(pMouseX), (int)(pMouseY - scrollOffset), pPartialTick);
                }

                pPoseStack.popPose();
                ModGuiUtils.endStencil();
                
                net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + 10,
                0x77000000, 0x00000000);
                net.minecraftforge.client.gui.GuiUtils.drawGradientRect(pPoseStack.last().pose(), 200, guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + AREA_H,
                0x00000000, 0x77000000);

                UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, minecraft.getMainRenderTarget());

                // Scrollbar
                double maxHeight = ENTRIES_START_Y_OFFSET + routes.length * (RouteEntryOverviewWidget.HEIGHT + 4) + ENTRIES_START_Y_OFFSET;
                double aH = AREA_H + 1;
                if (aH / maxHeight < 1) {
                    int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
                    int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

                    fill(pPoseStack, guiLeft + AREA_X + AREA_W - 3, startY, guiLeft + AREA_X + AREA_W, startY + scrollerHeight, 0x7FFFFFFF);
                }
            }
        } else {            
            double offsetX = Math.sin(Math.toRadians(angle)) * 5;
            double offsetY = Math.cos(Math.toRadians(angle)) * 5; 
            
            drawCenteredString(pPoseStack, font, searchingText, guiLeft + GUI_WIDTH / 2, guiTop + 32 + GUI_HEIGHT / 2, 0xFFFFFF);
            AllIcons.I_MTD_SCAN.render(pPoseStack, (int)(guiLeft + GUI_WIDTH / 2 - 8 + offsetX), (int)(guiTop + GUI_HEIGHT / 2 + offsetY));
        }

        if (switchButtonsArea.isInBounds(pMouseX, pMouseY)) {
            fill(pPoseStack, switchButtonsArea.getLeft(), switchButtonsArea.getTop(), switchButtonsArea.getRight(), switchButtonsArea.getBottom(), 0x3FFFFFFF);
        }

        renderForeground(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
	public void renderForeground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		if (destinationSuggestions != null) {
			matrixStack.pushPose();
			matrixStack.translate(0, 0, 500);
			destinationSuggestions.render(matrixStack, mouseX, mouseY);
			matrixStack.popPose();
		}
        
        ModGuiUtils.renderTooltip(this, searchButton, List.of(tooltipSearch.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);
        ModGuiUtils.renderTooltip(this, locationButton, List.of(tooltipLocation.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);
        ModGuiUtils.renderTooltip(this, switchButtonsArea, List.of(tooltipSwitch.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);
        ModGuiUtils.renderTooltip(this, goToTopButton, List.of(Constants.TOOLTIP_GO_TO_TOP.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);
        ModGuiUtils.renderTooltip(this, searchSettingsButton, List.of(tooltipSearchSettings.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);

        if (globalSettingsButton != null) {
            ModGuiUtils.renderTooltip(this, globalSettingsButton, List.of(tooltipGlobalSettings.getVisualOrderText()), matrixStack, mouseX, mouseY, 0, 0);
        }
    }

    protected void updateEditorSubwidgets(EditBox field) {
        clearSuggestions();

		destinationSuggestions = new ModDestinationSuggestions(this.minecraft, this, field, this.font, getViableStations(field), field.getHeight() + 2 + field.y);
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
	}

    private List<TrainStationAlias> getViableStations(EditBox field) {
        return ClientTrainStationSnapshot.getInstance().getAllTrainStations().stream()
            .map(x -> GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(x))
            .distinct()
            .filter(x -> !GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(x))
            .sorted((a, b) -> a.getAliasName().get().compareTo(b.getAliasName().get()))
            .toList();
	}

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (destinationSuggestions != null && destinationSuggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
			return true;

		float scrollOffset = scroll.getValue();

        if (switchButtonsArea.isInBounds(pMouseX, pMouseY)) {
            switchButtonClick();
        }

        if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
            routesCollection.performForEach(x -> x.mouseClicked(pMouseX, pMouseY + scrollOffset, pButton));
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (destinationSuggestions != null && destinationSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers))
			return true;

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(pMouseX, pMouseY, Mth.clamp(pDelta, -1.0D, 1.0D)))
			return true;

		float chaseTarget = scroll.getChaseTarget();
		float max = -AREA_H;
        if (routes != null && routes.length > 0) {
            max += ENTRIES_START_Y_OFFSET + routes.length * (RouteEntryOverviewWidget.HEIGHT + 4) + ENTRIES_START_Y_OFFSET;
        }

		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = Mth.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }
    
}
