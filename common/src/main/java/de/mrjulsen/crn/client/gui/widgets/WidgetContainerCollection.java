package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;

public class WidgetContainerCollection {

    public final List<WidgetContainer> components = new ArrayList<>(); 
    
    private boolean enabled = true;
    private boolean visible = true;

    public void performForEach(Predicate<? super WidgetContainer> filter, Consumer<? super WidgetContainer> consumer) {
        components.stream().filter(filter).forEach(consumer);
    }

    public void performForEach(Consumer<? super WidgetContainer> consumer) {
        performForEach(x -> true, consumer);
    }

    public <C extends WidgetContainer> void performForEachOfType(Class<C> clazz, Predicate<C> filter, Consumer<C> consumer) {
        components.stream().filter(clazz::isInstance).map(clazz::cast).filter(filter).forEach(consumer);
    }

    public <C extends WidgetContainer> void performForEachOfType(Class<C> clazz, Consumer<C> consumer) {
        performForEachOfType(clazz, x -> true, consumer);
    }

    

    public boolean isVisible() {
        return visible;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setVisible(boolean v) {
        this.visible = v;
        performForEach(x -> x.set_visible(v));
    }

    public void setEnabled(boolean e) {
        this.enabled = e;
        performForEach(x -> x.set_active(e));
    }

    public <W extends WidgetContainer> void add(W widget) {
        widget.set_active(enabled);
        widget.set_visible(visible);
        components.add(widget);
    }

    public void clear() {
        components.clear();
    }

    public void clear(Consumer<WidgetContainer> onRemove) {
        performForEach(x -> onRemove.accept(x));
        clear();
    }
}