package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.widgets.skins.CRNFlatButtonRenderer;
import de.mrjulsen.crn.data.storage.RecentSearchQueries.RecentSearchQuery;
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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class RecentSearchQueryButton extends DLButton {

    private final Component text;
    private final Component subText;

    public RecentSearchQueryButton(RouteViewer viewer, int x, int y, int width, RecentSearchQuery query) {
        super(x, y, width, 12);
        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            viewer.search(query.getStartStation(), query.getDestinationStation(), () -> {});
            return false;
        });
        this.cursor.set(CursorType.HAND);
        this.subText = TextUtils.truncateWithEllipsis(Minecraft.getInstance().font, TextUtils.text(DragonLib.DATE_FORMAT.format(query.getCreationTime())).withStyle(ChatFormatting.GRAY), (int)((float)(width - 6) / 0.75f));
        this.text = TextUtils.truncateWithEllipsis(Minecraft.getInstance().font, TextUtils.text(String.format("%s \u2192 %s", query.getStartStation(), query.getDestinationStation())), (int)((float)(width - 6 - Minecraft.getInstance().font.width(subText) - 5) / 0.75f));
        componentRenderer.set(CRNFlatButtonRenderer.INSTANCE);

        DLContextMenu contextMenu = new DLContextMenu((pX, pY) -> {
            List<DLContextMenu.ItemEntry> entries = new ArrayList<>();
            entries.add(new DLContextMenu.ItemEntry(Constants.TEXT_SEARCH, DLSprite.empty(), true, () -> {
                viewer.search(query.getStartStation(), query.getDestinationStation(), () -> {});                
            }, null));
            entries.add(DLContextMenu.ItemEntry.SEPARATOR);
            entries.add(new DLContextMenu.ItemEntry(Constants.TEXT_REMOVE, DLSprite.empty(), true, () -> {
                DLUtils.doIfNotNull(viewer.getUserSettings(), a -> {
                    a.recentSearchQueries.getValue().remove(query);
                    a.clientSave(() -> {
                        //viewer.hasSearched = false;
                        //viewer.refresh(viewer.userSettings);
                    });
                });
            }, null));
            return entries;
        });

        addEventListener(DLGuiStandardEvents.RightClickEvent.class, (src, event) -> {
            contextMenu.open(getWindowManager(), (int)getWindowManager().mouseXOnScreen(), (int)getWindowManager().mouseYOnScreen());
            return false;
        });
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
        graphics.poseStack().translate(3, 3, 0);
        graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 0, text, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.LEFT, false);
        graphics.poseStack().popPose();
        
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(width() - 3, 3, 0);
        graphics.poseStack().scale(0.75f, 0.75f, 0.75f);
        GuiUtils.drawString(graphics, graphics.defaultFont(), 0, 0, subText, DragonLib.VANILLA_BUTTON_ACTIVE_FONT_COLOR, ETextAlignment.RIGHT, false);
        graphics.poseStack().popPose();
        
    }
    
}