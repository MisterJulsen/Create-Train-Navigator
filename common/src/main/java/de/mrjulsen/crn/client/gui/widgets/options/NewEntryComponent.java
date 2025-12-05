package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.List;
import java.util.function.Consumer;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTextBox;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.DLAbstractRichTextInputField;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;

public class NewEntryComponent extends DLGuiComponent {

    public NewEntryComponent(int x, int y, int w, Consumer<String> onAccept) {
        super(x, y, w, 26);
        FlatIconButton addBtn = addComponent(new FlatIconButton(0, height() / 2 - FlatIconButton.HEIGHT / 2, ModGuiIcons.ADD.getAsSprite(16, 16)) {
            @Override
            public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
                CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, width(), height(), ColorShade.DARK);
                super.renderMainLayer(graphics, mouseX, mouseY, renderBounds);
            }
        });
        

        addBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            InputArea inputArea = addComponent(new InputArea(0, 0, width(), height(), onAccept));
            inputArea.anchor.set(EAlign.values());
            return false;
        });

    }

    private static final class InputArea extends DLGuiComponent {
        
        private final MutableComponent textAdd = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".new_entry.add");
        private final MutableComponent textNew = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".new_entry.new");

        public InputArea(int x, int y, int w, int h, Consumer<String> onAccept) {
            super(x, y, w, h);

            FlatIconButton declineBtn = addComponent(new FlatIconButton(width() - 4 - FlatIconButton.WIDTH, 4, ModGuiIcons.CROSS.getAsSprite(16, 16)));
            declineBtn.anchor.set2(EAlign.RIGHT);
            declineBtn.tooltip.set(new DLTooltip(List.of(CommonComponents.GUI_CANCEL), 200));
            FlatIconButton acceptBtn = addComponent(new FlatIconButton(width() - 6 - FlatIconButton.WIDTH * 2, 4, ModGuiIcons.CHECK.getAsSprite(16, 16)));
            acceptBtn.anchor.set2(EAlign.RIGHT);
            acceptBtn.tooltip.set(new DLTooltip(List.of(textAdd), 200));
            int i = 10 + Minecraft.getInstance().font.width(textNew);
            CreateTextBox inputBox = addComponent(new CreateTextBox(i, 4, width() - i - 8 - FlatIconButton.WIDTH * 2));
            inputBox.acceptAndCancelKeysEnabled.set(true);
            inputBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
                onAccept.accept(inputBox.text.get().getPlainText());
                this.getParent().ifPresent(a -> a.removeComponent(this));
                return false;
            });

            declineBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                this.getParent().ifPresent(a -> a.removeComponent(this));
                return false;
            });
            acceptBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
                onAccept.accept(inputBox.text.get().getPlainText());
                this.getParent().ifPresent(a -> a.removeComponent(this));
                return false;
            });

            addEventListener(DLGuiStandardEvents.WindowManagerChangeEvent.class, (s, e) -> {                
                if (e.newWindowManager() != null) {
                    e.newWindowManager().focusComponent(inputBox);
                }
                return false;
            });
        }

        @Override
        public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
            CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, width(), height(), ColorShade.DARK);
            GuiUtils.drawString(graphics, graphics.defaultFont(), 5, height() / 2 - graphics.defaultFont().lineHeight / 2, textNew, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        }
        
    }
}
