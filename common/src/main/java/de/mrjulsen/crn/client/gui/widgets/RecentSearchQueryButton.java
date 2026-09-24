package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.datafixers.kinds.Const;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.data.settings.RecentSearchQueries;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.client.gui.widgets.layout.FlowLayout;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.Padding;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.widgets.skins.CRNFlatButtonRenderer;
import de.mrjulsen.crn.data.settings.RecentSearchQueries.RecentSearchQuery;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class RecentSearchQueryButton extends DLButton {

    private static final float SCALE = 0.75f;
    private static final float PADDING = 3;

    private final Component text;
    private final Component subText;
    private final RecentSearchQuery query;
    private final RouteViewer viewer;
    private final DLContextMenu contextMenu;

    private float usableWidth;
    private Component truncatedText;
    private Component truncatedSubText;

    public static int getHeight() {
        return (int)(PADDING * 2 + (float)Minecraft.getInstance().font.lineHeight * SCALE);
    }

    public RecentSearchQueryButton(RouteViewer viewer, int x, int y, int width, RecentSearchQuery query) {
        super(x, y, width, getHeight());
        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            viewer.search(query.getStartStation(), query.getDestinationStation(), () -> {});
            return false;
        });
        this.cursor.set(CursorType.HAND);
        this.viewer = viewer;
        this.query = query;
        this.subText = TextUtils.text(DragonLib.DATE_FORMAT.format(query.getCreationTime())).withStyle(ChatFormatting.GRAY);
        this.text = TextUtils.text(String.format("%s → %s", query.getStartStation(), query.getDestinationStation()));
        this.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);

        this.contextMenu = new DLContextMenu((pX, pY) -> {
            List<DLContextMenu.ItemEntry> entries = new ArrayList<>();
            entries.add(new DLContextMenu.ItemEntry(Constants.TEXT_SEARCH, DLSprite.empty(), true, () -> {
                viewer.search(query.getStartStation(), query.getDestinationStation(), () -> {});
            }, null));

            RecentSearchQueries recent = viewer.getUserSettings().recentSearchQueries.getValue();
            entries.add(new DLContextMenu.ItemEntry(recent.isPinned(query) ? Constants.TEXT_UNPIN : Constants.TEXT_PIN, DLSprite.empty(), true, () -> {
                if (recent.isPinned(query)) {
                    recent.unpin(query);
                } else {
                    recent.pin(query);
                }
                viewer.getUserSettings().clientSave(() -> {});
                viewer.loadList();
            }, null));
            entries.add(DLContextMenu.ItemEntry.SEPARATOR);
            entries.add(new DLContextMenu.ItemEntry(Constants.TEXT_REMOVE, DLSprite.empty(), true, () -> {
                DLUtils.doIfNotNull(viewer.getUserSettings(), a -> {
                    a.recentSearchQueries.getValue().remove(query);
                    viewer.loadList();
                    a.clientSave(() -> {});
                });
            }, null));
            return entries;
        });

        addEventListener(DLGuiStandardEvents.RightClickEvent.class, (src, event) -> {
            contextMenu.open(getWindowManager(), (int)getWindowManager().mouseXOnScreen(), (int)getWindowManager().mouseYOnScreen());
            return false;
        });
        addEventListener(DLGuiStandardEvents.ComponentPosAndSizeChanged.class, (src, event) -> {
            updateText();
            return false;
        });
        addEventListener(DLGuiStandardEvents.MouseEnterEvent.class, (src, event) -> {
            getComponents().forEach(c -> c.visible.set(true));
            updateText();
            return false;
        });
        addEventListener(DLGuiStandardEvents.MouseLeaveEvent.class, (src, event) -> {
            getComponents().forEach(c -> c.visible.set(false));
            updateText();
            return false;
        });
        updateButtons();
    }

    private void updateText() {
        this.usableWidth = width() - PADDING * 2;
        this.truncatedSubText = TextUtils.truncateWithEllipsis(Minecraft.getInstance().font, subText, (int)(usableWidth / SCALE));

        float subTextW = (float)Minecraft.getInstance().font.width(subText) * SCALE + PADDING * 2;
        int buttonsW = isSelected() ? 0 : (int)(height() * componentsCount() + PADDING * 2);

        this.truncatedText = TextUtils.truncateWithEllipsis(Minecraft.getInstance().font, text, (int)((usableWidth - subTextW - buttonsW) / SCALE));

        FlowLayout layout = new FlowLayout();
        layout.wrap.set(false);
        layout.fillCrossAxis.set(true);
        layout.padding.set(new Padding(0, (int)(subTextW + PADDING), 0, 0));
        this.layout.set(layout);
    }

    private void updateButtons() {
        clearComponents();
        RecentSearchQueries recent = viewer.getUserSettings().recentSearchQueries.getValue();
        addButton(ModGuiIcons.MORE_OPTIONS.getAsSprite(height(), height()), Constants.TEXT_MORE_OPTIONS, (b) -> {
            contextMenu.open(getWindowManager(), (int)(b.getXOnScreen()), (int)(b.getYOnScreen() + b.height() * b.getGlobalScale()));
        });
        addButton((recent.isPinned(query) ? ModGuiIcons.UNPIN : ModGuiIcons.PIN).getAsSprite(height(), height()), recent.isPinned(query) ? Constants.TEXT_UNPIN : Constants.TEXT_PIN, (b) -> {
            if (recent.isPinned(query)) {
                recent.unpin(query);
            } else {
                recent.pin(query);
            }
            viewer.getUserSettings().clientSave(() -> {});
            viewer.loadList();
        });
    }

    private void addButton(DLSprite icon, Component text, Consumer<DLButton> action) {
        DLButton btn = new DLButton(0, 0, height(), height());
        btn.icon.set(icon);
        btn.text.set(TextUtils.EMPTY);
        btn.tooltip.set(new DLTooltip(List.of(text), 100));
        btn.layoutContraint.set(FlowLayout.FlowConstraint.END);
        btn.componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);
        btn.visible.set(false);
        btn.inputConsumptionPolicy.set(a -> a != ConsumptionType.MOUSE_MOVE);
        btn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            action.accept(btn);
            return false;
        });
        addComponent(btn);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        if (!enabled.get()) {
            componentRenderer.get().renderSprite(graphics, 0, 0, width(), height(), this, ButtonState.DISABLED);
        } else if (isMouseDown() && (getWindowManager() != null ? getWindowManager().getMouseDownButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT : true)) {
            componentRenderer.get().renderSprite(graphics, 0, 0, width(), height(), this, ButtonState.DOWN_SELECTED);
        } else if (isSelected()) {
            componentRenderer.get().renderSprite(graphics, 0, 0, width(), height(), this, ButtonState.SELECTED);
        } else {
            componentRenderer.get().renderSprite(graphics, 0, 0, width(), height(), this, ButtonState.NORMAL);
        }

        graphics.poseStack().pushPose();
        graphics.poseStack().translate(PADDING, PADDING, 0);
        graphics.poseStack().scale(SCALE, SCALE, SCALE);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 0, truncatedText, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        graphics.poseStack().popPose();

        graphics.poseStack().pushPose();
        graphics.poseStack().translate(width() - PADDING, PADDING, 0);
        graphics.poseStack().scale(SCALE, SCALE, SCALE);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 0, truncatedSubText, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.RIGHT, false);
        graphics.poseStack().popPose();

    }

}