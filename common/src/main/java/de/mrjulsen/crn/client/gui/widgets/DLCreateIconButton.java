package de.mrjulsen.crn.client.gui.widgets;

import com.simibubi.create.foundation.gui.element.ScreenElement;
import com.simibubi.create.foundation.gui.widget.IconButton;

import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;

public class DLCreateIconButton extends IconButton implements IDragonLibWidget {

    private boolean mouseSelected;

    public DLCreateIconButton(int x, int y, int w, int h, ScreenElement icon) {
        super(x, y, w, h, icon);
    }

    public DLCreateIconButton(int x, int y, ScreenElement icon) {
        super(x, y, icon);
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
