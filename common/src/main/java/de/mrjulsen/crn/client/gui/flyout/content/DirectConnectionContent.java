package de.mrjulsen.crn.client.gui.flyout.content;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.flyout.SettingFlyout;
import de.mrjulsen.crn.client.gui.widgets.SelectionListBox;
import de.mrjulsen.crn.core.navigator.RoutingStrategy;
import de.mrjulsen.crn.data.settings.UserSettings.UserSetting;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.EAlign;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.function.Supplier;

public class DirectConnectionContent extends FlyoutContent {

    private static final int WIDTH = 100;
    private static final int TEXT_BORDER = 5;
    private static final float DESCRIPTION_SCALE = 0.75f;

    private final Component description = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.direct_connection.description");

    private final MutableComponent title;
    private final Supplier<UserSetting<Boolean>> getUserSetting;

    private SelectionListBox<Boolean> values;
    private MultiLineLabel messageLabel;

    public DirectConnectionContent(MutableComponent title, Supplier<UserSetting<Boolean>> getUserSetting) {
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

        values = addComponent(new SelectionListBox<>(0, 0, width(), height()));
        values.anchor.set(EAlign.values());
        values.textFormat.set(v -> v ? CommonComponents.GUI_YES : CommonComponents.GUI_NO);
        values.multiselect.set(false);
        values.items.addAll(true, false);
        values.allowDeselect.set(false);
        values.addEventListener(SelectionListBox.SelectEvent.class, (s, e) -> {
            getUserSetting.get().setValue((boolean) e.item().getItem());
            return false;
        });
        this.messageLabel = MultiLineLabel.create(Minecraft.getInstance().font, description, scale(width() - TEXT_BORDER * 2, true));
        setHeight(getItemsHeight() + scale(messageLabel.getLineCount() * Minecraft.getInstance().font.lineHeight, false) + TEXT_BORDER * 2);
    }

    @Override
    public void onShow() {
        values.selectedItems.set(List.of(getUserSetting.get().getValue()));
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
