package de.mrjulsen.crn.client.gui.windows;

import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.BarColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.ContainerColor;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets.FooterSize;
import de.mrjulsen.crn.client.gui.widgets.create.CreateButton;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public abstract class AbstractNavigatorScreen extends DLWindow {

    protected static final int GUI_WIDTH = 240;
    protected static final int GUI_HEIGHT = 247;

    protected BarColor primaryColoring;
    protected ContainerColor containerColor;
    protected final CreateButton backButton;
    protected final Component title;

    protected AbstractNavigatorScreen(DLWindowManager manager, Component title, ContainerColor containerColor, BarColor primaryColoring) {
        super(manager);
        setSize(GUI_WIDTH, GUI_HEIGHT);
        this.title = title;
        this.containerColor = containerColor;
        this.primaryColoring = primaryColoring;
        this.windowSpawnPosition.set(WindowPosition.CENTER);

        backButton = addComponent(new CreateButton(8, 223, AllIcons.I_CONFIG_BACK));
        backButton.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });

        addEventListener(DLGuiStandardEvents.ScreenLayoutUpdatedEvent.class, (s, e) -> {
            //setPosition(getWindowManager().getScreenWidth() / 2 - width() / 2, getWindowManager().getScreenHeight() / 2 - height() / 2);
            return false;
        });

    }
    
    @Override
    public void renderMainLayer(DLGuiGraphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
        CreateDynamicWidgets.renderWindow(graphics, 0, 0, GUI_WIDTH, GUI_HEIGHT, containerColor, primaryColoring, FooterSize.DEFAULT.size(), FooterSize.SMALL.size(), true);        
        GuiUtils.drawString(graphics, graphics.defaultFont(), 6, 4, title, DLColor.fromInt(0xFF4F4F4F), ETextAlignment.LEFT, false);
        String timeString = DLTime.fromLevelTime(Minecraft.getInstance().level, new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME);
        GuiUtils.drawString(graphics, graphics.defaultFont(), GUI_WIDTH - 6, 4, TextUtils.text(timeString), DLColor.fromInt(0xFF4F4F4F), ETextAlignment.RIGHT, false);
    }
}
