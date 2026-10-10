package de.mrjulsen.crn.client.gui.flyout.content;

import java.util.*;
import java.util.function.Supplier;

import de.mrjulsen.crn.client.gui.widgets.SearchBox;
import de.mrjulsen.crn.client.gui.widgets.SelectionListBox;
import de.mrjulsen.crn.data.settings.TrainCategory;
import de.mrjulsen.crn.data.settings.UserSettings.UserSetting;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractCollectionComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.DLAbstractRichTextInputField;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.MutableComponent;

public class TrainCategoriesContent extends FlyoutContent {

    private final MutableComponent title;
    private final Supplier<UserSetting<Set<UUID>>> getUserSetting;

    private SelectionListBox<TrainCategory> trainCategories;
    private SearchBox searchBox;

    public TrainCategoriesContent(MutableComponent title, Supplier<UserSetting<Set<UUID>>> getUserSetting) {
        super(150, 108);
        this.title = title;
        this.getUserSetting = getUserSetting;
    }

    @Override
    protected void onMount() {
        searchBox = addComponent(new SearchBox(2, 0, width() - 4));
        searchBox.anchor.set2(EAlign.LEFT, EAlign.RIGHT, EAlign.TOP);
        searchBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
            trainCategories.filter.set(cat -> cat.getCategoryName().toLowerCase().contains(searchBox.text.get().getPlainText().toLowerCase()));
            return false;
        });

        int listTop = searchBox.height() + 3;
        trainCategories = addComponent(new SelectionListBox<>(0, listTop, width(), height() - listTop));
        trainCategories.anchor.set(EAlign.values());
        trainCategories.textFormat.set(item -> TextUtils.text(item.getCategoryName()));
        trainCategories.multiselect.set(true);
        trainCategories.addEventListener(DLAbstractCollectionComponent.FilterChangedEvent.class, (s, e) -> {
            trainCategories.selectIf(x -> !getUserSetting.get().getValue().contains(x.getId()));
            return false;
        });
    }

    @Override
    public void onShow() {
        trainCategories.enabled.set(false);
		ModNetworkManager.GET_ALL_TRAIN_CATEGORIES.send(NetworkDirection.toServer(), (response) -> {
			List<TrainCategory> sortedCategories = new ArrayList<>(response.getCategories());
			sortedCategories.sort((a, b) -> a.getCategoryName().compareToIgnoreCase(b.getCategoryName()));

			trainCategories.items.set(sortedCategories);
			trainCategories.selectIf(x -> !getUserSetting.get().getValue().contains(x.getId()));
            trainCategories.enabled.set(true);
        }, () -> {});
    }

    @Override
    public void onHide() {
		Set<UUID> uuids = new HashSet<>();
		for (TrainCategory category : trainCategories.items.get()) {
			if (trainCategories.selectedItems.get().contains(category)) continue;
			if (!trainCategories.filter.get().test(category)) continue;

			uuids.add(category.getId());
		}


        getUserSetting.get().setValue(uuids);
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
