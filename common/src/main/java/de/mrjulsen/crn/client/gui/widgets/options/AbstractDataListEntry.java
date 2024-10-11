package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public abstract class AbstractDataListEntry<T, S, E extends AbstractDataListEntry.AbstractDataSectionDefinition<T, S>> extends WidgetContainer {

    protected final DataListContainer<T, S> parent;

    public final int CONTENT_POS_LEFT = 0;
    public final int CONTENT_POS_RIGHT = 0;
    public final int CONTENT_SPACING = 3;
    public final int TEXT_OFFSET = 3;

    private boolean wasBuild = false;

    private int buttonsXOffset = 0;
    private int sectionsXOffset = 0;
    private List<E> sections = new ArrayList<>();

    protected final S data;
    private String text;

    public AbstractDataListEntry(DataListContainer<T, S> parent, int x, int y, int width, S data) {
        super(x, y, width, 20);
        this.parent = parent;
        this.data = data;
    }

    public void setText(String text) {        
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public ImmutableList<E> getSections() {
        return ImmutableList.copyOf(sections);
    }

    public int getCurrentButtonsXOffset() {
        return buttonsXOffset;
    }

    public int getCurrentSectionsXOffset() {
        return sectionsXOffset;
    }

    public <W extends IDragonLibWidget & GuiEventListener & Widget> W addWidget(W widget) {
        if (wasBuild) {
            throw new IllegalStateException("Cannot add elements to this widget after finishing creation.");
        }
        addRenderableWidget(widget);
        buttonsXOffset += widget.width();
        return widget;
    }

    public DLIconButton addButton(Sprite icon, DataListEntryContext<DLIconButton, T, S> onClick) {
        if (wasBuild) {
            throw new IllegalStateException("Cannot add elements to this widget after finishing creation.");
        }
        DLIconButton btn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, icon, x() + width() - DLIconButton.DEFAULT_BUTTON_WIDTH - buttonsXOffset - CONTENT_POS_RIGHT, y() + 1, TextUtils.empty(),
        (b) -> {
            onClick.run(b, this.parent.getData(), data, (newData) -> newData.ifPresent(a -> this.parent.displayData(a)));
        }));
        btn.setBackColor(0x00000000);
        buttonsXOffset += btn.width();
        return btn;
    }

    public DLIconButton addDeleteButton(DataListEntryContext<DLIconButton, T, S> onClick) {
        return addButton(ModGuiIcons.DELETE.getAsSprite(16, 16), onClick);
    }

    protected E createSection(E section) {
        if (wasBuild) {
            throw new IllegalStateException("Cannot add elements to this widget after finishing creation.");
        }
        sections.add(section);
        sectionsXOffset += section.width + CONTENT_SPACING;
        return section;
    }

    /**
     * Called after creating this entry. Can be used to create edit boxes for editable sections and more.
     */
    protected abstract void build();
    public final void buildInternal() {
        wasBuild = true;
        build();
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderWidgetBase(graphics, mouseX, mouseY, partialTicks);
        for (E section : sections) {
            int xCoord = x() + width() - CONTENT_POS_RIGHT - CONTENT_SPACING - buttonsXOffset - section.xOffset - section.width;
            renderSection(graphics, mouseX, mouseY, partialTicks, section, new GuiAreaDefinition(xCoord, y() + 1, section.width, 18));
        }
        int remainingWidth = width() - CONTENT_POS_LEFT - CONTENT_POS_RIGHT - CONTENT_SPACING - buttonsXOffset - sectionsXOffset;
        int xCoord = x() + CONTENT_POS_LEFT;
        renderMainSection(graphics, mouseX, mouseY, partialTicks, text, new GuiAreaDefinition(xCoord, y() + 1, remainingWidth, 18));
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
    }

    protected abstract void renderWidgetBase(Graphics graphics, int mouseX, int mouseY, float partialTicks);
    protected abstract void renderSection(Graphics graphics, int mouseX, int mouseY, float partialTicks, E section, GuiAreaDefinition area);
    protected abstract void renderMainSection(Graphics graphics, int mouseX, int mouseY, float partialTicks, String text, GuiAreaDefinition area);

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

    @FunctionalInterface
    public static interface DataListEntryContext<B extends DLButton, T, S> {
        void run(B btn, T data, S entry, Consumer<Optional<T>> refreshAction);
    }

    public static abstract class AbstractDataSectionDefinition<T, S> {
        public final int xOffset;
        public final int width;

        public AbstractDataSectionDefinition(int xOffset, int width) {
            this.xOffset = xOffset;
            this.width = width;
        }
    }
}
