package de.mrjulsen.crn.client.gui.flyout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.client.gui.widgets.SearchBox;
import de.mrjulsen.crn.client.gui.widgets.SelectionListBox;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.UserSettings.UserSetting;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLAbstractCollectionComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.DLAbstractRichTextInputField;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

public class FlyoutTrainCategoriesWidget extends AbstractFlyoutWidget {

    private final MutableComponent textTrainCategories = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_categories").withStyle(ChatFormatting.BOLD);
    private final UserSettings settings;

    private final SelectionListBox<TrainCategory> trainCategories;
    private final Supplier<UserSetting<Set<UUID>>> getUserSetting;

    public FlyoutTrainCategoriesWidget(DLWindowManager manager, DLGuiComponent parentComponent, FlyoutPointer pointer, ColorShade pointerShade, UserSettings settings, Supplier<UserSetting<Set<UUID>>> getUserSetting) {
        super(manager, parentComponent, 1, 135, pointer, pointerShade);
        setWidth(Math.max(150, Minecraft.getInstance().font.width(textTrainCategories) + FlatIconButton.WIDTH + 20 + FlyoutPointer.WIDTH * 2));
        this.settings = settings;
        this.getUserSetting = getUserSetting;

        int top = (int)contentArea.get().y() + 21;
        int contentHeight = (int)contentArea.get().height() - 21 - 2 - 15;

        this.trainCategories = addComponent(new SelectionListBox<>((int)contentArea.get().x() + 2, top + 15, (int)contentArea.get().width() - 4, contentHeight));
        this.trainCategories.textFormat.set(item -> TextUtils.text(item.getCategoryName()));        
        this.trainCategories.multiselect.set(true);  
        this.trainCategories.addEventListener(DLAbstractCollectionComponent.FilterChangedEvent.class, (s, e) -> {            
            trainCategories.selectIf(x -> !getUserSetting.get().getValue().contains(x.getId()));
            return false;
        });

        SearchBox searchBox = addComponent(new SearchBox((int)contentArea.get().x() + 5, top, (int)contentArea.get().width() - 10));
        searchBox.addEventListener(DLAbstractRichTextInputField.TextAcceptKeyPressedEvent.class, (s, e) -> {
            this.trainCategories.filter.set(cat -> cat.getCategoryName().toLowerCase().contains(searchBox.text.get().getPlainText().toLowerCase()));
            return false;
        });
        
        FlatIconButton resetBtn = addComponent(new FlatIconButton((int)contentArea.get().right() - FlatIconButton.WIDTH - 2, (int)contentArea.get().top() + 2, ModGuiIcons.REFRESH.getAsSprite(16, 16)));
        resetBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getUserSetting.get().setToDefault();
            onOpen();
            return false;
        });        
    }

    @Override
    public void renderFlyoutContent(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle contentArea) {
        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)contentArea.x() + 8, (int)contentArea.y() + 8, textTrainCategories, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
    }

    @Override
    protected void onOpen() {
        trainCategories.enabled.set(false);
        reload(() -> {
            trainCategories.enabled.set(true);
        });
    }

    private void reload(Runnable andThen) {
        ModNetworkManager.GET_ALL_TRAIN_CATEGORIES.send(NetworkDirection.toServer(), (response) -> {
            trainCategories.items.set(new ArrayList<>(response.getCategories().stream().sorted((a, b) -> a.getCategoryName().compareToIgnoreCase(b.getCategoryName())).toList()));
            trainCategories.selectIf(x -> !getUserSetting.get().getValue().contains(x.getId()));
            andThen.run();
        }, () -> {});
    }

    @Override
    protected void onClose() {
        DLUtils.doIfNotNull(settings, x -> {    
            getUserSetting.get().setValue(new HashSet<>(trainCategories.items.get().stream().filter(a -> {
                return !trainCategories.selectedItems.get().contains(a) && trainCategories.filter.get().test(a);
            }).map(a -> a.getId()).collect(Collectors.toSet())));
            x.clientSave(super::close);
        });
    }
}
