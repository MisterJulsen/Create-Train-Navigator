package de.mrjulsen.crn.client.gui.widgets.routedetails;

import com.simibubi.create.content.trains.entity.TrainIconType;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Widget showing details about the train */
public class TrainDataWidget extends DLGuiComponent {
    
    private final RouteLeg part;

    public TrainDataWidget(RouteLeg part) {
        super(0, 0, 100, 26);
        this.part = part;
    }
    
    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        final float scale = 0.75f;
        final float mul = 1 / scale;
        final float maxWidth = 140;

        CreateDynamicWidgets.renderTextSlotOverlay(graphics, 0, 0, width(), height());
        trainIcon().render(TrainIconType.ENGINE, graphics.graphics(), 4, 7);

        graphics.poseStack().pushPose();
        graphics.poseStack().scale(scale, scale, scale);

        Component trainName = TextUtils.text(part.displayName()).withStyle(ChatFormatting.BOLD);
        CreateDynamicWidgets.renderTextHighlighted(graphics, (int)(28 / scale), (int)(3 / scale), graphics.defaultFont(), trainName, part.displayColor());

        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)(28 / scale) + graphics.defaultFont().width(trainName) + 10, (int)(5 / scale), TextUtils.truncateWithEllipsis(graphics.defaultFont(), TextUtils.text(String.format("%s (%s)", part.trainName(), part.trainId().toString().split("-")[0])), (int)((maxWidth - graphics.defaultFont().width(trainName) - 25) / scale)), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)(28 / scale), (int)(17 / scale), TextUtils.truncateWithEllipsis(graphics.defaultFont(), TextUtils.text(part.destinationText()), (int)((maxWidth - 24) / scale)), DLColor.fromInt(0xFFDBDBDB), ETextAlignment.LEFT, false);

        graphics.poseStack().scale(mul, mul, mul);
        graphics.poseStack().popPose();
    }

    private TrainIconType trainIcon() {
        return part.iconId() == null ? TrainIconType.getDefault() : TrainIconType.byId(part.iconId());
    }
}
