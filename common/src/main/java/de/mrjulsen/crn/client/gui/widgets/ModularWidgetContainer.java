package de.mrjulsen.crn.client.gui.widgets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.mcdragonlib.annotations.SupportsEvents;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.Orientation;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.LayoutResult;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.events.IEvent;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;

@SupportsEvents({
    ModularWidgetContainer.ContentLayoutUpdatedEvent.class
})
public class ModularWidgetContainer extends DLGuiComponent {

    public record ContentLayoutUpdatedEvent(LayoutResult layoutResult) implements IEvent {}

    public final BooleanProperty autoHeight = new BooleanProperty(false);

    public final DLPanel contentPanel;
    public final DLScrollBar scrollbar;

    private final Map<String, DLPanel> namedPanels = new ConcurrentHashMap<>();

    public ModularWidgetContainer(int x, int y, int w, int h) {
        super(x, y, w, h);

        contentPanel = addComponent(new DLPanel(0, 0, width(), height()));
        contentPanel.anchor.set(EAlign.values());
        contentPanel.inputConsumptionPolicy.set((type) -> false);
        
        FlowLayout layout = new FlowLayout();
        layout.fillCrossAxis.set(true);
        layout.flowDirection.set(Direction.VERTICAL);
        layout.padding.set(new Padding(10, 15, 10, 15));
        layout.wrap.set(false);
        layout.verticalGap.set(3);
        contentPanel.layout.set(layout);

        scrollbar = addComponent(new DLScrollBar(width() - 5, 0, 5, height(), Orientation.VERTICAL));
        scrollbar.componentRenderer.set(ModernScrollbarComponentRenderer.INSTANCE);
        scrollbar.anchor.set2(EAlign.TOP, EAlign.BOTTOM, EAlign.RIGHT);
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
            invokeEvent(this, new ContentLayoutUpdatedEvent(e.layoutResult()));          
            scrollbar.max.set(e.layoutResult().contentHeight());
            scrollbar.screenSize.set(contentPanel.height());
            if (autoHeight.get()) {
                setHeight(e.layoutResult().contentHeight());
            }
            return false;
        });
    }

    public DLPanel addLine(String name) {
        if (namedPanels.containsKey(name)) {
            return namedPanels.get(name);
        }
        DLPanel panel = new DLPanel(0, 0, 0, 0);
        FlowLayout layout = new FlowLayout();
        layout.flowDirection.set(Direction.HORIZONTAL);
        layout.horizontalGap.set(3);
        layout.wrap.set(false);
        panel.layout.set(layout);
        panel.addEventListener(DLGuiStandardEvents.ComponentLayoutUpdatedEvent.class, (s, e) -> {
            panel.setHeight(e.layoutResult().contentHeight());
            return false;
        });
        namedPanels.put(name, panel);
        return contentPanel.addComponent(panel);
    }

    public void removeLine(DLPanel linePanel) {
        contentPanel.removeComponent(linePanel);
        namedPanels.values().removeIf(x -> x == linePanel);
    }    

    public void removeLine(String name) {
        contentPanel.removeComponent(namedPanels.remove(name));
    }

    public void clearLines() {
        contentPanel.clearComponents();
        namedPanels.clear();
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
}
