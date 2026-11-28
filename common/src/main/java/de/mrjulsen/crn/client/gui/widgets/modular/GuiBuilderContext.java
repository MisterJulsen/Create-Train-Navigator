/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

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
