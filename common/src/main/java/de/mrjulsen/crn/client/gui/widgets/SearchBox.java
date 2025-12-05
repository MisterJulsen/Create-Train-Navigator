package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.render.IStateRenderer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;

public class SearchBox extends DLRichTextEditBox {

    public static class SearchBoxRenderer implements IStateRenderer<TextBoxState> {

        public static final SearchBoxRenderer INSTANCE = new SearchBoxRenderer(false);

        private final boolean light;

        public SearchBoxRenderer(boolean light) {
            this.light = light;
        }

        @Override
        public void renderSprite(DLGuiGraphics graphics, int x, int y, int w, int h, DLGuiComponent component, TextBoxState state) {
            GuiUtils.fill(graphics, 1, 1, w - 1, h - 1, DLColor.fromInt(light ? 0xFF303030 : 0xFF505050));
            GuiUtils.fill(graphics, 0, 0, w - 1, h - 1, DLColor.fromInt(light ? 0xFF505050 : 0xFF303030));
            ModGuiIcons.SEARCH.render(graphics, 0, h / 2 - ModGuiIcons.ICON_SIZE / 2);
        }
        
    }

    public SearchBox(int x, int y, int w) {
        super(x, y, w, 14);
        this.componentRenderer.set(SearchBoxRenderer.INSTANCE);
        this.contentPadding.set(new Padding(0, 1, 0, 18));
        this.acceptAndCancelKeysEnabled.set(true);
        this.placeholderText.set(Constants.TEXT_SEARCH);        
    }
}
