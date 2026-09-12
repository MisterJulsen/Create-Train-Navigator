package de.mrjulsen.crn.client.gui.flyout;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.flyout.content.FlyoutContent;
import de.mrjulsen.crn.client.gui.flyout.content.IFlyoutPageHost;
import de.mrjulsen.crn.client.gui.widgets.AbstractFlyoutWidget;
import de.mrjulsen.crn.client.gui.widgets.FlatIconButton;
import de.mrjulsen.crn.data.settings.UserSettings;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLPanel;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.BorderLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;

public class SettingFlyout extends AbstractFlyoutWidget implements IFlyoutPageHost {

    private static final int MIN_WIDTH = 100;
    private static final int MIN_HEIGHT = 50;

    private final UserSettings settings;
    private final Deque<FlyoutContent> pageStack = new ArrayDeque<>();

    private final FlatIconButton backBtn;
    private final FlatIconButton resetBtn;
    private final DLPanel contentPanel;

    public SettingFlyout(DLWindowManager manager, DLGuiComponent parentComponent, FlyoutPointer pointer, ColorShade pointerShade, UserSettings settings) {
        super(manager, parentComponent, MIN_WIDTH, MIN_HEIGHT, pointer, pointerShade);
        this.settings = settings;

        BorderLayout layout = new BorderLayout(3, 3);
        layout.setPadding(new Padding((int)contentArea.get().top() + 2, width() - (int)contentArea.get().right() + 2, height() - (int)contentArea.get().bottom() + 2, (int)contentArea.get().left() + 2));
        this.layout.set(layout);

        DLPanel header = new DLPanel(0, 0, 1, FlatIconButton.HEIGHT);
        FlowLayout headerLayout = new FlowLayout();
        headerLayout.wrap.set(false);
        header.layout.set(headerLayout);
        header.layoutContraint.set(BorderLayout.BorderPosition.NORTH);
        addComponent(header);

        contentPanel = new DLPanel(0, 0, 1, 1);
        contentPanel.layoutContraint.set(BorderLayout.BorderPosition.CENTER);
        addComponent(contentPanel);

        backBtn = header.addComponent(new FlatIconButton(0, 0, GuiIcons.ARROW_LEFT.getAsSprite(16, 16)));
        backBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            popPage();
            return false;
        });
        backBtn.tooltip.set(new DLTooltip(List.of(Constants.TOOLTIP_GO_BACK), 200));
        backBtn.visible.set(false);

        resetBtn = header.addComponent(new FlatIconButton(0, 0, ModGuiIcons.REFRESH.getAsSprite(16, 16)));
        resetBtn.layoutContraint.set(FlowLayout.FlowConstraint.END);
        resetBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            FlyoutContent c = getCurrentPage();
            if (c != null) {
                c.resetToDefaults();
            }
            return false;
        });
        resetBtn.tooltip.set(new DLTooltip(List.of(Constants.TOOLTIP_RESET_DEFAULTS), 200));
        resetBtn.visible.set(false);
    }

    public SettingFlyout open(FlyoutContent root) {
        pushPage(root);
        return this;
    }

    private FlyoutContent getCurrentPage() {
        return pageStack.peek();
    }

    @Override
    public void pushPage(FlyoutContent content) {
        FlyoutContent prev = getCurrentPage();
        if (prev != null) {
            prev.onHide();
            contentPanel.removeComponent(prev);
        }
        pageStack.push(content);
        showCurrent();
    }

    @Override
    public void popPage() {
        if (pageStack.size() <= 1) {
            close();
            return;
        }
        FlyoutContent top = pageStack.pop();
        top.onHide();
        contentPanel.removeComponent(top);
        showCurrent();
    }

    @Override
    public void closeFlyout() {
        close();
    }

    private void showCurrent() {
        FlyoutContent c = getCurrentPage();
        if (c == null) {
            return;
        }
        if (!getComponents().contains(c)) {
            contentPanel.addComponent(c);
        }
        c.ensureMounted(this);
        resizeToContent(c);
        layoutHeader();
        c.onShow();
    }

    public int getRequiredWidth(FlyoutContent c) {
        int headerButtons = (pageStack.size() > 1 ? 1 : 0) + (c.isResettable() ? 1 : 0);
        return Math.max(MIN_WIDTH, Minecraft.getInstance().font.width(c.getTitle()) + (FlatIconButton.WIDTH) * headerButtons + 16);
    }

    public void resizeToContent(FlyoutContent c) {
        int minWidth = getRequiredWidth(c);
        int borderW = (FlyoutPointer.WIDTH) * 2;
        int borderH = FlatIconButton.HEIGHT + 3 + (FlyoutPointer.HEIGHT) * 2;

        setWidth(Math.max(minWidth, c.width()) + borderW);
        setHeight(c.height() + borderH);
        setPos();
    }

    private void layoutHeader() {
        Rectangle area = contentArea.get();
        FlyoutContent current = getCurrentPage();
        backBtn.visible.set(pageStack.size() > 1);
        resetBtn.visible.set(current != null && current.isResettable());
    }

    @Override
    public void renderFlyoutContent(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle contentArea) {
        FlyoutContent c = getCurrentPage();
        if (c == null) {
            return;
        }
        int titleX = (int)contentArea.x() + 8 + (backBtn.visible.get() ? FlatIconButton.WIDTH : 0);
        GuiUtils.drawString(graphics, graphics.defaultFont(), titleX, (int) contentArea.y() + 8, c.getTitle(), DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
    }

    @Override
    protected void onClose() {
        FlyoutContent c = getCurrentPage();
        if (c != null) {
            c.onHide();
        }
        DLUtils.doIfNotNull(settings, x -> x.clientSave(super::close));
    }
}
