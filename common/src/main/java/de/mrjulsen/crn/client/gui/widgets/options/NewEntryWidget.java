package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.function.Function;
import java.util.List;
import java.util.function.Supplier;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.DLWidgetsCollection;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;

public class NewEntryWidget extends WidgetContainer {

    private final DLEditBox nameBox;
    private final DLIconButton addBtn;
    private final DLWidgetsCollection collection = new DLWidgetsCollection();
    private final Screen parent;
    private final Supplier<Pair<Double, Double>> scrollOffset;

    private final MutableComponent textAdd = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".new_entry.add");
    private final MutableComponent textNew = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".new_entry.new").append(" ");
    
    private boolean expanded = false;

    public NewEntryWidget(Screen parent, Supplier<Pair<Double, Double>> scrollOffset, Function<String, Boolean> onAcceptNewName, int x, int y, int width) {
        super(x, y, width, 26);
        this.parent = parent;
        this.scrollOffset = scrollOffset;

        int w = width() - 12 - DLIconButton.DEFAULT_BUTTON_WIDTH * 2 - font.width(textNew);
        nameBox = addRenderableWidget(new DLEditBox(font, x() + 10 + font.width(textNew), y() + 9, w - 8, font.lineHeight, TextUtils.empty()));
        nameBox.setBordered(false);
        nameBox.setMaxLength(StationTag.MAX_NAME_LENGTH);

        addBtn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.ADD.getAsSprite(16, 16), x(), y() + height() / 2 - DLIconButton.DEFAULT_BUTTON_WIDTH / 2, TextUtils.empty(),
        (b) -> {
            expanded = true;
            b.set_visible(false);
            collection.setVisible(true);
            nameBox.setValue("");
        }) {
            @Override
            public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
                CreateDynamicWidgets.renderSingleShadeWidget(graphics, x(), y(), width(), height(), ColorShade.DARK);            
                super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
            }
        
        });
        addBtn.setBackColor(0x00000000);

        DLIconButton acceptButton = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.CHECKMARK.getAsSprite(16, 16), x() + width() - DLIconButton.DEFAULT_BUTTON_WIDTH * 2 - 5, y() + 4, TextUtils.empty(),
        (b) -> {
            if (nameBox.getValue() == null || nameBox.getValue().isBlank()) {
                return;
            }
            DLUtils.doIfNotNull(onAcceptNewName, a -> {
                if (a.apply(nameBox.getValue())) {
                    collapse();
                }
            });
        }));
        acceptButton.setBackColor(0x00000000);

        DLIconButton denyButton = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.X.getAsSprite(16, 16), x() + width() - DLIconButton.DEFAULT_BUTTON_WIDTH - 4, y() + 4, TextUtils.empty(),
        (b) -> {
            collapse();
        }));
        denyButton.setBackColor(0x00000000);

        collection.add(nameBox);
        collection.add(acceptButton);
        collection.add(denyButton);
        collection.setVisible(false);
    }

    private void collapse() {
        expanded = false;
        collection.setVisible(false);
        addBtn.set_visible(true);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (expanded) {
            CreateDynamicWidgets.renderSingleShadeWidget(graphics, x(), y(), width(), height(), ColorShade.DARK);
            int w = width() - 12 - DLIconButton.DEFAULT_BUTTON_WIDTH * 2 - font.width(textNew);
            CreateDynamicWidgets.renderTextBox(graphics, x() + 5 + font.width(textNew), y() + 4, w);
            GuiUtils.drawString(graphics, font, x() + 5, y() + height() / 2 - font.lineHeight / 2, textNew, DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
        }
        
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);
        if (addBtn.visible() && addBtn.isMouseSelected()) {            
            GuiUtils.renderTooltipAt(parent, GuiAreaDefinition.of(parent), List.of(textAdd), parent.width / 4, graphics, addBtn.x() + addBtn.width() + 5 + scrollOffset.get().getFirst().intValue(), addBtn.y() + 1 + scrollOffset.get().getSecond().intValue(), mouseX, mouseY, 0, 0);
        }
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
