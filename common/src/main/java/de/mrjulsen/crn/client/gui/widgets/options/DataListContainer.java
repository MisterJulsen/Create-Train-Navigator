package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.Single.MutableSingle;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class DataListContainer<T, S> extends WidgetContainer {

    private static final int BORDER_WIDTH = 2;

    private final Screen parent;
    private final OptionEntry<?> parentEntry;
    private T data;
    private int entriesHeight;
    private int addNewEntryHeight;
    private int contentHeight;

    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private int paddingBottom;
    private boolean renderBackground = true;
    private boolean bordered = true;

    private final Collection<DLTooltip> tooltips = new ArrayList<>();

    private final Function<T, Iterator<S>> onGetData;
    private final BiFunction<S, SimpleDataListEntry<T, S>, String> onCreateEntry;
    private final Consumer<DataListContainer<T, S>> onContainerSizeChanged;
    private final BiConsumer<T, SimpleDataListNewEntry<T, S>> createNewEntry;

    private String filterText = "";
    private BiPredicate<S, Supplier<String>> filter;



    /**
     * @param parent The parent screen.
     * @param x The x position.
     * @param y The y position.
     * @param width The width of this widget.
     * @param initialData The data which this widget should display and process.
     * @param dataIterator The iterator of the contents of the data.
     * @param onCreateEntry Called when creating the entries.
     * @param createNewEntry Called when creating the "Add New Entry" widget. Pass {@code null} if no "Add New Entry" widget should be created.
     * @param onContainerSizeChanged Called when the size of the container changes to adjust the gui surrounding it.
     */
    public DataListContainer(OptionEntry<?> parentEntry, int x, int y, int width, T initialData, Function<T, Iterator<S>> dataIterator, BiFunction<S, SimpleDataListEntry<T, S>, String> onCreateEntry, BiConsumer<T, SimpleDataListNewEntry<T, S>> createNewEntry, Consumer<DataListContainer<T, S>> onContainerSizeChanged) {
        super(x, y, width, 100);
        this.parent = parentEntry.getParentScreen();
        this.parentEntry = parentEntry;

        this.onGetData = dataIterator;
        this.onCreateEntry = onCreateEntry;
        this.onContainerSizeChanged = onContainerSizeChanged;
        this.createNewEntry = createNewEntry;

        displayData(initialData, false);
    }

    public void setPadding(int top, int right, int bottom, int left) {
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        this.paddingLeft = left;
        displayData(data, false);
    }

    public void setFilter(BiPredicate<S, Supplier<String>> filter) {
        this.filter = filter;
        displayData(data, false);
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public boolean isRenderBackground() {
        return renderBackground;
    }

    public void setRenderBackground(boolean renderBackground) {
        this.renderBackground = renderBackground;
    }

    public boolean isBordered() {
        return bordered;
    }

    public void setBordered(boolean bordered) {
        this.bordered = bordered;
    }

    public int displayData(T data) {
        return displayData(data, true);
    }

    private int displayData(T data, boolean notifySizeChanged) {
        clearWidgets();
        tooltips.clear();
        this.data = data;
        contentHeight = paddingTop + BORDER_WIDTH;
        entriesHeight = 0;
        addNewEntryHeight = 0;

        MutableSingle<DLEditBox> searchBox = new MutableSingle<>(null);
        DLUtils.doIfNotNull(filter, x -> {      
            searchBox.setFirst(addRenderableWidget(new DLEditBox(font, x() + BORDER_WIDTH + paddingLeft + 1, y() + 2 + contentHeight, width() - BORDER_WIDTH * 2 - paddingLeft - paddingRight - 2, 14, TextUtils.empty()) {
                @Override
                public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
                    if (code == GLFW.GLFW_KEY_ENTER) {
                        filterText = getValue();
                        displayData(data, true);
                        return true;
                    }
                    return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
                }
            }));
            searchBox.getFirst().withHint(DragonLib.TEXT_SEARCH);
            searchBox.getFirst().setValue(filterText);
            int h = searchBox.getFirst().height() + 4;
            contentHeight += h;
            entriesHeight += h;
            addNewEntryHeight += h;
        });

        Iterator<S> content = onGetData.apply(data);
        while (content.hasNext()) {
            final S current = content.next();
            if (filter != null) {
                if (!filter.test(current, () -> searchBox.getFirst() == null ? "" : searchBox.getFirst().getValue())) {
                    continue;
                }
            }
            SimpleDataListEntry<T, S> entry = addRenderableWidget(new SimpleDataListEntry<>(this, x() + BORDER_WIDTH + paddingLeft, y() + contentHeight, width() - BORDER_WIDTH * 2 - paddingLeft - paddingRight, current));
            entry.setText(onCreateEntry.apply(current, entry));
            entry.buildInternal();
            contentHeight += entry.height();
            entriesHeight += entry.height();
        }
        
        DLUtils.doIfNotNull(createNewEntry, x -> {            
            SimpleDataListNewEntry<T, S> entry = addRenderableWidget(new SimpleDataListNewEntry<>(this, x() + BORDER_WIDTH + paddingLeft, y() + contentHeight, width() - BORDER_WIDTH * 2 - paddingLeft - paddingRight));
            createNewEntry.accept(data, entry);
            entry.buildInternal();
            contentHeight += entry.height();
            addNewEntryHeight = entry.height();
        });

        contentHeight += paddingBottom + BORDER_WIDTH;
        set_height(contentHeight);

        if (notifySizeChanged) {
            onContainerSizeChanged.accept(this);
        }
        return height();
    }

    public Screen getParentScreen() {
        return parent;
    }

    public OptionEntry<?> getParentEntry() {
        return parentEntry;
    }

    public T getData() {
        return data;
    }
    
    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isRenderBackground()) {
            if (isBordered()) {
                if (createNewEntry == null) {
                    CreateDynamicWidgets.renderSingleShadeWidget(graphics, x(), y(), width(), height(), ColorShade.LIGHT);
                } else {
                    CreateDynamicWidgets.renderDuoShadeWidget(graphics, x(), y(), width(), BORDER_WIDTH + paddingTop + entriesHeight, ColorShade.LIGHT, BORDER_WIDTH + paddingBottom + addNewEntryHeight, ColorShade.DARK);
                }
            } else {
                GuiUtils.fill(graphics, x(), y(), width(), height(), ColorShade.LIGHT.getColor());
                if (createNewEntry != null) {
                    GuiUtils.fill(graphics, x(), y() + BORDER_WIDTH + paddingTop + entriesHeight, width(), BORDER_WIDTH + paddingBottom + addNewEntryHeight, ColorShade.DARK.getColor());
                }
            }
        }
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
    }

    public Collection<DLTooltip> getTooltips() {
        return tooltips;
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);
        tooltips.stream().forEach(x -> x.render(parent, graphics, mouseX, mouseY));

    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }
}
