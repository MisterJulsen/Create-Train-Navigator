package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.widgets.SearchBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractDataView;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout.Direction;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.DLAbstractRichTextInputField;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.Property;

public class OptionsDataView<T> extends DLAbstractDataView<T, OptionsDataView.DLBasicItem<T>> {    

    public final Property<Function<OptionsDataView<T>, CreateEntryItem<T>>> createNewItemBuilder = new Property<Function<OptionsDataView<T>, CreateEntryItem<T>>>(null)
        .withAfterPropertyChangedCallback((a, b) -> {
            createComponents();
        });
        
    public final Property<BiPredicate<T, String>> searchFilter = new Property<BiPredicate<T, String>>(null)
        .withAfterPropertyChangedCallback((a, b) -> {
            createComponents();
        });

    private int requiredHeight = 0;

    private SearchBox searchBox;

    public OptionsDataView(int x, int y, int w, int h) {
        super(x, y, w, h);

        FlowLayout layout = new FlowLayout();
        layout.fillCrossAxis.set(true);
        layout.flowDirection.set(Direction.VERTICAL);
        layout.verticalGap.set(2);
        //layout.padding.set(new Padding(5, 5, 5, 15));
        layout.wrap.set(false);
        contentPanel.layout.set(layout);
        
        this.searchBox = new SearchBox(0, 0, 1);
        this.searchBox.acceptAndCancelKeysEnabled.set(true);
        this.searchBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
            createComponents();
            return false;
        });
        this.searchBox.placeholderText.set(Constants.TEXT_SEARCH);
    }

    @Override
    protected DLBasicItem<T> defaultItemBuilder(T item) {
        return new DLBasicItem<>(this, item);
    }    

    @Override
    protected void createComponents() {
        contentPanel.clearComponents();
        if (searchFilter.get() != null) {
            contentPanel.addComponent(searchBox);
        }
        if (createNewItemBuilder.get() != null) {
            contentPanel.addComponent(createNewItemBuilder.get().apply(this));
        }
        for (T item : items.get()) {
            if (searchFilter.get() != null && searchBox != null && !searchFilter.get().test(item, searchBox.text.get().getPlainText())) {
                continue;
            }
            DLBasicItem<T> listItem = itemBuilder.get().apply(item);
            contentPanel.addComponent(listItem);
        }
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
    }
    

    public static class DLBasicItem<T> extends DLAbstractDataView.DLDataViewItem<T, OptionsDataView<T>> {
        public DLBasicItem(OptionsDataView<T> collectionComponentRef, T item) {
            super(collectionComponentRef, item);
        }

        @Override
        protected void refresh() {
            this.contentPanel.clearComponents();
            Map<String, DLGuiComponent> content = subComponents.stream().collect(Collectors.toMap(x -> x.name(), x -> x.component()));

            int totalWidth = collectionComponentRef.width();
            int fixedWidthSum = collectionComponentRef.dataSlots.stream().filter(s -> s.mode() == SizeMode.FIXED).mapToInt(s -> (int)(s.size() + 2)).sum();
            int remainingWidth = Math.max(totalWidth - fixedWidthSum, 0);
            double percentTotal = collectionComponentRef.dataSlots.stream().filter(s -> s.mode() == SizeMode.PERCENTAGE).mapToDouble(s -> (s.size() + 2)).sum();

            Map<String, Integer> widths = new HashMap<>();

            for (DataSlot s : collectionComponentRef.dataSlots) {
                if (s.mode() == SizeMode.FIXED) {
                    widths.put(s.name(), (int)(s.size() + 2));
                } else {
                    double part = (percentTotal == 0 ? 0 : ((s.size() + 2) / percentTotal));
                    widths.put(s.name(), (int)Math.round(remainingWidth * part));
                }
            }

            int x = 0;
            int maxH = 0;

            for (DataSlot s : collectionComponentRef.dataSlots) {
                int w = widths.get(s.name());
                DLGuiComponent c = content.get(s.name());
                if (c != null) {
                    int h = c.height();
                    c.setPosition(x, 0);
                    c.setSize(w - 2, h);
                    maxH = Math.max(maxH, h);
                    contentPanel.addComponent(c);
                }
                x += w;
            }
            setHeight(maxH);
        }
    }

    public static class CreateEntryItem<T> extends DLBasicItem<T> {

        public CreateEntryItem(OptionsDataView<T> collectionComponentRef) {
            super(collectionComponentRef, null);
        }
        
    }
}
