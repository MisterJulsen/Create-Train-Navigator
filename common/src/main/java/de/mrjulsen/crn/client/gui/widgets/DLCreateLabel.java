package de.mrjulsen.crn.client.gui.widgets;

import com.simibubi.create.foundation.gui.widget.Label;

import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import net.minecraft.network.chat.Component;

public class DLCreateLabel extends Label implements IDragonLibWidget {

    private boolean mouseSelected;

    public DLCreateLabel(int x, int y, Component text) {
        super(x, y, text);
    }

    @Override
    public void onFocusChangeEvent(boolean focus) {}

    @Override
    public DLContextMenu getContextMenu() {
        return null;
    }

    @Override
    public void setMenu(DLContextMenu menu) {}

    @Override
    public boolean isMouseSelected() {
        return mouseSelected;
    }

    @Override
    public void setMouseSelected(boolean selected) {
        this.mouseSelected = selected;
    }    

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
    
}
