package de.mrjulsen.crn.client.gui.screen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.utility.Components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.overlay.RouteDetailsOverlay;
import de.mrjulsen.crn.client.gui.overlay.OverlayPosition;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIndicator;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.registry.ModItems;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.client.util.WidgetsCollection;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class RouteOverlaySettingsScreen extends DLScreen {

    private static final ResourceLocation GUI = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/route_overlay_settings.png");
    private static final int GUI_WIDTH = 213;
    private static final int GUI_HEIGHT = 79;
    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private final Font shadowlessFont;
	private final ItemStack renderedItem = new ItemStack(ModItems.NAVIGATOR.get());
    private int guiLeft, guiTop;

    private final RouteDetailsOverlay overlay;

    private DLCreateIconButton backButton;
    private DLCreateIconButton detailsButton;
    private IconButton removeOverlayButton;
    private DLCreateIconButton notificationsButton;
    private DLCreateIndicator notificationsIndicator;
    private ScrollInput scaleInput;
    private Component scaleLabel;

    private final Map<IconButton, Pair<Component, Component>> buttonTooltips = new LinkedHashMap<>();

    private final WidgetsCollection positionButtons = new WidgetsCollection();
    private final WidgetsCollection buttons = new WidgetsCollection();

    private static final MutableComponent narratorOn = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.narrator.on");
    private static final MutableComponent narratorOff = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.narrator.off");    
    private static final MutableComponent notificationsOn = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.notifications.on");
    private static final MutableComponent notificationsOff = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.notifications.off");
    private static final MutableComponent textScale = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.scale");
    private static final MutableComponent textShowDetails = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.show_details");
    private static final MutableComponent textUnpin = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.unpin");

    private static final MutableComponent textNarrator = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.narrator");
    private static final MutableComponent textNarratorDescription = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.narrator.description").withStyle(ChatFormatting.GRAY);
    private static final MutableComponent textNotifications = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.notifications");
    private static final MutableComponent textNotificationsDescription = TextUtils.translate("gui.createrailwaysnavigator.route_overlay_settings.notifications.description").withStyle(ChatFormatting.GRAY);
    
    @SuppressWarnings("resource")
    public RouteOverlaySettingsScreen(RouteDetailsOverlay overlay) {
        super(TextUtils.translate("gui.createrailwaysnavigator.overlay_settings.title"));
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font);
        this.overlay = overlay;
    }

    @Override
    public void onClose() {        
        ModClientConfig.SPEC.save();
        ModClientConfig.SPEC.afterReload();
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;
        positionButtons.clear();
        buttons.clear();

        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 180, guiTop + 55, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
        backButton.withCallback(() -> {
            onClose();
        });
        buttons.add(backButton);

        detailsButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 7, guiTop + 55, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_VIEW_SCHEDULE));
        detailsButton.withCallback(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new RouteDetailsScreen(null, overlay.getRoute()));
        });
        detailsButton.setToolTip(textShowDetails);
        buttons.add(detailsButton);

        removeOverlayButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 27, guiTop + 55, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_CONFIG_DISCARD));
        removeOverlayButton.withCallback(() -> {
            InstanceManager.removeRouteOverlay();
            onClose();
        });
        removeOverlayButton.setToolTip(textUnpin);
        buttons.add(removeOverlayButton);

        // Position buttons
        OverlayPosition[] positions = OverlayPosition.values();
        for (int i = 0; i < positions.length; i++) {
            final OverlayPosition pos = positions[i];            
            final IconButton remOverlayButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 123 + DEFAULT_ICON_BUTTON_WIDTH * i, guiTop + 23, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, pos.getIcon().getAsCreateIcon()));
            remOverlayButton.withCallback(() -> {
                positionButtons.performForEach(x -> x.active = true);
                remOverlayButton.active = false;

                ModClientConfig.ROUTE_OVERLAY_POSITION.set(pos);
            });
            remOverlayButton.setToolTip(TextUtils.translate(pos.getEnumTranslationKey(CreateRailwaysNavigator.MOD_ID)));
            remOverlayButton.getToolTip().add(TextUtils.translate(pos.getValueInfoTranslationKey(CreateRailwaysNavigator.MOD_ID)).withStyle(ChatFormatting.GRAY));
            remOverlayButton.active = pos != ModClientConfig.ROUTE_OVERLAY_POSITION.get();

            removeOverlayButton = remOverlayButton;
            positionButtons.add(remOverlayButton);
            buttons.add(remOverlayButton);
        }

        notificationsButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 20, guiTop + 26, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.INFO.getAsCreateIcon()));
        notificationsButton.withCallback(() -> {
            overlay.getRoute().setShowNotifications(!overlay.getRoute().shouldShowNotifications());
        });
        buttons.add(notificationsButton);
        buttonTooltips.put(notificationsButton, Pair.of(textNotifications, textNotificationsDescription));
        notificationsIndicator = this.addRenderableWidget(new DLCreateIndicator(guiLeft + 20, guiTop + 20, Components.immutableEmpty()));

        // scale
        scaleInput = addRenderableWidget(new ScrollInput(guiLeft + 63, guiTop + 23, 43, 18)
            .withRange((int)(ModClientConfig.MIN_SCALE * 100), (int)(ModClientConfig.MAX_SCALE * 100) + 1)
            .withStepFunction(x -> 5 * (x.shift ? 5 : 1))
            .titled(textScale)
            .calling((i) -> {
                double val = (double)i / 100.0d;
                ModClientConfig.OVERLAY_SCALE.set((double)i / 100.0d);
                scaleLabel = TextUtils.text(String.format("%.2f", val) + "x");
            })
            .setState((int)(ModClientConfig.OVERLAY_SCALE.get() * 100)));
            scaleInput.onChanged();
        buttons.add(scaleInput);        
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        notificationsIndicator.state = overlay.getRoute().shouldShowNotifications() ? State.ON : State.OFF;

        buttons.performForEachOfType(IconButton.class, x -> {
            if (!buttonTooltips.containsKey(x)) {
                return;
            }

            x.setToolTip(buttonTooltips.get(x).getFirst());
            x.getToolTip().add(TooltipHelper.holdShift(Palette.YELLOW, hasShiftDown()));

            if (hasShiftDown()) {
                x.getToolTip().add(buttonTooltips.get(x).getSecond());
            }
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double pDelta) {

        if (scaleInput.mouseScrolled(mouseX, mouseY, pDelta)) {
            return true;
        }
        
        if (super.mouseScrolled(mouseX, mouseY, pDelta)) {
            return true;
        }

        return false;
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderScreenBackground(graphics);
        GuiUtils.drawTexture(GUI, graphics, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        GuiUtils.drawString(graphics, shadowlessFont, guiLeft + 6, guiTop + 4, title, DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.LEFT, false);

        GuiGameElement.of(renderedItem).<GuiGameElement
			.GuiRenderBuilder>at(guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT - 48, -200)
			.scale(5)
			.render(graphics.graphics());

        CreateDynamicWidgets.renderTextBox(graphics, guiLeft + 63, guiTop + 23, 43);

        GuiUtils.drawString(graphics, font, guiLeft + 67, guiTop + 28, scaleLabel, 0xFFFFFF, EAlignment.LEFT, true);

        super.renderMainLayer(graphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderFrontLayer(graphics, pMouseX, pMouseY, pPartialTick);
        buttons.performForEach(widget -> {
            if (widget instanceof AbstractSimiWidget simiWidget && simiWidget instanceof IDragonLibWidget dlw && dlw.isMouseSelected()) {
				List<Component> tooltip = simiWidget.getToolTip();
				if (tooltip.isEmpty())
					return;
				int ttx = simiWidget.lockedTooltipX == -1 ? pMouseX : simiWidget.lockedTooltipX + simiWidget.getX();
				int tty = simiWidget.lockedTooltipY == -1 ? pMouseY : simiWidget.lockedTooltipY + simiWidget.getY();
				graphics.graphics().renderComponentTooltip(font, tooltip, ttx, tty);
			}
        });
    }
}