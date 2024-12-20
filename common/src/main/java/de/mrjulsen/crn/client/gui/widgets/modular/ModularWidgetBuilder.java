package de.mrjulsen.crn.client.gui.widgets.modular;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractScrollBar;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
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
            line.set_width(container.width() - container.getPaddingLeft() - container.getPaddingRight());
            c.accept(line);
            currentY += container.addLine(line, currentY);
        }
        
        DLAbstractScrollBar<?> scrollBar = container.getScrollbar();
        scrollBar.set_x(container.x() + container.width() - scrollBar.width());
        scrollBar.set_y(container.y());
        scrollBar.set_height(container.height());
        scrollBar.setAutoScrollerSize(true);
        scrollBar.setScreenSize(container.height());
        scrollBar.setMaxScroll(currentY + container.getPaddingBottom());
        scrollBar.withOnValueChanged((sb) -> container.setYScrollOffset(sb.getScrollValue()));
        scrollBar.setStepSize(10);
    }
}
