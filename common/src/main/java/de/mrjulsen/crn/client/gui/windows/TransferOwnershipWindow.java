package de.mrjulsen.crn.client.gui.windows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.simibubi.create.foundation.gui.AllIcons;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.flyout.FlyoutConfirmDialog;
import de.mrjulsen.crn.client.gui.widgets.IconSlotWidget;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.client.gui.widgets.create.CreateItemPicker;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.util.Lock;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineLabel;

public class TransferOwnershipWindow extends DLWindow {    

    private static final int GUI_WIDTH = 250;
    private static final int GUI_HEIGHT = 120;
    
    private static final FooterSize headerSize = FooterSize.DEFAULT;
    private static final FooterSize footerSize = FooterSize.SMALL;
    
    private final Owner current;
    private final List<Owner> playerList = new ArrayList<>();
    private final MultiLineLabel messageLabel;
    private final IconSlotWidget iconWidget;
    private final CreateItemPicker<Owner> selectionBox;

    public TransferOwnershipWindow(DLWindowManager manager, Owner selected, Consumer<Owner> okAction) {
        super(manager);
        this.setSize(GUI_WIDTH, GUI_HEIGHT);
        this.movable.set(true);
        this.windowSpawnPosition.set(WindowPosition.PARENT_CENTER);
        this.current = selected;
        
        Rectangle workingArea = Rectangle.withSize(1, headerSize.size(), width() - 2, height() - headerSize.size() - footerSize.size());
        this.iconWidget = addComponent(new IconSlotWidget((int)workingArea.x() + 16, (int)workingArea.y() + 8));
        this.iconWidget.icon.set(ModGuiIcons.USER.getAsSprite(16, 16));

        this.selectionBox = addComponent(new CreateItemPicker<>(iconWidget.x() + iconWidget.width() + 6, iconWidget.y(), width() - (iconWidget.x() + iconWidget.width()) + x() - 16 - 6));
        this.selectionBox.enabled.set(false);
        this.selectionBox.renderArrow.set(true);
        this.selectionBox.filter.set((i, s) -> i.name().toLowerCase().contains(s.toLowerCase()));
        this.selectionBox.formatter.set((i) -> i == null ? TextUtils.empty() : TextUtils.text(i.name()));
        
        CreateButton okBtn = addComponent(new CreateButton(width() - 17 - CreateButton.WIDTH * 2, height() - 6 - CreateButton.HEIGHT, AllIcons.I_CONFIRM));
        okBtn.enabled.set(false);
        okBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal((mgr) -> new FlyoutConfirmDialog(mgr, okBtn, FlyoutPointer.RIGHT, ColorShade.DARK, () -> {
                DLUtils.doIfNotNull(okAction, x -> x.accept(playerList.get(this.selectionBox.selectedIndex.get())));
                getAssignedModal().ifPresent(m -> getWindowManager().closeModal(m));
            }));
            return false;
        });
        
        CreateButton cancelBtn = addComponent(new CreateButton(width() - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT, AllIcons.I_MTD_CLOSE));
        cancelBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getAssignedModal().ifPresent(m -> getWindowManager().closeModal(m));
            return false;
        });

        this.messageLabel = MultiLineLabel.create(Minecraft.getInstance().font, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".lock.transfer_ownership.warning").withStyle(ChatFormatting.GOLD), (int)(selectionBox.width() / 0.75f));

        

        ModNetworkManager.GET_ONLINE_PLAYERS.send(NetworkDirection.toServer(), (response) -> {
            this.playerList.add(new Owner());
            this.playerList.addAll(response.getPlayers().stream().collect(Collectors.toSet()));
            this.selectionBox.items.set(playerList);
            this.selectionBox.selectedIndex.set(MathUtils.clamp(current == null ? 0 : playerList.indexOf(current), 0, playerList.size() - 1));
            okBtn.enabled.set(true);
            this.selectionBox.enabled.set(true);
        }, () -> {});
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        Rectangle workingArea = Rectangle.withSize(1, headerSize.size(), width() - 2, height() - headerSize.size() - footerSize.size());
     
        CreateDynamicWidgets.renderWindow(graphics, 0, 0, width(), height(), ContainerColor.PURPLE, BarColor.GOLD, BarColor.GRAY, headerSize.size(), footerSize.size(), false);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, width() - 31, height() - footerSize.size() + 2, footerSize.size() - 4, BarColor.GRAY);
        CreateDynamicWidgets.renderContainer(graphics, (int)workingArea.x(), (int)workingArea.y() - 1, (int)workingArea.width(), (int)workingArea.height() + 2, ContainerColor.PURPLE);

        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, 4, TextUtils.translate(Lock.TRANSLATION_KEY_TRANSFER_OWNERSHIP), DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.LEFT, false);
        ModGuiIcons.WARN.render(graphics, iconWidget.x(), iconWidget.y() + iconWidget.height() + 8);
        
        if (selectionBox != null) {
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(selectionBox.x(), selectionBox.y() + selectionBox.height() + 8, 0);
            graphics.poseStack().scale(0.75f, 0.75f, 0.75f);        
            DLUtils.doIfNotNull(messageLabel, x -> x.renderLeftAligned(graphics.graphics(), 0, 0, graphics.defaultFont().lineHeight, 0xFFFFFFFF));
            graphics.poseStack().popPose();
        }
    }
}
