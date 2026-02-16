package de.mrjulsen.crn.client.gui.windows;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.TrainStatsViewer;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.util.TextUtils;

public class TrainStatsWindow extends AbstractNavigatorScreen {

    public TrainStatsWindow(DLWindowManager manager) {
        super(manager, TextUtils.text("Train Debug Screen"), CreateDynamicWidgets.ContainerColor.PURPLE, CreateDynamicWidgets.BarColor.PURPLE);

        pauseGame.set(false);
        manager.setPauseScreen(false);

        int wY = CreateDynamicWidgets.FooterSize.DEFAULT.size() - 1;
        int wH = GUI_HEIGHT - wY - CreateDynamicWidgets.FooterSize.SMALL.size();
        TrainStatsViewer viewer = addComponent(new TrainStatsViewer(3, wY + 2, width() - 6, wH - 3));
        viewer.displayTrains();

        CreateButton refreshButton = addComponent(new CreateButton(width() - CreateButton.WIDTH - 8, 223, ModGuiIcons.REFRESH.getAsCreateIcon()));
        refreshButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            viewer.displayTrains();
            return false;
        });
    }
}
