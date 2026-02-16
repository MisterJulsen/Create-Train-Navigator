package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.client.Minecraft;

public class TrainStatsSummary extends DLGuiComponent {

    private final int trainsCount;

    public TrainStatsSummary(int trainsCount) {
        super(0, 0, 100, (int)(5 + Minecraft.getInstance().font.lineHeight + 5));
        this.trainsCount = trainsCount;
    }

    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, width(), height(), CreateDynamicWidgets.ColorShade.DARK);

        GuiUtils.drawString(graphics, graphics.defaultFont(), 5, 5, TextUtils.truncateWithEllipsis(graphics.defaultFont(), "Trains: " + trainsCount, width() - 10), DLColor.WHITE, ETextAlignment.LEFT, false);

    }
}
