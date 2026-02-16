package de.mrjulsen.crn.client.gui.windows;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.SavedRoutesViewer;
import de.mrjulsen.crn.data.SavedRoutesManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.util.TextUtils;

public class SavedRoutesWindow extends AbstractNavigatorScreen {

    private final SavedRoutesViewer viewer;

    public SavedRoutesWindow(DLWindowManager manager) {
        super(manager, TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.title"), ContainerColor.PURPLE, BarColor.GOLD);
        
        int wY = FooterSize.DEFAULT.size() - 1;
        int wH = GUI_HEIGHT - wY - FooterSize.SMALL.size();
        this.viewer = new SavedRoutesViewer(3, wY + 2, width() - 6, wH - 3);
        this.viewer.displaySavedRoutes(SavedRoutesManager.getAllSavedRoutes());
        
        addComponent(viewer);
    }
}
