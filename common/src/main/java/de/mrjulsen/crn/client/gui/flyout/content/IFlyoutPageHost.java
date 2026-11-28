/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.client.gui.flyout.content;

public interface IFlyoutPageHost {
    void pushPage(FlyoutContent content);
    void popPage();
    void closeFlyout();
}
