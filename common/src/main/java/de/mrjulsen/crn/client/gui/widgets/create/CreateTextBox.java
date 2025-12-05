package de.mrjulsen.crn.client.gui.widgets.create;

import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.TextStyle;
import net.minecraft.client.Minecraft;

public class CreateTextBox extends DLRichTextEditBox {

    public CreateTextBox(int x, int y, int w) {
        super(x, y, w, 18);
        this.text.get().setFormat(0, text.get().length(), new TextStyle(Minecraft.getInstance().font, false, false, false, false, false, 0xFFFFFFFF, true, 1, 0));
        this.componentRenderer.set(CreateTextBoxComponentRenderer.INSTANCE);
        contentPadding.set(new Padding(0, 4, 0, 4));
    } 
}
