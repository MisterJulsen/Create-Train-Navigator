package de.mrjulsen.crn.client.gui.widgets.create;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.utility.CreateLang;

import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLNumberPicker;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.properties.Property;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class CreateScrollNumberInput extends DLNumberPicker {

	protected final Component scrollToModify = CreateLang.translateDirect("gui.scrollInput.scrollToModify").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
	protected final Component shiftScrollsFaster = CreateLang.translateDirect("gui.scrollInput.shiftScrollsFaster").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);

    public final Property<Component> title = new Property<Component>(CreateLang.translateDirect("gui.scrollInput.defaultTitle"));
    public final Property<Component> hint = new Property<Component>(TextUtils.empty());
    public final Property<Function<Double, Component>> formatter = new Property<>(t -> TextUtils.text(String.valueOf(t.intValue())));

    public CreateScrollNumberInput(int x, int y, int w) {
        super(x, y, w, 18);
        this.textboxComponentRenderer.set(CreateTextBoxComponentRenderer.INSTANCE);
        this.showButtons.set(false);

        addEventListener(DLNumberPicker.ValueChangedEvent.class, (s, e) -> {
            updateTooltip();
            return false;
        });

        addEventListener(DLGuiStandardEvents.ScrollEvent.class, (s, e) -> {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(AllSoundEvents.SCROLL_VALUE.getMainEvent(), (float)(1.5f + 0.1f * (value.get() - min.get()) / (max.get() - min.get()))));
            return false;
        });
        
        addEventListener(DLNumberPicker.ValueRangeChangedEvent.class, (s, e) -> {
            updateTooltip();
            return false;
        });
    }    
    
    public void addToValue(double fac) {
        super.addToValue(fac * -1);
    }

    protected void updateTooltip() {
        List<FormattedText> lines = new ArrayList<>();
		lines.add(title.get().plainCopy().withStyle(s -> s.withColor(AbstractSimiWidget.HEADER_RGB.getRGB())));
		lines.add(hint.get().plainCopy().withStyle(s -> s.withColor(AbstractSimiWidget.HINT_RGB.getRGB())));
		lines.add(scrollToModify);
		lines.add(shiftScrollsFaster);

        textBox.tooltip.set(new DLTooltip(lines, 200));
    }
}
