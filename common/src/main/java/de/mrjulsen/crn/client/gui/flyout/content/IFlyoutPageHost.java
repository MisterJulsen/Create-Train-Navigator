package de.mrjulsen.crn.client.gui.flyout.content;

public interface IFlyoutPageHost {
    void pushPage(FlyoutContent content);
    void popPage();
    void closeFlyout();
}
