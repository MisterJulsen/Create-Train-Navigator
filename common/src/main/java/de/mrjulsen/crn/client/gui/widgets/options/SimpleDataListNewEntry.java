package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.widgets.options.AbstractDataListEntry.AbstractDataSectionDefinition;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.components.EditBox;

public class SimpleDataListNewEntry<T, S> extends AbstractDataListEntry<T, S, SimpleDataListNewEntry.InputDataSectionDefinition<T, S>> { 
    
    public static final String MAIN_INPUT_KEY = "name";

    public Map<String, Supplier<String>> values = new HashMap<>();

    private Consumer<DLEditBox> onCreateMainEditBox;

    public SimpleDataListNewEntry(DataListContainer<T, S> parent, int x, int y, int width) {
        super(parent, x, y, width, null);
    }
    
    /**
     * Adds a new data section.
     * @param width The width of the sections
     * @param displayName The display text of the section.
     * @param onEdit Called when editing this value. {@code null} if this value should not be editable.
     */
    public void addDataSection(int width, String key, Consumer<DLEditBox> onCreateEditBox) {
        createSection(new InputDataSectionDefinition<>(getCurrentSectionsXOffset(), width, key, onCreateEditBox));
    }

    public void addAddButton(Sprite icon, DataListAddNewEntryContext<DLIconButton, T> onClick) {
        addButton(icon,
        (btn, data, entry, refreshAction) -> {
            if (onClick.run(btn, parent.getData(), ImmutableMap.copyOf(values), refreshAction)) {
                children().stream().filter(x -> x instanceof EditBox).map(x -> (EditBox)x).forEach(x -> x.setValue(""));
            }
        });
    }

    public void editNameEditBox(Consumer<DLEditBox> onCreateMainEditBox) {
        this.onCreateMainEditBox = onCreateMainEditBox;
    }

    @Override
    protected void build() {
        values.clear();
        for (InputDataSectionDefinition<T, S> section : getSections()) {
            int xCoord = x() + width() - CONTENT_POS_RIGHT - CONTENT_SPACING - getCurrentButtonsXOffset() - section.xOffset - section.width;
            DLEditBox modifyPlatformInput = addRenderableWidget(new DLEditBox(font, xCoord + 4, y() + 5, section.width - 8, height() - 10, TextUtils.empty()));
            modifyPlatformInput.setValue("");
            modifyPlatformInput.setBordered(false);
            DLUtils.doIfNotNull(section.onCreateEditBox, x -> x.accept(modifyPlatformInput));
            values.put(section.key, () -> modifyPlatformInput.getValue());
        }

        int remainingWidth = width() - CONTENT_POS_LEFT - CONTENT_POS_RIGHT - CONTENT_SPACING - getCurrentButtonsXOffset() - getCurrentSectionsXOffset();
        int xCoord = x() + CONTENT_POS_LEFT;
        DLEditBox modifyPlatformInput = addRenderableWidget(new DLEditBox(font, xCoord + 4, y() + 5, remainingWidth - 8, height() - 10, TextUtils.empty()));
        modifyPlatformInput.setValue("");
        modifyPlatformInput.setBordered(false);
        DLUtils.doIfNotNull(onCreateMainEditBox, x -> x.accept(modifyPlatformInput));
        values.put(MAIN_INPUT_KEY, () -> modifyPlatformInput.getValue());
    }

    @Override
    protected void renderWidgetBase(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void renderSection(Graphics graphics, int mouseX, int mouseY, float partialTicks, InputDataSectionDefinition<T, S> section, GuiAreaDefinition area) {
        CreateDynamicWidgets.renderTextBox(graphics, area.getX(), area.getY(), area.getWidth());
    }

    @Override
    protected void renderMainSection(Graphics graphics, int mouseX, int mouseY, float partialTicks, String text, GuiAreaDefinition area) {
        CreateDynamicWidgets.renderTextBox(graphics, area.getX(), area.getY(), area.getWidth());
    }

    
    @FunctionalInterface
    public static interface DataListAddNewEntryContext<B extends DLButton, T> {
        boolean run(B btn, T data, ImmutableMap<String, Supplier<String>> inputValues, Consumer<Optional<T>> refreshAction);
    }

    public static class InputDataSectionDefinition<T, S> extends AbstractDataSectionDefinition<T, S> {
        private final String key;
        private final Consumer<DLEditBox> onCreateEditBox;

        public InputDataSectionDefinition(int xOffset, int width, String key, Consumer<DLEditBox> onCreateEditBox) {
            super(xOffset, width);
            this.key = key;
            this.onCreateEditBox = onCreateEditBox;
        }
    }
}
