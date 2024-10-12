package de.mrjulsen.crn.client.gui.widgets.create;

import com.simibubi.create.foundation.gui.widget.ScrollInput;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.CreateDynamicWidgets;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.gui.widgets.WidgetContainer;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CreateTimeSelectionWidget extends WidgetContainer {

    public static final int WIDHT = 66;
    public static final int HEIGHT = 18;

    private final MutableComponent transferTimeBoxText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_settings.transfer_time");
    
    protected ScrollInput transferTimeInput;
    private Component transferLabel = TextUtils.empty();

    private int value = 0;

    public CreateTimeSelectionWidget(int x, int y, int max) {
        super(x, y, WIDHT, HEIGHT);

        transferTimeInput = addRenderableWidget(new ScrollInput(x() + 3, y(), width() - 6, height())
            .withRange(0, max)
            .withStepFunction(a -> a.shift ? 1000 : 500)
            .titled(transferTimeBoxText.copy())
            .calling((i) -> {
                if (transferTimeInput == null) return;
                value = transferTimeInput.getState();
                transferLabel = TextUtils.text(TimeUtils.parseDurationShort(value));
            })
        );        
        setValue(0);
    }

    @Override
    public void renderMainLayer(Graphics graphics, int mouseX, int mouseY, float partialTicks) {
        CreateDynamicWidgets.renderTextBox(graphics, x(), y(), WIDHT);
        GuiUtils.drawString(graphics, font, x() + 5, y() + 5, transferLabel, DragonLib.NATIVE_BUTTON_FONT_COLOR_ACTIVE, EAlignment.LEFT, true);
    }

    @Override
    public void tick() {
        super.tick();
        transferTimeInput.tick();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        DLUtils.doIfNotNull(transferTimeInput, x -> x.setState(value));
        transferLabel = TextUtils.text(TimeUtils.parseDurationShort(value));
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
