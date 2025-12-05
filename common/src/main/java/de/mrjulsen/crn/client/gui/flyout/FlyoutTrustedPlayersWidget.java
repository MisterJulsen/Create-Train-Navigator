package de.mrjulsen.crn.client.gui.flyout;

import java.util.Set;
import java.util.function.Consumer;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.TrustedPlayerListComponent;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;

public class FlyoutTrustedPlayersWidget extends AbstractFlyoutWidget {

    private final TrustedPlayerListComponent playerList;
    private final Consumer<Set<Owner>> onClose;

    public FlyoutTrustedPlayersWidget(DLWindowManager manager, DLGuiComponent parentComponent, FlyoutPointer pointer, ColorShade pointerShade, Set<Owner> trusted, Consumer<Set<Owner>> onClose) {
        super(manager, parentComponent, 1, 60, pointer, pointerShade);
        this.onClose = onClose;
        playerList = addComponent(new TrustedPlayerListComponent(10, 10, 150, 120, trusted));
        setWidth(playerList.width() + 20);
        setHeight(playerList.height() + 20);
    }

    @Override
    protected void onOpen() {
    }
    
    @Override
    protected void onClose() {
        onClose.accept(playerList.getSelectedPlayers());
    }
    
}
