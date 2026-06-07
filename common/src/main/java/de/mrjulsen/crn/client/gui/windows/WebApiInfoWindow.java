package de.mrjulsen.crn.client.gui.windows;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.widgets.WebApiInfoViewer;
import de.mrjulsen.crn.network.packets.pain.ShowWebApiScreenPacketData;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.util.TextUtils;

public class WebApiInfoWindow extends AbstractNavigatorScreen {

    private final WebApiInfoViewer viewer;

    public WebApiInfoWindow(DLWindowManager manager, ShowWebApiScreenPacketData data) {
        super(
            manager,
            TextUtils.translate("gui.createrailwaysnavigator.web_api.title"),
            CreateDynamicWidgets.ContainerColor.PURPLE,
            CreateDynamicWidgets.BarColor.PURPLE
        );

        pauseGame.set(false);
        manager.setPauseScreen(false);

        int wY = CreateDynamicWidgets.FooterSize.DEFAULT.size() - 1;
        int wH = GUI_HEIGHT - wY - CreateDynamicWidgets.FooterSize.SMALL.size();
        viewer = addComponent(new WebApiInfoViewer(3, wY + 2, width() - 6, wH - 3));
        viewer.display(data);
    }
}
