package de.mrjulsen.crn.client.gui.widgets;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ColorShade;
import de.mrjulsen.crn.debug.TrainDebugData;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenu;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLContextMenuItem.ContextMenuItemData;
import de.mrjulsen.mcdragonlib.client.render.DynamicGuiRenderer.AreaStyle;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TrainDebugWidget extends DLButton {    

    public static final int HEADER_HEIGHT = 20;
    public static final int DEFAULT_LINE_HEIGHT = 12;
    public static final float DEFAULT_SCALE = 0.75f;

    private final TrainDebugData data;

    public TrainDebugWidget(Screen parent, TrainDebugViewer viewer, int x, int y, int width, TrainDebugData data) {
        super(x, y, width, 32, TextUtils.empty(), (b) -> {});
        this.data = data;
        setRenderStyle(AreaStyle.FLAT);
        setMenu(new DLContextMenu(() -> GuiAreaDefinition.of(this), () -> new DLContextMenuItem.Builder()
            .add(new ContextMenuItemData(TextUtils.text("Reset Predictions"), Sprite.empty(), true, (b) -> DataAccessor.getFromServer(data.trainId(), ModAccessorTypes.TRAIN_SOFT_RESET, $ -> viewer.reload()), null))
            .addSeparator()
            .add(new ContextMenuItemData(TextUtils.text("Hard Reset"), Sprite.empty(), true, (b) -> DataAccessor.getFromServer(data.trainId(), ModAccessorTypes.TRAIN_HARD_RESET, $ -> viewer.reload()), null))
        ));
    }  

    @Override
    public void renderMainLayer(Graphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        
        CreateDynamicWidgets.renderSingleShadeWidget(graphics, x(), y(), width(), height(), ColorShade.DARK.getColor());

        if (isMouseSelected()) {
            GuiUtils.fill(graphics, x(), y(), width(), height(), 0x22FFFFFF);
        }

        final float scale = 0.75f;
        Component trainName = TextUtils.text(data.trainName()).withStyle(ChatFormatting.BOLD);
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(x(), y(), 0);
        graphics.poseStack().scale(scale, scale, scale); 

        Component predictionsText = TextUtils.text(data.predictionsInitialized() + " / " + data.predictionsCount());
        int platformTextWidth = font.width(predictionsText);
        CreateDynamicWidgets.renderTextHighlighted(graphics, 5, 4, font, trainName, Constants.COLOR_TRAIN_BACKGROUND);        
        final int maxIdWidth = (int)(width() - platformTextWidth * scale - 15 - (45 + font.width(trainName)) * scale);
        MutableComponent idText = TextUtils.text(data.trainId().toString());
        if (font.width(idText) > maxIdWidth) {
            idText = TextUtils.text(font.substrByWidth(idText, maxIdWidth).getString()).append(TextUtils.text("...")).withStyle(idText.getStyle());
        }
        GuiUtils.drawString(graphics, font, 20 + font.width(trainName), 6, idText, 0xFFFFFF, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, (int)(width() / scale) - 6, 6, predictionsText, 0xFFFFFF, EAlignment.RIGHT, false);
        GuiUtils.drawString(graphics, font, 5, 20, TextUtils.text("Session: " + data.sessionId()), 0xFFDBDBDB, EAlignment.LEFT, false);
        GuiUtils.drawString(graphics, font, 5, 30, TextUtils.text("Status: " + data.state().getName()), data.state().getColor(), EAlignment.LEFT, false);
        graphics.poseStack().popPose();  
    }
    
}
