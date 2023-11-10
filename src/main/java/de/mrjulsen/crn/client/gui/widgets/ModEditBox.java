package de.mrjulsen.crn.client.gui.widgets;

import java.util.function.BiConsumer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ModEditBox extends EditBox {

    private BiConsumer<EditBox, Boolean> onFocusChanged = null;

    public ModEditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pFont, pX, pY, pWidth, pHeight, pMessage);
    }

    public void setOnFocusChanged(BiConsumer<EditBox, Boolean> onFocusChanged) {
        this.onFocusChanged = onFocusChanged;
    }

    @Override
    protected void setFocused(boolean pFocused) {
        super.setFocused(pFocused);
        if (onFocusChanged != null) {
            onFocusChanged.accept(this, pFocused);
        }
    }

    @Override
    public void onFocusedChanged(boolean pFocused) {
        super.onFocusedChanged(pFocused);
        if (onFocusChanged != null) {
            onFocusChanged.accept(this, pFocused);
        }
    }
}
