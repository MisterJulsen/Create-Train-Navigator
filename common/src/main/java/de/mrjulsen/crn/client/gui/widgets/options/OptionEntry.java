package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.List;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractCollectionComponent.ListLayoutChangedEvent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.events.IEventListener;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class OptionEntry<T> extends DLGuiComponent {

    private final OptionEntryHeader header;
    public final OptionsDataView<T> dataView;
    private final Padding padding = new Padding(10, 5, 10, 20);

    public final BooleanProperty expanded = new BooleanProperty(false);

    int requiredHeight = 0;
    
    public OptionEntry(Component caption, List<FormattedText> description, IEventListener<DLGuiComponent, DLGuiStandardEvents.MousePressedEvent> clickEvent) {
        super(0, 0, 100, 100);
        header = addComponent(new OptionEntryHeader(caption, description));
        header.addEventListener(DLGuiStandardEvents.MousePressedEvent.class, (s, e) -> clickEvent.invoke(this, e));
        setHeight(header.height());
        dataView = new OptionsDataView<>(padding.left(), header.height() + padding.top(), width() - padding.left() - padding.right(), height() - padding.bottom() - header.height());
        dataView.anchor.set(EAlign.values());
        dataView.visible.set(false);
        dataView.addEventListener(ListLayoutChangedEvent.class, (s, e) -> {
            requiredHeight = e.layoutResult().contentHeight();
            if (expanded.get()) {
                setHeight(getRequiredHeight());
                dataView.setHeight(e.layoutResult().contentHeight());
            }
            return false;
        });
        
        expanded.withAfterPropertyChangedCallback((o, n) -> {
            header.icon.set(n ? GuiIcons.ARROW_UP.getAsSprite(16, 16) : GuiIcons.ARROW_DOWN.getAsSprite(16, 16));            
            setHeight(n ? getRequiredHeight() : getHeader().height());
            dataView.visible.set(n);
        });

        addComponent(dataView);
    }

    public int getRequiredHeight() {
        return header.height() + padding.top() + padding.bottom() + requiredHeight;
    }

    public OptionEntryHeader getHeader() {
        return header;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (expanded.get()) {                
            CreateDynamicWidgets.renderSingleShadeWidget(graphics, 1, 1, width() - 2, height() - 2, ColorShade.LIGHT);
            GuiUtils.fillGradient(graphics, 1, header.height(), width() - 2, 10, DLColor.fromInt(0x77000000), DLColor.TRANSPARENT, EAlign.TOP);
        }
    }
    
}
