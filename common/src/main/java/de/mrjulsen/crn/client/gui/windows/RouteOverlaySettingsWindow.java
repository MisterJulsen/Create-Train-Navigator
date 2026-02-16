package de.mrjulsen.crn.client.gui.windows;

import java.util.List;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.Indicator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.overlay.RouteDetailsOverlay;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateIndicator;
import de.mrjulsen.crn.client.gui.widgets.create.CreateScrollNumberInput;
import de.mrjulsen.crn.client.gui.overlay.OverlayPosition;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.registry.ModItems;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLNumberPicker;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.INumberFormatAdapter;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class RouteOverlaySettingsWindow extends DLWindow {

    private static final int GUI_WIDTH = 205;
    private static final int GUI_HEIGHT = 79;
	private final ItemStack renderedItem = new ItemStack(ModItems.NAVIGATOR.get());

    private CreateButton backButton;
    private CreateButton detailsButton;
    private CreateButton removeOverlayButton;
    private CreateButton notificationsButton;
    private CreateIndicator notificationsIndicator;
    private CreateScrollNumberInput scaleInput;

    private static final MutableComponent title = TextUtils.translate("gui.createrailwaysnavigator.overlay_settings.title");
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
    
    public RouteOverlaySettingsWindow(DLWindowManager manager, RouteDetailsOverlay overlay) {
        super(manager);
        setSize(GUI_WIDTH, GUI_HEIGHT);
        windowSpawnPosition.set(WindowPosition.PARENT_CENTER);

        backButton = addComponent(new CreateButton(width() - CreateButton.WIDTH - 7, height() - CreateButton.HEIGHT - 6, AllIcons.I_CONFIRM));
        backButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false; 
        });
        
        detailsButton = addComponent(new CreateButton(7, height() - CreateButton.HEIGHT - 6, AllIcons.I_VIEW_SCHEDULE));
        detailsButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal(mgr -> new RouteDetailsWindow(mgr, overlay.getRoute()));
            return false; 
        });
        detailsButton.tooltip.set(new DLTooltip(List.of(textShowDetails), 200));
        
        removeOverlayButton = addComponent(new CreateButton(7 + CreateButton.WIDTH + 3, height() - CreateButton.HEIGHT - 6, ModGuiIcons.DELETE.getAsCreateIcon()));
        removeOverlayButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            if (overlay.getWindowManager() != null) {
                overlay.getWindowManager().closeWindow(overlay);
            }
            return false; 
        });
        removeOverlayButton.tooltip.set(new DLTooltip(List.of(textUnpin), 200));


        // Notifications
        notificationsIndicator = addComponent(new CreateIndicator(20, 20));
        notificationsButton = addComponent(new CreateButton(20, 26, ModGuiIcons.INFO.getAsCreateIcon()));
        notificationsButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            overlay.getRoute().setShowNotifications(!overlay.getRoute().shouldShowNotifications());
        notificationsIndicator.state.set(overlay.getRoute().shouldShowNotifications() ? Indicator.State.ON : Indicator.State.OFF);
            return false;
        });
        notificationsButton.tooltip.set(new DLTooltip(List.of(textNotifications, textNotificationsDescription), 200));
        notificationsIndicator.state.set(overlay.getRoute().shouldShowNotifications() ? Indicator.State.ON : Indicator.State.OFF);

        
        scaleInput = addComponent(new CreateScrollNumberInput(63, 23, 43));
        scaleInput.format.set(new INumberFormatAdapter.DecimalNumberFormat(2));
        scaleInput.step.set(0.05);
        scaleInput.shiftStep.set(0.25);
        scaleInput.min.set(ModClientConfig.MIN_SCALE);
        scaleInput.max.set(ModClientConfig.MAX_SCALE);
        scaleInput.title.set(textScale);
        scaleInput.value.set(ModClientConfig.OVERLAY_SCALE.get());
        scaleInput.addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            ModClientConfig.OVERLAY_SCALE.set(e.value());
            overlay.scale.set(e.value());
            return false;
        });

        OverlayPosition[] positions = OverlayPosition.values();
        CreateButton[] positionBtns = new CreateButton[positions.length];
        for (int i = 0; i < positions.length; i++) {
            final OverlayPosition pos = positions[i];
            final CreateButton remOverlayButton = positionBtns[i] = addComponent(new CreateButton(123 + CreateButton.WIDTH * i, 23, pos.getIcon().getAsCreateIcon()));
            remOverlayButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                for (CreateButton b : positionBtns) {
                    b.enabled.set(true);
                }
                remOverlayButton.enabled.set(false);
                ModClientConfig.ROUTE_OVERLAY_POSITION.set(pos);
                return false;
            });
            remOverlayButton.enabled.set(pos != ModClientConfig.ROUTE_OVERLAY_POSITION.get());
            remOverlayButton.tooltip.set(new DLTooltip(List.of(pos.getEnumTranslation(), pos.getValueTranslation().withStyle(ChatFormatting.GRAY)), 200));
        }
    }

    @Override
    public void close() {        
        ModClientConfig.SPEC.save();
        ModClientConfig.SPEC.afterReload();
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderWindow(graphics, 0, 0, width(), height(), ContainerColor.PURPLE, BarColor.GRAY, FooterSize.DEFAULT.size(), FooterSize.SMALL.size(), false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, 4, title, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);

        CreateDynamicWidgets.renderContainer(graphics, 1, FooterSize.DEFAULT.size() - 1, 54, 36, ContainerColor.BLUE);
        CreateDynamicWidgets.renderContainer(graphics, 54, FooterSize.DEFAULT.size() - 1, 61, 36, ContainerColor.BLUE);
        CreateDynamicWidgets.renderContainer(graphics, 54 + 60, FooterSize.DEFAULT.size() - 1, 90, 36, ContainerColor.BLUE);
        
        GuiGameElement.of(renderedItem).<GuiGameElement
			.GuiRenderBuilder>at(width(), height() - 48, -200)
			.scale(5)
			.render(graphics.graphics());
    }
}