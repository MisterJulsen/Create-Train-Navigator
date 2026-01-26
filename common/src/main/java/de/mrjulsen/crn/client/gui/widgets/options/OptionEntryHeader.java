package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.List;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.Property;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class OptionEntryHeader extends DLButton {

    public final Property<DLSprite> icon = new Property<DLSprite>(GuiIcons.ARROW_DOWN.getAsSprite(16, 16));

    private final Component caption;
    private int buttonsWidth = 0;

    public OptionEntryHeader(Component caption, List<FormattedText> description) {
        super(0, 0, 100, 20);
        this.anchor.set2(EAlign.TOP, EAlign.LEFT, EAlign.RIGHT);
        this.cursor.set(CursorType.HAND);
        this.caption = caption;
        this.tooltip.set(new DLTooltip(description, 200));
    }
    
    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, width(), height(), ColorShade.DARK);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, height() / 2 - graphics.defaultFont().lineHeight / 2, TextUtils.truncateWithEllipsis(graphics.defaultFont(), caption, width() - buttonsWidth - 32), isSelected() ? DragonLib.VANILLA_BUTTON_HIGHLIGHTED_FONT_COLOR : DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        if (isSelected()) {
            GuiUtils.fill(graphics, 0, 0, width(), height(), DLColor.fromInt(0x22FFFFFF));
        }

        icon.get().render(graphics, width() - 2 - ModGuiIcons.ICON_SIZE, 2);
    }    
}
