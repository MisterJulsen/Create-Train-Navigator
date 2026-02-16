package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.debug.TrainDebugData;
import de.mrjulsen.crn.network.packets.pain.TeleportPlayerPacket;
import de.mrjulsen.crn.network.packets.pain.TrainHardResetPacketData;
import de.mrjulsen.crn.network.packets.pain.TrainSoftResetPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public class TrainStatsEntry extends DLGuiComponent {

    private final TrainDebugData data;

    public TrainStatsEntry(TrainDebugData data) {
        super(0, 0, 100, (int)(5 + Minecraft.getInstance().font.lineHeight + 2 + (Minecraft.getInstance().font.lineHeight * 3) * 0.75f + 5));
        this.data = data;

        DLContextMenu contextMenu = new DLContextMenu((pX, pY) -> {
            List<DLContextMenu.ItemEntry> entries = new ArrayList<>();
            entries.add(new DLContextMenu.ItemEntry(TextUtils.text("Teleport"), DLSprite.empty(), true, () -> {
                ModNetworkManager.TELEPORT_PLAYER.send(NetworkDirection.toServer(), new TeleportPlayerPacket(data.pos(), data.dimension()));
            }, null));
            entries.add(DLContextMenu.ItemEntry.SEPARATOR);
            entries.add(new DLContextMenu.ItemEntry(TextUtils.text("Clear Delays"), DLSprite.empty(), true, () -> {
                ModNetworkManager.TRAIN_SOFT_RESET.send(NetworkDirection.toServer(), new TrainSoftResetPacketData(data.trainId()));
            }, null));
            entries.add(new DLContextMenu.ItemEntry(TextUtils.text("Reset"), DLSprite.empty(), true, () -> {
                ModNetworkManager.TRAIN_HARD_RESET.send(NetworkDirection.toServer(), new TrainHardResetPacketData(data.trainId()));
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
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, 0, 0, width(), height(), CreateDynamicWidgets.ColorShade.DARK);

        MutableComponent predictions = TextUtils.text(String.format("%s [%s / %s]", data.state().getName(), data.predictionsInitialized(), data.predictionsCount()));
        MutableComponent trainName = TextUtils.text(data.trainName()).withStyle(ChatFormatting.BOLD);

        int lineX = 5;
        int lineY = 5;
        float scale = 0.75f;
        GuiUtils.drawString(graphics, graphics.defaultFont(), lineX, lineY, TextUtils.truncateWithEllipsis(graphics.defaultFont(), trainName, width() - 15 - graphics.defaultFont().width(predictions)), DLColor.WHITE, ETextAlignment.LEFT, false);
        GuiUtils.drawString(graphics, graphics.defaultFont(), width() - 5, lineY, predictions, data.state().getColor(), ETextAlignment.RIGHT, false);
        lineY += graphics.defaultFont().lineHeight + 2;
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(0, lineY, 0);
        graphics.poseStack().scale(scale, scale, scale);
        lineY = 0;
        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)(lineX / scale), lineY, TextUtils.truncateWithEllipsis(graphics.defaultFont(), "Train Id: " + data.trainId(), (int)(width() / scale) - 10), DLColor.fromInt(ChatFormatting.GRAY.getColor().intValue()), ETextAlignment.LEFT, false);
        lineY += graphics.defaultFont().lineHeight;
        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)(lineX / scale), lineY,  TextUtils.truncateWithEllipsis(graphics.defaultFont(), "Duration: " + data.totalDuration(), (int)(width() / scale) - 10), DLColor.fromInt(ChatFormatting.GRAY.getColor().intValue()), ETextAlignment.LEFT, false);
        lineY += graphics.defaultFont().lineHeight;
        GuiUtils.drawString(graphics, graphics.defaultFont(), (int)(lineX / scale), lineY,  TextUtils.truncateWithEllipsis(graphics.defaultFont(), "Position: " + data.pos().toShortString() + ", " + data.dimension(), (int)(width() / scale) - 10), DLColor.fromInt(ChatFormatting.GRAY.getColor().intValue()), ETextAlignment.LEFT, false);
        graphics.poseStack().popPose();
    }
}
