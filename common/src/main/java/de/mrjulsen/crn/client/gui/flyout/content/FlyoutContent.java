/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.client.gui.flyout.content;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import net.minecraft.network.chat.MutableComponent;

public abstract class FlyoutContent extends DLGuiComponent {

    private final int preferredW;
    private final int preferredH;
    private boolean mounted = false;

    protected IFlyoutPageHost host;

    public FlyoutContent(int preferredWidth, int preferredHeight) {
        super(0, 0, preferredWidth, preferredHeight);
        this.preferredW = preferredWidth;
        this.preferredH = preferredHeight;
    }

    public abstract MutableComponent getTitle();

    public void resetToDefaults() {}

    public boolean isResettable() {
        return true;
    }

    /*
    public int preferredWidth() {
        return preferredW;
    }

    public int preferredHeight() {
        return preferredH;
    }

     */

    protected void onMount() {}

    public void onShow() {}

    public void onHide() {}

    public final void ensureMounted(IFlyoutPageHost host) {
        this.host = host;
        if (!mounted) {
            mounted = true;
            onMount();
        }
    }
}
