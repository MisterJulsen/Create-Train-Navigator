package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.render.VanillaSimpleButtonRenderer;
import de.mrjulsen.mcdragonlib.client.render.DefaultGuiTextures;
import de.mrjulsen.mcdragonlib.client.render.GuiIcons;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;

/*
 * DB = Database, not Deutsche Bahn!
 */
public class DBNavigatorWidget extends DLGuiComponent {

    public static final int WIDTH = 54;
    private final String txt;

    public DBNavigatorWidget(int x, int y, int height, int currentIndex, int max, Runnable next, Runnable previous) {
        super(x, y, WIDTH, height);

        DLButton backBtn = addComponent(new DLButton(0, 0, 10, height()));
        backBtn.icon.set(GuiIcons.ARROW_LEFT.getAsSprite(16, 16));
        backBtn.text.set(TextUtils.EMPTY);
        backBtn.componentRenderer.set(VanillaSimpleButtonRenderer.VANILLA_BUTTON_GRAY);
        backBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            DLUtils.doIfNotNull(previous, Runnable::run);
            return false;
        });

        DLButton nextBtn = addComponent(new DLButton(width() - 10, 0, 10, height()));
        nextBtn.icon.set(GuiIcons.ARROW_RIGHT.getAsSprite(16, 16));
        nextBtn.text.set(TextUtils.EMPTY);
        nextBtn.componentRenderer.set(VanillaSimpleButtonRenderer.VANILLA_BUTTON_GRAY);
        nextBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            DLUtils.doIfNotNull(next, Runnable::run);
            return false;
        });
        this.txt = String.format("%s/%s", currentIndex + 1, max);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        DefaultGuiTextures.DRAGONLIB_UI.getSprite("button_gray_normal").render(graphics, 10, 0, width() - 20, height());
        GuiUtils.drawString(graphics, graphics.defaultFont(), width() / 2, height() / 2 - graphics.defaultFont().lineHeight / 2, txt, DragonLib.VANILLA_UI_FONT_COLOR, ETextAlignment.CENTER, false);
    }
    
}
