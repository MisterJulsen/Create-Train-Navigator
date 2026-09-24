package de.mrjulsen.crn.client.gui.flyout.content;

import de.mrjulsen.crn.client.gui.flyout.SettingFlyout;
import de.mrjulsen.crn.client.gui.widgets.SelectionListBox;
import de.mrjulsen.crn.core.navigator.RoutingStrategy;
import de.mrjulsen.crn.data.settings.UserSettings.UserSetting;
import de.mrjulsen.crn.util.EDepartureBoardTrainFilter;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.function.Supplier;

public class BoardFilterContent extends FlyoutContent {

    private static final int WIDTH = 120;
    private static final int TEXT_BORDER = 5;
    private static final float DESCRIPTION_SCALE = 0.75f;

    private final MutableComponent title;
    private final Supplier<UserSetting<EDepartureBoardTrainFilter>> getUserSetting;

    private SelectionListBox<EDepartureBoardTrainFilter> filter;

    public BoardFilterContent(MutableComponent title, Supplier<UserSetting<EDepartureBoardTrainFilter>> getUserSetting) {
        super(WIDTH, getItemsHeight());
        this.title = title;
        this.getUserSetting = getUserSetting;
    }

    private static int getItemsHeight() {
        return EDepartureBoardTrainFilter.values().length * SelectionListBox.SelectionBoxItem.HEIGHT;
    }

    @Override
    protected void onMount() {
        if (host instanceof SettingFlyout f) {
            setWidth(Math.max(WIDTH, f.getRequiredWidth(this)));
        }

        filter = addComponent(new SelectionListBox<>(0, 0, width(), height()));
        filter.anchor.set(EAlign.values());
        filter.textFormat.set(ITranslatableEnum::getValueTranslation);
        filter.multiselect.set(false);
        filter.items.addAll(EDepartureBoardTrainFilter.values());
        filter.allowDeselect.set(false);
        filter.addEventListener(SelectionListBox.SelectEvent.class, (s, e) -> {
            getUserSetting.get().setValue((EDepartureBoardTrainFilter) e.item().getItem());
            return false;
        });
    }

    @Override
    public void onShow() {
        filter.selectedItems.set(List.of(getUserSetting.get().getValue()));
    }

    private int scale(int i, boolean invert) {
        if (invert) {
            return (int)((float)i / DESCRIPTION_SCALE);
        }
        return (int)((float)i * DESCRIPTION_SCALE);
    }

    @Override
    public void resetToDefaults() {
        getUserSetting.get().setToDefault();
        onShow();
    }

    @Override
    public MutableComponent getTitle() {
        return title;
    }
}
