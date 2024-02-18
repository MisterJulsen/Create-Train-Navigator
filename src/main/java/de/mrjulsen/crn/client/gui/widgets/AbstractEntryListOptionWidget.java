package de.mrjulsen.crn.client.gui.widgets;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public abstract class AbstractEntryListOptionWidget extends Button implements IEntryListSettingsOption {

    public AbstractEntryListOptionWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress);
    }
}
