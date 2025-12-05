package de.mrjulsen.crn.client.gui.widgets.modular;

import java.util.ArrayList;
import java.util.Collection;

import com.simibubi.create.foundation.gui.widget.ScrollInput;

import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class ModularWidgetLine extends DLGuiComponent {

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
        currentX += w.getX() - currentX + w.getWidth();
        if (w instanceof ScrollInput i) {
            scrollInputs.add(i);
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        for (ScrollInput i : scrollInputs) {
            i.tick();
        }
    }
    
}
