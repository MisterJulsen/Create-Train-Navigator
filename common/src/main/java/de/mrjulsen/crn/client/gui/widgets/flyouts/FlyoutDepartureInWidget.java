package de.mrjulsen.crn.client.gui.widgets.flyouts;

import java.util.function.Consumer;
import java.util.function.Supplier;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTimeSelectionWidget;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.UserSettings.UserSetting;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.MutableComponent;

public class FlyoutDepartureInWidget<T extends GuiEventListener & Renderable & NarratableEntry> extends AbstractFlyoutWidget<T> {

    private final MutableComponent textDepartureIn = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.departure_in").withStyle(ChatFormatting.BOLD);
    private final UserSettings settings;

    private final CreateTimeSelectionWidget timeSelection;
    private final Supplier<UserSetting<Integer>> getUserSetting;

    public FlyoutDepartureInWidget(DLScreen screen, FlyoutPointer pointer, ColorShade pointerShade, Consumer<T> addRenderableWidgetFunc, UserSettings settings, Supplier<UserSetting<Integer>> getUserSetting, Consumer<GuiEventListener> removeWidgetFunc) {
        super(screen, 1, 60, pointer, pointerShade, addRenderableWidgetFunc, removeWidgetFunc);
        set_width(Math.max(CreateTimeSelectionWidget.WIDHT + 16, font.width(textDepartureIn) + DLIconButton.DEFAULT_BUTTON_WIDTH + 16 + 10 + FlyoutPointer.WIDTH * 2));
        this.settings = settings;
        this.getUserSetting = getUserSetting;

        this.timeSelection = addRenderableWidget(new CreateTimeSelectionWidget(getContentArea().getX() + 8, getContentArea().getY() + 24, DragonLib.TICKS_PER_DAY * 10));
        DLIconButton resetBtn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.REFRESH.getAsSprite(16, 16), getContentArea().getX() + getContentArea().getWidth() - DLIconButton.DEFAULT_BUTTON_WIDTH - 2, getContentArea().getY() + 2, TextUtils.empty(), (b) -> {
            getUserSetting.get().setToDefault();
            timeSelection.setValue(getUserSetting.get().getValue());
        }));
        
        resetBtn.setBackColor(0x00000000);

    }

    @Override
    public void renderFlyoutContent(Graphics graphics, int mouseX, int mouseY, float partialTicks, GuiAreaDefinition contentArea) {
        super.renderFlyoutContent(graphics, mouseX, mouseY, partialTicks, contentArea);
        GuiUtils.drawString(graphics, font, contentArea.getX() + 8, contentArea.getY() + 8, textDepartureIn, DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
    }

    @SuppressWarnings("resource")
    @Override
    public void open(IDragonLibWidget parent) {
        this.timeSelection.setValue(getUserSetting.get().getValue());
        super.open(parent);
    }

    @Override
    public void close() {
        DLUtils.doIfNotNull(settings, x -> {
            getUserSetting.get().setValue(timeSelection.getValue());
            x.clientSave(super::close);
        });
    }
    
}
