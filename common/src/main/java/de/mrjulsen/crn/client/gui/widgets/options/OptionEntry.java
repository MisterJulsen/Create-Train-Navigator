package de.mrjulsen.crn.client.gui.widgets.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;

import de.mrjulsen.crn.client.gui.Animator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLAbstractImageButton.ButtonType;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class OptionEntry<T extends WidgetContainer> extends WidgetContainer {

    public static void expandOrCollapse(OptionEntry<?> entry) {
        if (entry.isExpanded())
            entry.collapse();
        else
            entry.expand();
    }

    private int btnX = 0;
    private final Collection<DLButton> additionalButtons = new ArrayList<>();
    private final Collection<DLTooltip> tooltips = new ArrayList<>();
    private List<FormattedText> descriptionTooltips;

    private final int initialHeight = OptionEntryHeader.DEFAULT_HEIGHT;
    private final Component description;
    private Component text;

    private final Screen parent;
    private final DLOptionsList parentList;
    private final T contentContainer;
    private final Consumer<OptionEntry<T>> onSizeChanged;
    private final OptionEntryHeader header;
    private DLEditBox editBox;
    private final Animator animator = addRenderableOnly(new Animator());

    private boolean expanded;

    public OptionEntry(Screen parent, DLOptionsList parentList, int x, int y, int width, Function<OptionEntry<T>, T> contentContainer, Component text, Component description, Consumer<OptionEntry<T>> onSizeChanged, BiConsumer<OptionEntry<T>, OptionEntryHeader> onHeaderClick, Function<String, Boolean> onTitleEdited) {
        super(x, y, width, OptionEntryHeader.DEFAULT_HEIGHT);
        this.parent = parent;
        this.parentList = parentList;
        this.text = text;
        this.contentContainer = contentContainer != null ? contentContainer.apply(this) : null;
        this.description = description;
        this.onSizeChanged = onSizeChanged;
        this.setTooltip(font.getSplitter().splitLines(description, width, Style.EMPTY));

        header = addRenderableWidget(new OptionEntryHeader(this, x(), y(), width(), text, (b) -> onHeaderClick.accept(this, b)));
        DLUtils.doIfNotNull(this.contentContainer, a -> {
            addRenderableWidget(a);
            a.set_visible(false);
        });

        if (onTitleEdited != null) {
            editBox = addRenderableWidget(new DLEditBox(font, x() + 5, y() + 6, width() - 5 - 25, font.lineHeight, text) {
                @Override
                public void setMouseSelected(boolean selected) {
                    super.setMouseSelected(selected);
                    if (selected) {
                        header.setMouseSelected(true);
                    }
                }
            });
            editBox.setValue(text.getString());
            editBox.setBordered(false);
            editBox.setMaxLength(StationTag.MAX_NAME_LENGTH);
            editBox.set_visible(false);
            editBox.withOnFocusChanged((box, focus) -> {
                if (!focus) {
                    if (onTitleEdited.apply(box.getValue())) {
                        this.text = TextUtils.text(box.getValue());
                        header.setMessage(this.text);
                    }
                }
            });

        }

        animator.start(4, null, null, null);
    }

    public void addAdditionalButton(Sprite icon, Component text, BiConsumer<OptionEntry<T>, DLIconButton> onClick) {
        DLIconButton btn = new DLIconButton(ButtonType.DEFAULT, AreaStyle.FLAT, icon, x() + width() - 2 - 18 - btnX - 16, y() + 2, 16, OptionEntryHeader.DEFAULT_HEIGHT - 4, TextUtils.empty(), x -> onClick.accept(this, x)) {
            @Override
            public void setMouseSelected(boolean selected) {
                super.setMouseSelected(selected);
                if (selected) {
                    header.setMouseSelected(true);
                }
            }
        };
        btn.setBackColor(0x00000000);
        DLTooltip tooltip = DLTooltip.of(text).assignedTo(btn);
        tooltip.setDynamicOffset(() -> (int)parentList.getXScrollOffset(), () -> (int)parentList.getYScrollOffset());
        tooltips.add(tooltip);
        btnX += btn.width();
        addRenderableWidget(btn);
        additionalButtons.add(btn);
    }

    public OptionEntry(Screen parent, DLOptionsList parentList, int x, int y, int width, Function<OptionEntry<T>, T> contentContainer, Component text, Component description, Consumer<OptionEntry<T>> onExpandedChanged, BiConsumer<OptionEntry<T>, OptionEntryHeader> onHeaderClick) {
        this(parent, parentList, x, y, width, contentContainer, text, description, onExpandedChanged, onHeaderClick, null);
    }

    public GuiAreaDefinition getContentSpace() {
        return new GuiAreaDefinition(x() + 3, y() + initialHeight, width() - 6, height() - initialHeight - 2);
    }

    public void collapse() {
        expanded = false;
        set_height(initialHeight);
        DLUtils.doIfNotNull(editBox, a -> {
            a.setVisible(false);
            a.setWidth(width() - 5 - 5 - 18 - btnX);
        });
        DLUtils.doIfNotNull(contentContainer, a -> a.set_visible(false));
        onSizeChanged.accept(this);
    }

    public void expand() {
        expanded = true;
        DLUtils.doIfNotNull(contentContainer, a -> {
            set_height(initialHeight + a.height() + 2);
            a.set_visible(true);
        });
        DLUtils.doIfNotNull(editBox, a -> a.setVisible(true));
        onSizeChanged.accept(this);
    }

    public void notifyContentSizeChanged() {
        DLUtils.doIfNotNull(contentContainer, a -> {
            set_height(initialHeight + a.height() + 2);
        });
        onSizeChanged.accept(this);
    }

    public int getInitialHeight() {
        return initialHeight;
    }

    public Component getText() {
        return text;
    }

    public Component getDescription() {
        return description;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public T getContentContainer() {
        return contentContainer;
    }

    public void setTooltip(List<FormattedText> tooltip) {
        this.descriptionTooltips = tooltip;
    }

    public List<FormattedText> getTooltips() {
        return descriptionTooltips;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.poseStack().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        if (animator.isRunning()) {
            graphics.poseStack().translate(-animator.getTotalTicks() * 5 + animator.getCurrentTicksSmooth() * 5, 0, 0);
            GuiUtils.setTint(1, 1, 1, animator.getPercentage());
        }

        CreateDynamicWidgets.renderSingleShadeWidget(graphics, x() + 1, y(), width() - 2, height(), ColorShade.LIGHT);
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
        DLUtils.doIfNotNull(editBox, a -> {
            if (a.visible()) {
                CreateDynamicWidgets.renderTextBox(graphics, x() + 1, y() + 1, width() - 2 - 2 - 20 - btnX);
                //GuiUtils.fill(graphics, x() + 2, y() + 2, width() - 2 - 2 - 18 - btnX, OptionEntryHeader.DEFAULT_HEIGHT - 4, 0xFF000000);
                editBox.render(graphics.poseStack(), mouseX, mouseY, partialTicks);
            }
        });
        if (isExpanded() && contentContainer != null) {
            GuiUtils.fillGradient(graphics, x() + 3, y() + 20, 0, width() - 6, 10, 0x77000000, 0x00000000);
        }
        graphics.poseStack().popPose();
    }

    @Override
    public void renderFrontLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderFrontLayer(graphics, mouseX, mouseY, partialTicks);
        tooltips.stream().forEach(x -> x.render(parent, graphics, mouseX, mouseY));

    }
}
