package de.mrjulsen.crn.client.gui.flyout.content;

import java.util.List;

import de.mrjulsen.crn.client.gui.widgets.SearchOptionButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import net.minecraft.network.chat.MutableComponent;

public class SettingsMenuContent extends FlyoutContent {

    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 50;

    private final MutableComponent title;
    private final List<SettingEntry> entries;
    private final DLPanel contentPanel;

    public SettingsMenuContent(MutableComponent title, List<SettingEntry> entries) {
        super(DEFAULT_WIDTH, Math.max(DEFAULT_HEIGHT, entries.size() * (SearchOptionButton.HEIGHT)));
        this.title = title;
        this.entries = entries;

        this.contentPanel = new DLPanel(0, 0, 1, 1);
        FlowLayout layout = new FlowLayout();
        layout.flowDirection.set(FlowLayout.Direction.VERTICAL);
        layout.wrap.set(false);
        this.contentPanel.layout.set(layout);
        this.contentPanel.setSize(this.width(), this.height());
        this.contentPanel.anchor.set(EAlign.values());
        addComponent(contentPanel);
    }

    @Override
    public void onShow() {
        this.contentPanel.clearComponents();
        for (SettingEntry entry : entries) {
            this.contentPanel.addComponent(new SearchOptionButton(0, 0, width(), entry.label(), entry.value(), b -> host.pushPage(entry.content().get())));
        }
    }

    @Override
    public MutableComponent getTitle() {
        return title;
    }

    @Override
    public boolean isResettable() {
        return false;
    }
}
