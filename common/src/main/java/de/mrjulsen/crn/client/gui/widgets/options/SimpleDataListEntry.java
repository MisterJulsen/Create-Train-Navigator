package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.widgets.options.AbstractDataListEntry.AbstractDataSectionDefinition;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;

public class SimpleDataListEntry<T, S> extends AbstractDataListEntry<T, S, SimpleDataListEntry.DisplayableDataSectionDefinition<T, S>> {    

    public SimpleDataListEntry(DataListContainer<T, S> parent, int x, int y, int width, S data) {
        super(parent, x, y, width, data);
    }
    
    /**
     * Adds a new data section.
     * @param width The width of the sections
     * @param displayName The display text of the section.
     * @param onEdit Called when editing this value. {@code null} if this value should not be editable.
     */
    public void addDataSection(int width, Function<S, String> displayName, EAlignment alignment, DataListEntryEditContext<T, S> onEdit) {
        createSection(new DisplayableDataSectionDefinition<>(getCurrentSectionsXOffset(), width, displayName.apply(data), alignment, onEdit));
    }

    @Override
    protected void build() {        
        for (DisplayableDataSectionDefinition<T, S> section : getSections()) {
            if (section.onEdit == null) {
                continue;
            }

            final DisplayableDataSectionDefinition<T, S> dataSection = section;
            int xCoord = x() + width() - CONTENT_POS_RIGHT - CONTENT_SPACING - getCurrentButtonsXOffset() - section.xOffset - section.width;
            DLEditBox modifyPlatformInput = addRenderableWidget(new DLEditBox(font, xCoord + 1, y() + 2, section.width - 2, height() - 4, TextUtils.empty()));
            DLButton modifyPlatformBtn = addRenderableWidget(new DLButton(xCoord, y() + 1, section.width, height() - 2, TextUtils.empty(),
            (b) -> {
                b.set_visible(false);
                modifyPlatformInput.setValue(dataSection.displayName);
                modifyPlatformInput.set_visible(true);
            }));
            modifyPlatformBtn.setRenderStyle(AreaStyle.FLAT);
            modifyPlatformBtn.setBackColor(0x00000000);

            modifyPlatformInput.setValue("");
            modifyPlatformInput.set_visible(false);
            modifyPlatformInput.withOnFocusChanged((box, focus) -> {
                if (box.visible() && !focus) {
                    dataSection.onEdit.run(parent.getData(), data, box.getValue(), (newData) -> newData.ifPresent(a -> this.parent.displayData(a)));
                    modifyPlatformInput.setValue("");
                    modifyPlatformInput.set_visible(false);
                    modifyPlatformBtn.set_visible(true);
                }
            });
        }
    }

    @Override
    protected void renderWidgetBase(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void renderSection(Graphics graphics, int mouseX, int mouseY, float partialTicks, DisplayableDataSectionDefinition<T, S> section, GuiAreaDefinition area) {
        CreateDynamicWidgets.renderTextSlotOverlay(graphics, area.getX(), area.getY(), area.getWidth(), area.getHeight());
        switch (section.alignment) {
            case RIGHT:
                GuiUtils.drawString(graphics, font, area.getX() + area.getWidth() - TEXT_OFFSET, area.getY() + area.getHeight() / 2 - font.lineHeight / 2, GuiUtils.ellipsisString(font, TextUtils.text(section.displayName), area.getWidth() - TEXT_OFFSET * 2), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.RIGHT, false);
                break;
            case CENTER:
                GuiUtils.drawString(graphics, font, area.getX() + TEXT_OFFSET + (area.getWidth() - TEXT_OFFSET * 2) / 2, area.getY() + area.getHeight() / 2 - font.lineHeight / 2, GuiUtils.ellipsisString(font, TextUtils.text(section.displayName), area.getWidth() - TEXT_OFFSET * 2), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.CENTER, false);
                break;
            default:
            case LEFT:
                GuiUtils.drawString(graphics, font, area.getX() + TEXT_OFFSET, area.getY() + area.getHeight() / 2 - font.lineHeight / 2, GuiUtils.ellipsisString(font, TextUtils.text(section.displayName), area.getWidth() - TEXT_OFFSET * 2), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
                break;
        }
    }

    @Override
    protected void renderMainSection(Graphics graphics, int mouseX, int mouseY, float partialTicks, String text, GuiAreaDefinition area) {
        CreateDynamicWidgets.renderTextSlotOverlay(graphics, area.getX(), area.getY(), area.getWidth(), area.getHeight());
        GuiUtils.drawString(graphics, font, area.getX() + TEXT_OFFSET, area.getY() + area.getHeight() / 2 - font.lineHeight / 2, GuiUtils.ellipsisString(font, TextUtils.text(text), area.getWidth() - TEXT_OFFSET * 2), DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
    }

    

    @FunctionalInterface
    public static interface DataListEntryEditContext<T, S> {
        void run(T data, S entry, String newValue, Consumer<Optional<T>> refreshAction);
    }

    public static class DisplayableDataSectionDefinition<T, S> extends AbstractDataSectionDefinition<T, S> {
        private final String displayName;
        private final EAlignment alignment;
        private final DataListEntryEditContext<T, S> onEdit;

        public DisplayableDataSectionDefinition(int xOffset, int width, String displayName, EAlignment alignment, DataListEntryEditContext<T, S> onEdit) {
            super(xOffset, width);
            this.displayName = displayName;
            this.alignment = alignment;
            this.onEdit = onEdit;
        }
    }
}
