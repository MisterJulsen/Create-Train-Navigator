package de.mrjulsen.crn.client.gui.widgets.routedetails;

import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.util.Holder;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;

public class TrainStatusInfoWidget extends DLGuiComponent {

    private final CompiledTrainStatus status;
    private boolean loopFix = false;

    public TrainStatusInfoWidget(int x, int y, int w, CompiledTrainStatus status) {
        super(x, y, w, 1);
        this.status = status;
        
        addEventListener(DLGuiStandardEvents.ComponentPosAndSizeChanged.class, (s, e) -> {
            if (loopFix) return false;
            loopFix = true;
            update();
            loopFix = false;
            return false;
        });

        update();
    }
    
    private void update() {
        this.setHeight(Math.max(9, (int)(ClientWrapper.getTextBlockHeight(Minecraft.getInstance().font, status.text(), (int)((width() - ModGuiIcons.ICON_SIZE) / 0.75f)) * 0.75f) + 2));
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        status.render(graphics, Holder.of(graphics.defaultFont()), 0, 0, width());
    }
    
}
