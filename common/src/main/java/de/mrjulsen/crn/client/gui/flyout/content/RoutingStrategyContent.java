package de.mrjulsen.crn.client.gui.flyout.content;

import de.mrjulsen.crn.client.gui.flyout.SettingFlyout;
import de.mrjulsen.crn.client.gui.widgets.SelectionListBox;
import de.mrjulsen.crn.core.navigator.RoutingStrategy;
import de.mrjulsen.crn.data.settings.UserSettings.UserSetting;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.function.Supplier;

public class RoutingStrategyContent extends FlyoutContent {

    private static final int WIDTH = 100;
    private static final int TEXT_BORDER = 5;
    private static final float DESCRIPTION_SCALE = 0.75f;

    private final MutableComponent title;
    private final Supplier<UserSetting<RoutingStrategy>> getUserSetting;

    private SelectionListBox<RoutingStrategy> strategies;
    private MultiLineLabel messageLabel;

    public RoutingStrategyContent(MutableComponent title, Supplier<UserSetting<RoutingStrategy>> getUserSetting) {
        super(WIDTH, getItemsHeight());
        this.title = title;
        this.getUserSetting = getUserSetting;
    }

    private static int getItemsHeight() {
        return RoutingStrategy.values().length * SelectionListBox.SelectionBoxItem.HEIGHT;
    }

    @Override
    protected void onMount() {
        if (host instanceof SettingFlyout f) {
            setWidth(Math.max(WIDTH, f.getRequiredWidth(this)));
        }

        strategies = addComponent(new SelectionListBox<>(0, 0, width(), height()));
        strategies.anchor.set(EAlign.values());
        strategies.textFormat.set(ITranslatableEnum::getValueTranslation);
        strategies.multiselect.set(false);
        strategies.items.addAll(RoutingStrategy.values());
        strategies.allowDeselect.set(false);
        strategies.addEventListener(SelectionListBox.SelectEvent.class, (s, e) -> {
            getUserSetting.get().setValue((RoutingStrategy) e.item().getItem());
            setDescription();
            return false;
        });
        setDescription();
    }

    @Override
    public void onShow() {
        strategies.selectedItems.set(List.of(getUserSetting.get().getValue()));
    }

    private void setDescription() {
        this.messageLabel = MultiLineLabel.create(Minecraft.getInstance().font, getUserSetting.get().getValue().getValueDescriptionTranslation(), scale(width() - TEXT_BORDER * 2, true));
        setHeight(getItemsHeight() + scale(messageLabel.getLineCount() * Minecraft.getInstance().font.lineHeight, false) + TEXT_BORDER * 2);
        if (host instanceof SettingFlyout f) {
            f.resizeToContent(this);
        }
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

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        super.renderMainLayer(graphics, mouseX, mouseY, renderBounds);

        graphics.poseStack().pushPose();
        graphics.poseStack().translate(TEXT_BORDER, getItemsHeight() + TEXT_BORDER, 0);
        graphics.poseStack().scale(DESCRIPTION_SCALE, DESCRIPTION_SCALE, DESCRIPTION_SCALE);
        DLUtils.doIfNotNull(messageLabel, x -> x.renderLeftAligned(graphics.graphics(), 0, 0, graphics.defaultFont().lineHeight, 0xFFDBDBDB));
        graphics.poseStack().popPose();
    }
}
