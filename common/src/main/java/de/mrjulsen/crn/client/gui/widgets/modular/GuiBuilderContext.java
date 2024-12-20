package de.mrjulsen.crn.client.gui.widgets.modular;

public class GuiBuilderContext {
    private final ModularWidgetBuilder builder;
    private final ModularWidgetContainer container;

    public GuiBuilderContext(ModularWidgetBuilder builder, ModularWidgetContainer container) {
        this.builder = builder;
        this.container = container;
    }

    public ModularWidgetBuilder builder() {
        return builder;
    }

    public ModularWidgetContainer container() {
        return container;
    }
}
