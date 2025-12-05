package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar.Orientation;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

public class OptionsView extends DLGuiComponent {

    public static interface IOptionsViewEntry {}
    
    private final DLPanel contentPanel;
    private final DLScrollBar scrollbar;

    private final List<DLGuiComponent> entries = new ArrayList<>();

    private boolean refreshing = false;

    public OptionsView(int x, int y, int w, int h) {
        super(x, y, w, h);

        contentPanel = addComponent(new DLPanel(0, 0, width(), height()));
        contentPanel.anchor.set2(EAlign.values());
        contentPanel.inputConsumptionPolicy.set((type) -> false);

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
    }

    public <T extends DLGuiComponent> T addEntry(T entry) {
        this.entries.add(entry);
        entry.addEventListener(DLGuiStandardEvents.ComponentPosAndSizeChanged.class, (s, e) -> {
            if (refreshing) {
                return false;
            }
            refresh();
            return false;
        });
        refresh();
        return entry;
    }

    public void clearEntries() {
        entries.clear();
    }

    public void refresh() {
        refreshing = true;
        contentPanel.clearComponents();
        int y = 10;
        for (DLGuiComponent entry : entries) {
            entry.setPosition(10, y);
            entry.setWidth(width() - 20);
            contentPanel.addComponent(entry);
            y += entry.height() + 3;
        }
        y += 7;
        
        scrollbar.max.set(y);
        scrollbar.screenSize.set(contentPanel.height());
        refreshing = false;
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
