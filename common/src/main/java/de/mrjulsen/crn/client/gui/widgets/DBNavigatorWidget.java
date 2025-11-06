package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.render.VanillaSimpleButtonRenderer;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.gui.narration.NarrationElementOutput;

/*
 * DB = Database, not Deutsche Bahn!
 */
public class DBNavigatorWidget extends DLGuiComponent {

    public static final int WIDTH = 54;
    private final String txt;

    public DBNavigatorWidget(int x, int y, int height, int currentIndex, int max, Runnable next, Runnable previous) {
        super(x, y, WIDTH, height);

        DLButton backBtn = addComponent(new DLButton(0, 0, 10, height()));
        backBtn.text.set(TextUtils.text("<"));
        backBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            DLUtils.doIfNotNull(previous, Runnable::run);
            return false;
        });
        backBtn.componentRenderer.set(VanillaSimpleButtonRenderer.VANILLA_BUTTON_GRAY);

        DLButton nextBtn = addComponent(new DLButton(width() - 10, 0, 10, height()));
        nextBtn.text.set(TextUtils.text(">"));
        nextBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            DLUtils.doIfNotNull(next, Runnable::run);
            return false;
        });
        nextBtn.componentRenderer.set(VanillaSimpleButtonRenderer.VANILLA_BUTTON_GRAY);
        nextBtn.componentRenderer.set(VanillaSimpleButtonRenderer.VANILLA_BUTTON_GRAY);

        this.txt = String.format("%s/%s", currentIndex + 1, max);
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        DynamicGuiRenderer.renderArea(graphics, new GuiAreaDefinition(x() + 10, y(), width() - 20, height()), AreaStyle.GRAY, ButtonState.BUTTON);
        GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 - font.lineHeight / 2, txt, DragonLib.NATIVE_UI_FONT_COLOR, ETextAlignment.CENTER, false);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {        
    }

    @Override
    public boolean consumeScrolling(double mouseX, double mouseY) {
        return false;
    }
    
}
