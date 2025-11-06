package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.ScrollInput;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget.FlyoutPointer;
import de.mrjulsen.crn.client.gui.widgets.flyouts.FlyoutConfirmDialog;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.util.Lock;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLWidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class TransferOwnershipWidget<T extends GuiEventListener & Renderable & NarratableEntry> extends DLWidgetContainer {

    private static final int GUI_WIDTH = 250;
    private static final int GUI_HEIGHT = 120;
    
    private static final FooterSize headerSize = FooterSize.DEFAULT;
    private static final FooterSize footerSize = FooterSize.SMALL;

    private final Consumer<GuiEventListener> removeWidgetFunc;


    private final DLScreen parent;
    private final Owner current;
    private final List<Owner> playerList = new ArrayList<>();
    private MultiLineLabel messageLabel;
    private IconSlotWidget icon;
    private ScrollInput selectionBox;
    private int selectedPlayerIndex = 0;

    public TransferOwnershipWidget(DLScreen parent, Owner selected, Consumer<Owner> okAction, Consumer<T> addRenderableWidgetFunc, Consumer<GuiEventListener> removeWidgetFunc) {
        super(parent.width() / 2 - GUI_WIDTH / 2, parent.height() / 2 - GUI_HEIGHT / 2, GUI_WIDTH, GUI_HEIGHT);
        this.parent = parent;  
        this.current = selected;
        this.removeWidgetFunc = removeWidgetFunc;
        parent.setAllowedLayer(parent.getAllowedLayer() + 1);
        setWidgetLayerIndex(parent.getAllowedLayer());

        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ONLINE_PLAYERS, (players) -> {
            this.playerList.add(new Owner());
            this.playerList.addAll(players.stream().collect(Collectors.toSet()));
            this.selectedPlayerIndex = MathUtils.clamp(current == null ? 0 : playerList.indexOf(current), 0, playerList.size() - 1);
            GuiAreaDefinition workingArea = new GuiAreaDefinition(x() + 1, y() + headerSize.size(), width() - 2, height() - headerSize.size() - footerSize.size());
            this.icon = addRenderableOnly(new IconSlotWidget(workingArea.getX() + 16, workingArea.getY() + 8, ModGuiIcons.USER.getAsSprite(16, 16)));
            this.selectionBox = addRenderableWidget(new DLCreateSelectionScrollInput(parent, icon.x() + icon.width() + 6, icon.y(), width() - (icon.x() + icon.width()) + x() - 16 - 6, 18)
                .setRenderArrow(true)
                .forOptions(playerList.stream().map(x -> TextUtils.text(x.name())).toList())
                .setState(selectedPlayerIndex)
                .calling((i) -> {
                    this.selectedPlayerIndex = i;
                })
            );
        
            DLCreateIconButton okBtn = this.addRenderableWidget(new DLCreateIconButton(x() + width() - 17 - DLIconButton.DEFAULT_BUTTON_WIDTH * 2, y() + height() - 6 - DLIconButton.DEFAULT_BUTTON_HEIGHT, DLIconButton.DEFAULT_BUTTON_WIDTH, DLIconButton.DEFAULT_BUTTON_HEIGHT, AllIcons.I_CONFIRM));
            okBtn.withCallback(() -> {
                FlyoutConfirmDialog<?> dlg = new FlyoutConfirmDialog<>(parent, FlyoutPointer.RIGHT, () -> {
                    DLUtils.doIfNotNull(okAction, x -> x.accept(playerList.get(selectedPlayerIndex)));
                    close();
                }, addRenderableWidgetFunc, (w) -> {
                    removeWidgetFunc.accept(w);   
                });
                dlg.open(okBtn);
            });
            this.messageLabel = MultiLineLabel.create(font, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".lock.transfer_ownership.warning").withStyle(ChatFormatting.GOLD), (int)(selectionBox.getWidth() / 0.75f));

        });
        
        DLCreateIconButton cancelBtn = this.addRenderableWidget(new DLCreateIconButton(x() + width() - 7 - DLIconButton.DEFAULT_BUTTON_WIDTH, y() + height() - 6 - DLIconButton.DEFAULT_BUTTON_HEIGHT, DLIconButton.DEFAULT_BUTTON_WIDTH, DLIconButton.DEFAULT_BUTTON_HEIGHT, AllIcons.I_MTD_CLOSE));
        cancelBtn.withCallback(() -> {
            close();
        });
    }

    

    public void close() {
        parent.setAllowedLayer(getWidgetLayerIndex() - 1);
        removeWidgetFunc.accept(this);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        parent.renderScreenBackground(graphics);   
        GuiAreaDefinition workingArea = new GuiAreaDefinition(x() + 1, y() + headerSize.size(), width() - 2, height() - headerSize.size() - footerSize.size());
     
        CreateDynamicWidgets.renderWindow(graphics, x(), y(), width(), height(), ContainerColor.PURPLE, BarColor.GOLD, BarColor.GRAY, headerSize.size(), footerSize.size(), false);
        CreateDynamicWidgets.renderVerticalSeparator(graphics, x() + width() - 31, y() + height() - footerSize.size() + 2, footerSize.size() - 4, BarColor.GRAY);

        CreateDynamicWidgets.renderContainer(graphics, workingArea.getX(), workingArea.getY() - 1, workingArea.getWidth(), workingArea.getHeight() + 2, ContainerColor.PURPLE);
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);

        GuiUtils.drawString(graphics, font, x() + 6, y() + 4, TextUtils.translate(Lock.TRANSLATION_KEY_TRANSFER_OWNERSHIP), DragonLib.NATIVE_UI_FONT_COLOR, ETextAlignment.LEFT, false);
        
        if (icon != null) {
            ModGuiIcons.WARN.render(graphics, icon.x(), icon.y() + icon.height() + 8);
        }
        if (selectionBox != null) {
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(selectionBox.getX(), selectionBox.getY() + selectionBox.getHeight() + 8, 0);
            graphics.poseStack().scale(0.75f, 0.75f, 0.75f);        
            DLUtils.doIfNotNull(messageLabel, x -> x.renderLeftAligned(graphics.graphics(), 0, 0, font.lineHeight, 0xFFFFFFFF));
            graphics.poseStack().popPose();
        }
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) { }

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }
}