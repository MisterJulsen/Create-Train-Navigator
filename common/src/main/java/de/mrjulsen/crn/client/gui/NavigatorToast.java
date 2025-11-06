package de.mrjulsen.crn.client.gui;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class NavigatorToast implements Toast {

    private static final long DISPLAY_TIME = 5000L;
    private static final int MAX_LINE_SIZE = 200;

    private static final DLTexture MOD_ICON = new DLTexture(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/mod_icon.png"), 64, 64);

    private static final DLColor COLOR_BORDER = DLColor.BLACK;
    private static final DLColor COLOR_INNER_BORDER = DLColor.fromInt(0xFF286485);
    private static final DLColor COLOR_CANVAS = DLColor.fromInt(0xFF082C4C);

    private Component title;
    private List<FormattedCharSequence> messageLines;
    private long lastChanged;
    private boolean changed;
    private final int width;

    @SuppressWarnings("resource")
    public NavigatorToast(Component pTitle, Component pMessage) {
        this(pTitle, nullToEmpty(pMessage), Math.max(160, 30 + Math.max(Minecraft.getInstance().font.width(pTitle), pMessage == null ? 0 : Minecraft.getInstance().font.width(pMessage))));
    }

    @SuppressWarnings("resource")
    public static NavigatorToast multiline(Component pTitle, Component pMessage) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> list = font.split(pMessage, MAX_LINE_SIZE);
        int lineWidth = Math.max(Math.max(MAX_LINE_SIZE, font.width(pTitle)), list.stream().mapToInt(font::width).max().orElse(MAX_LINE_SIZE));
        return new NavigatorToast(pTitle, list, lineWidth + 48);
    }

    private NavigatorToast(Component pTitle, List<FormattedCharSequence> pMessageLines, int pWidth) {
        this.title = pTitle;
        this.messageLines = pMessageLines;
        this.width = pWidth;
    }

    private static ImmutableList<FormattedCharSequence> nullToEmpty(Component pMessage) {
        return pMessage == null ? ImmutableList.of() : ImmutableList.of(pMessage.getVisualOrderText());
    }

    public int width() {
        return this.width;
    }

    /**
     * 
     * @param pTimeSinceLastVisible time in milliseconds
     */
    @SuppressWarnings("resource")
    public Toast.Visibility render(GuiGraphics guiGraphics, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
        if (this.changed) {
            this.lastChanged = pTimeSinceLastVisible;
            this.changed = false;
        }

        DLGuiGraphics graphics = new DLGuiGraphics(guiGraphics, guiGraphics.pose(), Minecraft.getInstance().font, 0);

        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int lineWidth = width;
        int lineHeight = 10;
        
        int toastHeight = this.height() + Math.max(0, this.messageLines.size() - 1) * lineHeight + 3;
        GuiUtils.fill(graphics, 0, 0, lineWidth, toastHeight, COLOR_BORDER);
        GuiUtils.fill(graphics, 1, 1, lineWidth - 2, toastHeight - 2, COLOR_INNER_BORDER);
        GuiUtils.fill(graphics, 3, 3, lineWidth - 6, toastHeight - 6, COLOR_CANVAS);

        GuiUtils.drawTexture(MOD_ICON, graphics, 4, this.messageLines == null || this.messageLines.size() <= 1 ? 0 : 4, 32, 32, 0, 0);

        if (this.messageLines == null) {
            GuiUtils.drawString(graphics, pToastComponent.getMinecraft().font, 40, lineHeight, title, DLColor.fromInt(-256), ETextAlignment.LEFT, false);
        } else {
            GuiUtils.drawString(graphics, pToastComponent.getMinecraft().font, 40, 7, title, DLColor.fromInt(-256), ETextAlignment.LEFT, false);

            for (int i = 0; i < this.messageLines.size(); ++i) {
                graphics.graphics().drawString(Minecraft.getInstance().font, this.messageLines.get(i), 40, (20 + i * lineHeight), -1, false);
            }
        }

        return pTimeSinceLastVisible - this.lastChanged < DISPLAY_TIME ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }
}
