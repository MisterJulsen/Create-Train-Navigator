package de.mrjulsen.crn.client.gui.widgets.flyouts;

import java.util.Set;
import java.util.function.Consumer;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.screen.GlobalSettingsScreen.IPlayerListSuggestionData;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.TrustedPlayersWidget;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public class FlyoutPlayerList<T extends GuiEventListener & Widget & NarratableEntry> extends AbstractFlyoutWidget<T> {

    private final TrustedPlayersWidget playerList;

    public FlyoutPlayerList(DLScreen screen, IPlayerListSuggestionData suggestions, Set<Owner> currentPlayers, Consumer<T> addRenderableWidgetFunc, Consumer<GuiEventListener> removeWidgetFunc) {
        super(screen, 100, 50, FlyoutPointer.RIGHT, ColorShade.DARK, addRenderableWidgetFunc, removeWidgetFunc);
        playerList = addRenderableWidget(new TrustedPlayersWidget(screen, x() + 10, y() + 10, 125, 100, suggestions, currentPlayers));
        set_width(playerList.width() + 20);
        set_height(playerList.height() + 20);
    }

    public TrustedPlayersWidget getPlayerList() {
        return playerList;
    }
}
