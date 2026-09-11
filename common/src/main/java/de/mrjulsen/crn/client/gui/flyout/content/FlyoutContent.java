package de.mrjulsen.crn.client.gui.flyout.content;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import net.minecraft.network.chat.MutableComponent;

public abstract class FlyoutContent extends DLGuiComponent {

    private final int preferredW;
    private final int preferredH;
    private boolean mounted = false;

    protected IFlyoutPageHost host;

    public FlyoutContent(int preferredWidth, int preferredHeight) {
        super(0, 0, preferredWidth, preferredHeight);
        this.preferredW = preferredWidth;
        this.preferredH = preferredHeight;
    }

    public abstract MutableComponent getTitle();

    public void resetToDefaults() {}

    public boolean isResettable() {
        return true;
    }

    public int preferredWidth() {
        return preferredW;
    }

    public int preferredHeight() {
        return preferredH;
    }

    protected void onMount() {}

    public void onShow() {}

    public void onHide() {}

    public final void ensureMounted(IFlyoutPageHost host) {
        this.host = host;
        if (!mounted) {
            mounted = true;
            onMount();
        }
    }
}
