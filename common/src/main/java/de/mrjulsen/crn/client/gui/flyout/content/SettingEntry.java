/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.client.gui.flyout.content;

import java.util.function.Supplier;

import net.minecraft.network.chat.Component;

public record SettingEntry(Component label, Supplier<String> value, Supplier<FlyoutContent> content) {

    public SettingEntry(Component label, Supplier<FlyoutContent> content) {
        this(label, () -> "", content);
    }
}
