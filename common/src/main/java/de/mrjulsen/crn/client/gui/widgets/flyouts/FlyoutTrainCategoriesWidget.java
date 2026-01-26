package de.mrjulsen.crn.client.gui.widgets.flyouts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.CRNListBox;
import de.mrjulsen.crn.client.gui.widgets.FlatCheckBox;
import de.mrjulsen.crn.client.gui.widgets.ModernVerticalScrollBar;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.UserSettings.UserSetting;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.IDragonLibWidget;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.MutableComponent;

public class FlyoutTrainCategoriesWidget<T extends GuiEventListener & Widget & NarratableEntry> extends AbstractFlyoutWidget<T> {

    private final MutableComponent textTrainCategories = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_categories").withStyle(ChatFormatting.BOLD);
    private final UserSettings settings;

    private final CRNListBox<TrainCategory, FlatCheckBox> trainCategories;
    private final Supplier<UserSetting<Set<UUID>>> getUserSetting;

    public FlyoutTrainCategoriesWidget(DLScreen screen, FlyoutPointer pointer, ColorShade pointerShade, Consumer<T> addRenderableWidgetFunc, UserSettings settings, Supplier<UserSetting<Set<UUID>>> getUserSetting, Consumer<GuiEventListener> removeWidgetFunc) {
        super(screen, 1, 120, pointer, pointerShade, addRenderableWidgetFunc, removeWidgetFunc);
        set_width(Math.max(150, font.width(textTrainCategories) + DLIconButton.DEFAULT_BUTTON_WIDTH + 16 + 10 + FlyoutPointer.WIDTH * 2));
        this.settings = settings;
        this.getUserSetting = getUserSetting;

        int top = getContentArea().getY() + 21;
        int contentHeight = getContentArea().getHeight() - 21 - 2;

        ModernVerticalScrollBar scrollBar = new ModernVerticalScrollBar(screen, getContentArea().getX() + getContentArea().getWidth() - 7, top, contentHeight, GuiAreaDefinition.of(screen));
        this.trainCategories = addRenderableWidget(new CRNListBox<>(screen, getContentArea().getX() + 2, top, getContentArea().getWidth() - 4, contentHeight, scrollBar));
        addRenderableWidget(scrollBar);
        DLIconButton resetBtn = addRenderableWidget(new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, ModGuiIcons.REFRESH.getAsSprite(16, 16), getContentArea().getX() + getContentArea().getWidth() - DLIconButton.DEFAULT_BUTTON_WIDTH - 2, getContentArea().getY() + 2, TextUtils.empty(), (b) -> {
            getUserSetting.get().setToDefault();
            settings.clientSave(() -> reload(null));
        }));
        
        resetBtn.setBackColor(0x00000000);
    }

    @Override
    public void renderFlyoutContent(Graphics graphics, int mouseX, int mouseY, float partialTicks, GuiAreaDefinition contentArea) {
        super.renderFlyoutContent(graphics, mouseX, mouseY, partialTicks, contentArea);
        GuiUtils.drawString(graphics, font, contentArea.getX() + 8, contentArea.getY() + 8, textTrainCategories, DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, false);
    }

    @Override
    public void open(IDragonLibWidget parent) {
        reload(() -> super.open(parent));
    }

    private void reload(Runnable andThen)  {
        DataAccessor.getFromServer(null, ModAccessorTypes.GET_ALL_TRAIN_CATEGORIES, (categories) -> {
            trainCategories.displayData(new ArrayList<>(categories.stream().sorted((a, b) -> a.getCategoryName().compareToIgnoreCase(b.getCategoryName())).toList()), (category, i) -> {
                FlatCheckBox cb = new FlatCheckBox(0, 0, 0, category.getCategoryName(), getUserSetting.get().getValue().stream().noneMatch(x -> x.equals(category.getId())), (b) -> {});
                return cb;
            });
            DLUtils.doIfNotNull(andThen, x -> x.run());
        }); 
    }

    @Override
    public void close() {
        DLUtils.doIfNotNull(settings, x -> {    
            getUserSetting.get().setValue(new HashSet<>(trainCategories.getEntries().stream().filter(a -> !a.getKey().isChecked()).map(a -> a.getValue()).map(a -> a.getId()).collect(Collectors.toSet())));
            x.clientSave(super::close);
        });
    }
}
