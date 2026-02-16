package de.mrjulsen.crn.client.gui.widgets.modular;

import de.mrjulsen.crn.client.gui.widgets.ModularWidgetContainer;

public class GuiBuilderContext {
    private final ModularWidgetContainer container;

    public GuiBuilderContext(ModularWidgetContainer container) {
        this.container = container;
    }

    public ModularWidgetContainer container() {
        return container;
    }
}
