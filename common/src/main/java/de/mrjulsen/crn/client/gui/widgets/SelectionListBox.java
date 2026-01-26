package de.mrjulsen.crn.client.gui.widgets;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.skins.ModernScrollbarComponentRenderer;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.annotations.SupportsEvents;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLItemSelectionBox;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.events.IEvent;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;

@SupportsEvents({
    SelectionListBox.SelectEvent.class
})
public class SelectionListBox<T> extends DLItemSelectionBox<T> { 
    
    public record SelectEvent(DLListBoxItem<?> item) implements IEvent {}

    public SelectionListBox(int x, int y, int w, int h) {
        super(x, y, w, h);
        scrollBar.componentRenderer.set(ModernScrollbarComponentRenderer.INSTANCE);
        scrollBar.setSize(5, height());
        scrollBar.setPosition(width() - 5, 0);
        
        this.contentPanel.setPosition(0, 0);
        this.contentPanel.setSize(width(), height());
    }

    @Override
    protected DLListBoxItem<T> defaultItemBuilder(T item) {
        SelectionBoxItem<T> itm = new SelectionBoxItem<>(this, item);
        itm.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            invokeEvent(this, new SelectEvent(itm));
            return false;
        });
        return itm;
    }

    @Override
    protected void defaultItemSelection(ItemSelectionChangeEvent event) {
        List<DLListBoxItem<T>> itemComponents = getSelectedComponent();
        DLListBoxItem<?> clickedItem = event.item();
        boolean isSelected = clickedItem.selected.get();

        if (!multiselect.get()) {
            for (DLListBoxItem<T> itm : itemComponents) {
                if (itm != event.item()) {
                    itm.selected.set(false);
                }
            }
        }
        
        event.selected().setValue(!isSelected);
    }

    public void selectIf(Predicate<T> test) {
        List<T> items = new LinkedList<>();
        for (T item : this.items) {
            if (test.test(item)) {
                items.add(item);
            }
        }
        this.selectedItems.set(items);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
    }

    public static class SelectionBoxItem<T> extends DLItemSelectionBox.DLListBoxItem<T> {

        protected SelectionBoxItem(SelectionListBox<T> collectionComponentRef, T item) {
            super(collectionComponentRef, item, 100, 16);
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            if (selected.get()) {
                ModGuiIcons.CHECKMARK.render(graphics, 1, 0);
            }
            
            if (isSelected()) {
                GuiUtils.fill(graphics, 0, 0, width(), height(), DLColor.fromInt(0x44FFFFFF));
            }
            GuiUtils.drawString(graphics, Minecraft.getInstance().font, 20, height() / 2 - Minecraft.getInstance().font.lineHeight / 2, ((SelectionListBox<T>)collectionComponentRef).textFormat.get().apply(item), selected.get() ? DragonLib.VANILLA_BUTTON_HIGHLIGHTED_FONT_COLOR : DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        }

        public T getItem() {
            return item;
        }

    }
}
