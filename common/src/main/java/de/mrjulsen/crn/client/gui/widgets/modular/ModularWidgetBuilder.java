package de.mrjulsen.crn.client.gui.widgets.modular;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;


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
    }
}
