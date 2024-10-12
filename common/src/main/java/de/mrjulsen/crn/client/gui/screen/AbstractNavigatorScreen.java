package de.mrjulsen.crn.client.gui.screen;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.DLCreateIconButton;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.DLScreen;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLIconButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.DLTooltip;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AbstractNavigatorScreen extends DLScreen {

    protected static final int GUI_WIDTH = 240;//255;
    protected static final int GUI_HEIGHT = 247;

    protected BarColor primaryColoring;
    protected int guiLeft, guiTop;

    protected DLCreateIconButton backButton;

    protected final Screen lastScreen;

    protected AbstractNavigatorScreen(Screen lastScreen, Component title, BarColor primaryColoring) {
        super(title);
        this.lastScreen = lastScreen;
        this.primaryColoring = primaryColoring;
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;

        backButton = this.addRenderableWidget(new DLCreateIconButton(guiLeft + 8, guiTop + 223, DLIconButton.DEFAULT_BUTTON_WIDTH, DLIconButton.DEFAULT_BUTTON_HEIGHT, AllIcons.I_CONFIG_BACK) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                onClose();
            }
        });
        addTooltip(DLTooltip.of(Constants.TOOLTIP_GO_BACK).assignedTo(backButton));
    }

    @SuppressWarnings("resource")
    public void renderNavigatorBackground(Graphics graphics, int mouseX, int mouseY, float partialTicks) {              
        renderScreenBackground(graphics);
        CreateDynamicWidgets.renderWindow(graphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, ContainerColor.GRAY, primaryColoring, FooterSize.DEFAULT.size(), FooterSize.SMALL.size(), false);        
        GuiUtils.drawString(graphics, font, guiLeft + 6, guiTop + 4, getTitle(), 0x4F4F4F, EAlignment.LEFT, false);
        String timeString = TimeUtils.parseTime((int)((Minecraft.getInstance().level.getDayTime() + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        GuiUtils.drawString(graphics, font, guiLeft + GUI_WIDTH - 6, guiTop + 4, TextUtils.text(timeString), 0x4F4F4F, EAlignment.RIGHT, false);
    }
    
}
