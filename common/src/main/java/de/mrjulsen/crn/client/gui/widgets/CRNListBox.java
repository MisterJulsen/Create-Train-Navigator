package de.mrjulsen.crn.client.gui.widgets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractScrollBar;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.ScrollableWidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class CRNListBox<T, W extends DLButton> extends ScrollableWidgetContainer {

    private final Screen parent;
    private final DLAbstractScrollBar<?> scrollBar;
    private int contentHeight = 0;
    private final Map<W, T> values = new HashMap<>();

    public CRNListBox(Screen parent, int x, int y, int width, int height, DLAbstractScrollBar<?> scrollBar) {
        super(x, y, width, height);
        this.parent = parent;
        this.scrollBar = scrollBar;
        
        scrollBar.setAutoScrollerSize(true);
        scrollBar.setScreenSize(height());
        scrollBar.updateMaxScroll(0);
        scrollBar.withOnValueChanged((sb) -> setYScrollOffset(sb.getScrollValue()));
        scrollBar.setStepSize(10);
    }

    public Screen getParent() {
        return parent;
    }

    public void displayData(List<T> data, Function<T, W> createItem) {
        clearWidgets();
        values.clear();
        contentHeight = 0;
        for (int i = 0; i < data.size(); i++) {
            T entry = data.get(i);
            W widget = createItem.apply(entry);
            widget.set_x(x());
            widget.set_width(width());
            widget.set_y(y() + contentHeight);        
            addRenderableWidget(widget);
            values.put(widget, entry);
            contentHeight += widget.height();
        }
        scrollBar.updateMaxScroll(contentHeight);
    }

    public Set<Entry<W, T>> getEntries() {
        return values.entrySet();
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        
        if (scrollBar.getScrollValue() > 0) {
            GuiUtils.fillGradient(graphics, x(), y(), 0, width(), 10, 0x77000000, 0x00000000);
        }
        if (scrollBar.getScrollValue() < scrollBar.getMaxScroll()) {
            GuiUtils.fillGradient(graphics, x(), y() + height() - 10, 0, width(), 10, 0x00000000, 0x77000000);
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
    
}
