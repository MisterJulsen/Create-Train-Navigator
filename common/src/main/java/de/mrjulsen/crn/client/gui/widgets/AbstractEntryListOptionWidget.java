package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;

public abstract class AbstractEntryListOptionWidget extends WidgetContainer implements IEntryListSettingsOption {

    public AbstractEntryListOptionWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }
    
    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return true;
    }

}
