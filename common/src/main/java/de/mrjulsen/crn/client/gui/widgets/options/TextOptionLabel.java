package de.mrjulsen.crn.client.gui.widgets.options;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLEditableLabel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.DLAbstractRichTextInputField;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

public class TextOptionLabel extends DLEditableLabel {

    public TextOptionLabel() {
        super(0, 0, 1, 18);
        this.padding.set(new Padding(0, 5, 0, 5));
        this.editable.set(false);
        this.editBox.acceptAndCancelKeysEnabled.set(true);
        this.editBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
            if (isEditing) {
                this.isEditing = false;
                this.text.set(this.editBox.text.get().getPlainText());
                this.editBox.visible.set(false);
                invokeEvent(this, new EditModeChangedEvent(false));
                invokeEvent(this, new TextEditedEvent(this.text.get()));
            }
            return false;
        });

        this.editable.withAfterPropertyChangedCallback((o, n) -> {
            this.cursor.set(n ? CursorType.HAND : null);
        });
    }
    
    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderTextSlotOverlay(graphics, 0, 0, width(), height());
        if (editable.get() && isSelected()) {
            GuiUtils.fill(graphics, 0, 0, width(), height(), DLColor.fromInt(0x22FFFFFF));
        }
        GuiUtils.drawString(graphics, graphics.defaultFont(), padding.get().left(), height() / 2 - graphics.defaultFont().lineHeight / 2, TextUtils.truncateWithEllipsis(graphics.defaultFont(), text.get(), width() - padding.get().left() - padding.get().right()), textColor.get(), ETextAlignment.LEFT, drawFontShadow.get());
    }
}
