package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLWidgetContainer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.ButtonState;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.client.gui.narration.NarrationElementOutput;

/*
 * DB = Database, not Deutsche Bahn!
 */
public class DBNavigatorWidget extends DLWidgetContainer {

    public static final int WIDTH = 54;
    private final String txt;

    public DBNavigatorWidget(int x, int y, int height, int currentIndex, int max, Runnable next, Runnable previous) {
        super(x, y, WIDTH, height);

        DLButton backBtn = addRenderableWidget(new DLButton(x(), y(), 10, height(), TextUtils.text("<"), (btn) -> DLUtils.doIfNotNull(previous, Runnable::run)));
        backBtn.setRenderStyle(AreaStyle.GRAY);
        DLButton nextBtn = addRenderableWidget(new DLButton(x() + width() - 10, y(), 10, height(), TextUtils.text(">"), (btn) -> DLUtils.doIfNotNull(next, Runnable::run)));
        nextBtn.setRenderStyle(AreaStyle.GRAY);
        this.txt = String.format("%s/%s", currentIndex + 1, max);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        DynamicGuiRenderer.renderArea(graphics, new GuiAreaDefinition(x() + 10, y(), width() - 20, height()), AreaStyle.GRAY, ButtonState.BUTTON);
        GuiUtils.drawString(graphics, font, x() + width() / 2, y() + height() / 2 - font.lineHeight / 2, txt, DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.CENTER, false);
        super.renderMainLayer(graphics, mouseX, mouseY, partialTicks);
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
