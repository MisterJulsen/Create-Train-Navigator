package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLItemSelectionBox;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;

public class FlatCheckListBox<T> extends DLItemSelectionBox<T> {

    public FlatCheckListBox(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
    }

    @SuppressWarnings("unchecked")
    public List<FlatCheckListBoxItem<T>> getItemComponents() {
        List<FlatCheckListBoxItem<T>> components = new ArrayList<>(items.size());
        for (DLGuiComponent component : contentPanel.getComponents()) {
            if (component instanceof FlatCheckListBoxItem c) {
                components.add((FlatCheckListBoxItem<T>)c);
            }
        }
        return components;
    }

    public static class FlatCheckListBoxItem<T> extends DLItemSelectionBox.DLListBoxItem<T> {

        public final BooleanProperty checked = new BooleanProperty(false);

        protected FlatCheckListBoxItem(DLItemSelectionBox<T> collectionComponentRef, T item, int w) {
            super(collectionComponentRef, item, w, 18);

            addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                checked.toggle();
                return false;
            });
        }

        public T getItem() {
            return item;
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            if (isSelected()) {
                GuiUtils.fill(graphics, getRenderBounds(), DLColor.fromInt(0x44FFFFFF));
            }

            if (checked.get()) {
                ModGuiIcons.CHECKMARK.render(graphics, 1, 1);
            }

            GuiUtils.drawString(graphics, graphics.defaultFont(), 22, height() / 2 - graphics.defaultFont().lineHeight / 2, this.item.toString(), isSelected() ? DragonLib.VANILLA_BUTTON_HIGHLIGHTED_FONT_COLOR : DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        }
        
    }
    
}
