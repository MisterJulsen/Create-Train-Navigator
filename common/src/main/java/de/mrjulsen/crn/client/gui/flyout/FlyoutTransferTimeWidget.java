package de.mrjulsen.crn.client.gui.flyout;

import java.util.function.Supplier;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateTimeSelectionComponent;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.UserSettings.UserSetting;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

public class FlyoutTransferTimeWidget extends AbstractFlyoutWidget {

    private final MutableComponent textDepartureIn = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.transfer_time").withStyle(ChatFormatting.BOLD);
    private final UserSettings settings;

    private final CreateTimeSelectionComponent timeSelection;
    private final Supplier<UserSetting<Integer>> getUserSetting;

    public FlyoutTransferTimeWidget(DLWindowManager manager, DLGuiComponent parentComponent, FlyoutPointer pointer, ColorShade pointerShade, UserSettings settings, Supplier<UserSetting<Integer>> getUserSetting) {
        super(manager, parentComponent, 1, 60, pointer, pointerShade);
        setWidth(Math.max(FlatIconButton.WIDTH, Minecraft.getInstance().font.width(textDepartureIn) + FlatIconButton.WIDTH + 20 + FlyoutPointer.WIDTH * 2));
        this.settings = settings;
        this.getUserSetting = getUserSetting;

        this.timeSelection = addComponent(new CreateTimeSelectionComponent((int)contentArea.get().x() + 8, (int)contentArea.get().y() + 24, 60));

        FlatIconButton resetBtn = addComponent(new FlatIconButton((int)contentArea.get().right() - FlatIconButton.WIDTH - 2, (int)contentArea.get().top() + 2, ModGuiIcons.REFRESH.getAsSprite(16, 16)));
        resetBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getUserSetting.get().setToDefault();
            timeSelection.value.set((double)getUserSetting.get().getValue());            
            return false;
        });
    }

    @Override
    public void renderFlyoutContent(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle contentArea) {
        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)contentArea.x() + 8, (int)contentArea.y() + 8, textDepartureIn, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
    }

    @Override
    protected void onOpen() {
        this.timeSelection.value.set((double)getUserSetting.get().getValue());
    }
    
    @Override
    protected void onClose() {        
        DLUtils.doIfNotNull(settings, x -> {
            getUserSetting.get().setValue(timeSelection.value.get().intValue());
            x.clientSave(super::close);
        });
    }
    
}
