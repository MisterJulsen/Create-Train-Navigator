package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.web.WebApiEndpoints;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.crn.network.packets.pain.ShowWebApiScreenPacketData;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.Orientation;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.BorderLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

public class WebApiInfoViewer extends DLGuiComponent {

    private final DLPanel contentPanel;
    private final DLScrollBar scrollbar;

    public WebApiInfoViewer(int x, int y, int w, int h) {
        super(x, y, w, h);

        contentPanel = addComponent(new DLPanel(0, 0, 1, 1));
        contentPanel.layoutContraint.set(BorderLayout.BorderPosition.CENTER);
        contentPanel.inputConsumptionPolicy.set((type) -> false);

        FlowLayout layout = new FlowLayout();
        layout.fillCrossAxis.set(true);
        layout.flowDirection.set(Direction.VERTICAL);
        layout.padding.set(new Padding(6, 6, 6, 6));
        layout.wrap.set(false);
        layout.verticalGap.set(4);
        contentPanel.layout.set(layout);

        scrollbar = addComponent(new DLScrollBar(0, 0, 5, height(), Orientation.VERTICAL));
        scrollbar.componentRenderer.set(ModernScrollbarComponentRenderer.INSTANCE);
        scrollbar.layoutContraint.set(BorderLayout.BorderPosition.EAST);
        scrollbar.scrollerSize.set(0);
        scrollbar.screenSize.set(contentPanel.height());
        scrollbar.scrollSteps.set(15);
        scrollbar.max.set(0);
        scrollbar.inputConsumptionPolicy.set((type) -> true);
        scrollbar.addEventListener(DLScrollBar.ValueChangedEvent.class, (s, e) -> {
            contentPanel.setScrollOffsetY(e.value());
            return false;
        });

        addEventListener(DLGuiStandardEvents.ScrollEvent.class, scrollbar::invokeEvent);

        contentPanel.addEventListener(DLGuiStandardEvents.ComponentLayoutUpdatedEvent.class, (s, e) -> {
            scrollbar.max.set(e.layoutResult().contentHeight());
            scrollbar.screenSize.set(contentPanel.height());
            return false;
        });

        BorderLayout borderLayout = new BorderLayout(0, 0);
        this.layout.set(borderLayout);
    }

    public void display(ShowWebApiScreenPacketData packetData) {
        contentPanel.clearComponents();
        contentPanel.suspendLayout();

        int innerWidth = width() - 16;
        contentPanel.addComponent(new WebApiStatusPanel(innerWidth, packetData));

        MutableComponent endpointsTitle = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".web_api.endpoints")
            .withStyle(ChatFormatting.GOLD);
        contentPanel.addComponent(new WebApiSectionLabel(innerWidth, endpointsTitle));

        MutableComponent hint = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".web_api.click_to_copy")
            .withStyle(ChatFormatting.GRAY);
        contentPanel.addComponent(new WebApiSectionLabel(innerWidth, hint));

        for (WebApiEndpoints.Endpoint endpoint : WebApiEndpoints.all()) {
            contentPanel.addComponent(new WebApiEndpointWidget(0, 0, innerWidth, packetData.baseUrl() + endpoint.path(), endpoint.description()));
        }

        contentPanel.resumeLayout();
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (scrollbar.canScroll() && scrollbar.value.get() > 0) {
            GuiUtils.fillGradient(graphics, 0, 0, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.TOP);
        }
        if (scrollbar.canScroll() && scrollbar.value.get() < scrollbar.max.get()) {
            GuiUtils.fillGradient(graphics, 0, height() - 10, width(), 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.BOTTOM);
        }
    }

    private static final class WebApiSectionLabel extends DLGuiComponent {
        private final MutableComponent text;

        WebApiSectionLabel(int width, MutableComponent text) {
            super(0, 0, width, 12);
            this.text = text;
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 2, text, DLColor.WHITE, ETextAlignment.LEFT, false);
        }
    }

    private static final class WebApiStatusPanel extends DLGuiComponent {
        private static final int BASE_HEIGHT = 72;
        private static final int EXTRA_LINE = 11;

        private final ShowWebApiScreenPacketData status;

        WebApiStatusPanel(int width, ShowWebApiScreenPacketData status) {
            super(0, 0, width, panelHeight(status));
            this.status = status;
        }

        private static int panelHeight(ShowWebApiScreenPacketData status) {
            int height = BASE_HEIGHT;
            if (!status.enabled() || (status.enabled() && !status.running())) {
                height += EXTRA_LINE;
            }
            return height;
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, width(), height(), ColorShade.DARK.getColor());

            int x = 6;
            int y = 5;
            int lineHeight = graphics.defaultFont().lineHeight + 2;
            String modId = CreateRailwaysNavigator.MOD_ID;

            drawLine(graphics, x, y, TextUtils.translate("gui." + modId + ".web_api.status.config"),
                TextUtils.text(status.enabled() ? "enabled" : "disabled")
                    .withStyle(status.enabled() ? ChatFormatting.GREEN : ChatFormatting.RED));
            y += lineHeight;
            drawLine(graphics, x, y, TextUtils.translate("gui." + modId + ".web_api.status.server"),
                TextUtils.text(status.running() ? "running" : "not running")
                    .withStyle(status.running() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            y += lineHeight;
            drawLine(graphics, x, y, TextUtils.text("Bind"), TextUtils.text(status.bindAddress() + ":" + status.port()).withStyle(ChatFormatting.AQUA));
            y += lineHeight;
            drawLine(graphics, x, y, TextUtils.text("Base URL"), TextUtils.text(status.baseUrl()).withStyle(ChatFormatting.AQUA));
            y += lineHeight;
            drawLine(graphics, x, y, TextUtils.text("Stats"),
                TextUtils.text(status.activeStops() + " at stations, " + status.trackedTrains() + " trains, "
                    + status.bufferedEvents() + "/" + status.maxEvents() + " events (latest id " + status.latestEventId() + ")")
                    .withStyle(ChatFormatting.WHITE));

            if (status.enabled() && !status.running()) {
                y += lineHeight;
                GuiUtils.drawString(
                    graphics,
                    graphics.defaultFont(),
                    x,
                    y,
                    TextUtils.translate("gui." + modId + ".web_api.not_running_hint").withStyle(ChatFormatting.YELLOW),
                    DLColor.fromInt(ChatFormatting.YELLOW.getColor().intValue()),
                    ETextAlignment.LEFT,
                    false
                );
            } else if (!status.enabled()) {
                y += lineHeight;
                GuiUtils.drawString(
                    graphics,
                    graphics.defaultFont(),
                    x,
                    y,
                    TextUtils.translate("gui." + modId + ".web_api.config_hint").withStyle(ChatFormatting.GRAY),
                    DLColor.fromInt(ChatFormatting.GRAY.getColor().intValue()),
                    ETextAlignment.LEFT,
                    false
                );
            }
        }

        private static void drawLine(DLGuiGraphics graphics, int x, int y, MutableComponent label, MutableComponent value) {
            GuiUtils.drawString(graphics, graphics.defaultFont(), x, y, label.copy().append(TextUtils.text(": ")).append(value), DLColor.WHITE, ETextAlignment.LEFT, false);
        }
    }
}
