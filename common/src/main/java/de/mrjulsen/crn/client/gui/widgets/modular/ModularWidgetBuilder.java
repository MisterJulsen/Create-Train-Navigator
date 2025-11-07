package de.mrjulsen.crn.client.gui.widgets.modular;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLScrollBar;

public class ModularWidgetBuilder {

    private final ModularWidgetContainer container;
    private final Map<String, Consumer<ModularWidgetLine>> lineBuilders = new LinkedHashMap<>();

    public ModularWidgetBuilder(ModularWidgetContainer container) {
        this.container = container;
    }
    
    public void addLine(String name, Consumer<ModularWidgetLine> lineBuilder) {
        if (lineBuilders.containsKey(name)) {
            return;
        }
        lineBuilders.put(name, lineBuilder);
    }

    public void clear() {
        lineBuilders.clear();
    }

    public int getCurrentLinesCount() {
        return lineBuilders.size();
    }

    public boolean hasLine(String name) {
        return lineBuilders.containsKey(name);
    }

    public Consumer<ModularWidgetLine> getLine(String name) {
        return lineBuilders.get(name);
    }

    public void addToLine(String name, Consumer<ModularWidgetLine> additional) {
        lineBuilders.replace(name, lineBuilders.get(name).andThen(additional));
    }

    public void build() {
        int currentY = 0;
        for (Consumer<ModularWidgetLine> c : lineBuilders.values()) {
            ModularWidgetLine line = new ModularWidgetLine(0, 0, container.width());
            line.setWidth(container.width() - container.getPaddingLeft() - container.getPaddingRight());
            c.accept(line);
            currentY += container.addLine(line, currentY);
        }
        
        DLScrollBar scrollBar = container.getScrollbar();
        scrollBar.setX(container.x() + container.width() - scrollBar.width());
        scrollBar.setY(container.y());
        scrollBar.setHeight(container.height());
        scrollBar.scrollerSize.set(0);
        scrollBar.screenSize.set(container.height());
        scrollBar.scrollSteps.set(10);
        //scrollBar.maxScroll(currentY + container.getPaddingBottom());
        scrollBar.addEventListener(DLScrollBar.ValueChangedEvent.class, (s, e) -> {
            container.setScrollOffsetY(e.value());
            return false;
        });
    }
}
