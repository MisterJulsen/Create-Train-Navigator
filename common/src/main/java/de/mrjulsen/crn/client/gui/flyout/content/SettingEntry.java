package de.mrjulsen.crn.client.gui.flyout.content;

import java.util.function.Supplier;

import net.minecraft.network.chat.Component;

public record SettingEntry(Component label, Supplier<String> value, Supplier<FlyoutContent> content) {

    public SettingEntry(Component label, Supplier<FlyoutContent> content) {
        this(label, () -> "", content);
    }
}
