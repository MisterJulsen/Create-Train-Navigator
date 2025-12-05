package de.mrjulsen.crn.client.gui.flyout;

import java.util.Optional;
import java.util.function.BiPredicate;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.SearchBox;
import de.mrjulsen.crn.client.gui.widgets.SelectionListBox;
import de.mrjulsen.crn.client.gui.widgets.create.CreateItemPicker;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractCollectionComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.DLAbstractRichTextInputField;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;

public class FlyoutItemSelectionBrowser<T> extends AbstractFlyoutWidget {

    private final SelectionListBox<T> trainCategories;

    public FlyoutItemSelectionBrowser(DLWindowManager manager, CreateItemPicker<T> parentComponent, BiPredicate<T, String> filter) {
        super(manager, parentComponent, parentComponent.width() + FlyoutPointer.WIDTH * 2 - 4, 120, FlyoutPointer.UP, ColorShade.DARK);

        int top = (int)contentArea.get().y() + FlyoutPointer.HEIGHT;
        int contentHeight = (int)contentArea.get().height() - FlyoutPointer.HEIGHT - 2 - 15;

        this.trainCategories = addComponent(new SelectionListBox<>((int)contentArea.get().x() + 2, top + 15, (int)contentArea.get().width() - 4, contentHeight));
        this.trainCategories.textFormat.set(parentComponent.formatter.get()::apply);
        this.trainCategories.addEventListener(DLAbstractCollectionComponent.FilterChangedEvent.class, (s, e) -> {
            trainCategories.selectIf(x -> x.equals(parentComponent.selectedItem.get().orElse(null)));
            return false;
        });

        SearchBox searchBox = addComponent(new SearchBox((int)contentArea.get().x() + 5, top, (int)contentArea.get().width() - 10));
        searchBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
            this.trainCategories.filter.set(a -> filter.test(a, searchBox.text.get().getPlainText()));
            return false;
        });

        trainCategories.addEventListener(SelectionListBox.SelectEvent.class, (s, e) -> {
            close();
            return false;
        });
    }

    @Override
    protected void onOpen() {
        trainCategories.enabled.set(false);
        reload(() -> {
            trainCategories.enabled.set(true);
        });
    }

    @SuppressWarnings("unchecked")
    private void reload(Runnable andThen) {
        ModNetworkManager.GET_ALL_TRAIN_CATEGORIES.send(NetworkDirection.toServer(), (response) -> {
            trainCategories.items.set(((CreateItemPicker<T>)parentComponent).items.get());
            trainCategories.selectIf(x -> x.equals(((CreateItemPicker<T>)parentComponent).selectedItem.get().orElse(null)));
            andThen.run();
        }, () -> {});
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onClose() {
        if (!trainCategories.selectedItems.get().isEmpty()) {
            ((CreateItemPicker<T>)parentComponent).selectedItem.set(Optional.ofNullable(trainCategories.selectedItems.get().get(0)));
        }
    }
}
