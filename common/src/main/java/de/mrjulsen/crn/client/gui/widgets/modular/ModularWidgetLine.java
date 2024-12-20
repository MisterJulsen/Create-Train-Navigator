package de.mrjulsen.crn.client.gui.widgets.modular;

import java.util.ArrayList;
import java.util.Collection;

import com.simibubi.create.foundation.gui.widget.ScrollInput;

import de.mrjulsen.mcdragonlib.client.gui.widgets.DLRenderable;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLWidgetContainer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class ModularWidgetLine extends DLWidgetContainer {

    protected static final int HEIGHT = 22;

    private int currentX = 0;
    private Collection<ScrollInput> scrollInputs = new ArrayList<>();
    
    public ModularWidgetLine(int x, int y, int width) {
        super(x, y, width, HEIGHT);
    }

    public int getCurrentX() {
        return currentX;
    }    

    public int getRemainingWidth() {
        return width() - currentX;
    }

    public <T extends AbstractWidget> T add(T w) {
        currentX += w.x - currentX + w.getWidth();
        if (w instanceof ScrollInput i) {
            scrollInputs.add(i);
        }
        return this.addRenderableWidget(w);
    }

    public <T extends DLRenderable> T add(T w) {
        currentX += w.x() - currentX + w.width();
        return this.addRenderableOnly(w);
    }

    @Override
    public void tick() {
        super.tick();
        for (ScrollInput i : scrollInputs) {
            i.tick();
        }
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
    
}
