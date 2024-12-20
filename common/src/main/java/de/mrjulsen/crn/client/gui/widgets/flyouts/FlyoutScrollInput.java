package de.mrjulsen.crn.client.gui.widgets.flyouts;

import java.util.Locale;
import java.util.function.Consumer;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.CRNListBox;
import de.mrjulsen.crn.client.gui.widgets.DLCreateSelectionScrollInput;
import de.mrjulsen.crn.client.gui.widgets.FlatCheckBox;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

public class FlyoutScrollInput<T extends GuiEventListener & Widget & NarratableEntry> extends AbstractFlyoutWidget<T> {
    private final DLEditBox searchBox;
    private final CRNListBox<Component, FlatCheckBox> content;
    private final DLCreateSelectionScrollInput input;

    public FlyoutScrollInput(DLScreen screen, FlyoutPointer pointer, ColorShade pointerShade, Consumer<T> addRenderableWidgetFunc, Consumer<GuiEventListener> removeWidgetFunc, DLCreateSelectionScrollInput input) {
        super(screen, 1, 120, pointer, pointerShade, addRenderableWidgetFunc, removeWidgetFunc);
        this.input = input;
        set_width(input.width() + 10);

        searchBox = addRenderableWidget(new DLEditBox(font, getContentArea().getX() + 3, getContentArea().getY() + 3, getContentArea().getWidth() - 6, 16, TextUtils.empty()));
        searchBox.withHint(DragonLib.TEXT_SEARCH);
        searchBox.setResponder((text) -> reload(null));

        final int searchBoxHeight = searchBox.x() + searchBox.height() - 1;
        
        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(screen, getContentArea().getX() + getContentArea().getWidth() - 7, searchBoxHeight + 2, getContentArea().getHeight() - searchBoxHeight + 1, GuiAreaDefinition.of(screen));
        this.content = addRenderableWidget(new CRNListBox<>(screen, getContentArea().getX() + 2, searchBoxHeight + 2, getContentArea().getWidth() - 4, getContentArea().getHeight() - searchBoxHeight + 1, scrollBar));
        
        addRenderableWidget(scrollBar);
    }

    @Override
    public void open(IDragonLibWidget parent) {
        reload(() -> super.open(parent));
    }

    private void reload(Runnable andThen) {
        content.displayData(input.getOptions(), (c, i) -> {
            if (!c.getString().toLowerCase(Locale.ROOT).contains(searchBox.getValue().toLowerCase(Locale.ROOT))) {
                return null;
            }
            
            return new FlatCheckBox(0, 0, 0, c.getString(), input.getState() == i,
                (b) -> {
                    content.getEntries().forEach(x -> {
                        boolean checked = x.getKey() == b;
                        x.getKey().setChecked(checked, false);
                    });                        
                    if (b.isChecked()) {
                        input.setState(i);
                        input.onChanged();
                        closeImmediately();
                    }
                }
            );
        });
        DLUtils.doIfNotNull(andThen, x -> x.run());
    }
}
