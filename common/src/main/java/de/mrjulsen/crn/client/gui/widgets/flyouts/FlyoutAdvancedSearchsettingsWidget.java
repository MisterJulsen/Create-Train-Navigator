package de.mrjulsen.crn.client.gui.widgets.flyouts;

import java.util.function.Consumer;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.MutableComponent;

public class FlyoutAdvancedSearchsettingsWidget<T extends GuiEventListener & Widget & NarratableEntry> extends AbstractFlyoutWidget<T> {

    private final MutableComponent textTrainGroups = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.advanced_options").withStyle(ChatFormatting.BOLD);
    //private UserSettings settings;

    public FlyoutAdvancedSearchsettingsWidget(DLScreen screen, FlyoutPointer pointer, ColorShade pointerShade, Consumer<T> addRenderableWidgetFunc, Consumer<GuiEventListener> removeWidgetFunc) {
        super(screen, 1, 120, pointer, pointerShade, addRenderableWidgetFunc, removeWidgetFunc);
        set_width(Math.max(150, font.width(textTrainGroups) + DLIconButton.DEFAULT_BUTTON_WIDTH + 16 + 10 + FlyoutPointer.WIDTH * 2));

        //int top = getContentArea().getY() + 21;
        //int contentHeight = getContentArea().getHeight() - 21 - 2;
        
        /*
        addRenderableWidget(new SearchOptionButton(getContentArea().getX() + 2, top, getContentArea().getWidth() - 4, 18, TextUtils.text("Train Groups"), () -> "Here be Dragons!", (b) -> {
            new FlyoutTrainGroupsWidget<>(screen, FlyoutPointer.UP, ColorShade.DARK, addRenderableWidgetFunc, (settings) -> {
                return settings.navigationExcludedTrainGroups;
            }, (w) -> {
                removeWidgetFunc.accept(w);
            }).open(b);
        }));
        */

        DLIconButton resetBtn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.REFRESH.getAsSprite(16, 16), getContentArea().getX() + getContentArea().getWidth() - DLIconButton.DEFAULT_BUTTON_WIDTH - 2, getContentArea().getY() + 2, TextUtils.empty(), (b) -> {
            
        }));
        
        resetBtn.setBackColor(0x00000000);
    }

    @Override
    public void renderFlyoutContent(Graphics graphics, int mouseX, int mouseY, float partialTicks, GuiAreaDefinition contentArea) {
        super.renderFlyoutContent(graphics, mouseX, mouseY, partialTicks, contentArea);
        GuiUtils.drawString(graphics, font, contentArea.getX() + 8, contentArea.getY() + 8, textTrainGroups, DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
    }

    @Override
    public void close() {
        super.close();
    }
}
