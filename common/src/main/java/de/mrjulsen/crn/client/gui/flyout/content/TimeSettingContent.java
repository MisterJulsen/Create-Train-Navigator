package de.mrjulsen.crn.client.gui.flyout.content;

import java.util.function.Supplier;

import de.mrjulsen.crn.client.gui.widgets.create.CreateTimeSelectionComponent;
import de.mrjulsen.crn.data.settings.UserSettings.UserSetting;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.network.chat.MutableComponent;

public class TimeSettingContent extends FlyoutContent {

    private final MutableComponent title;
    private final Supplier<UserSetting<Integer>> getUserSetting;

    private CreateTimeSelectionComponent timeSelection;

    public TimeSettingContent(MutableComponent title, Supplier<UserSetting<Integer>> getUserSetting) {
        super(68, CreateTimeSelectionComponent.HEIGHT + 8);
        this.title = title;
        this.getUserSetting = getUserSetting;
    }

    @Override
    protected void onMount() {
        timeSelection = addComponent(new CreateTimeSelectionComponent(4, 4, 60));
    }

    @Override
    public void onShow() {
        timeSelection.value.set((double) getUserSetting.get().getValue());
    }

    @Override
    public void onHide() {
        getUserSetting.get().setValue(timeSelection.value.get().intValue());
    }

    @Override
    public void resetToDefaults() {
        getUserSetting.get().setToDefault();
        timeSelection.value.set((double) getUserSetting.get().getValue());
    }

    @Override
    public MutableComponent getTitle() {
        return title;
    }
}
