package de.mrjulsen.crn.client.gui.widgets.create;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.utility.CreateLang;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.flyout.FlyoutItemSelectionBrowser;
import de.mrjulsen.mcdragonlib.client.gui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLCycleButton;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLItemPicker;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLTooltip;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.properties.BooleanProperty;
import de.mrjulsen.mcdragonlib.util.properties.Property;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class CreateItemPicker<T> extends DLItemPicker<T> {

	protected final Component scrollToModify = CreateLang.translateDirect("gui.scrollInput.scrollToModify").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
	protected final Component shiftScrollsFaster = CreateLang.translateDirect("gui.scrollInput.shiftScrollsFaster").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
	protected final Component clickToSearch = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.click_to_search").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC);

    public final Property<Component> title = new Property<Component>(CreateLang.translateDirect("gui.scrollInput.defaultTitle"));
    public final Property<Component> hint = new Property<Component>(TextUtils.empty()).withAfterPropertyChangedCallback((o, n) -> updateTooltip());
    public final Property<Function<T, Component>> formatter = new Property<Function<T, Component>>(t -> TextUtils.text(t.toString()));
    public final Property<BiPredicate<T, String>> filter = new Property<>((i, s) -> String.valueOf(i).toLowerCase().contains(s.toLowerCase()));
    public final BooleanProperty renderArrow = new BooleanProperty(false);

    public CreateItemPicker(int x, int y, int w) {
        super(x, y, w, 18);
        this.componentRenderer.set(CreateItemPickerRenderer.INSTANCE);
        this.showButtons.set(false);
        this.drawFontShadow.set(true);

        this.formatter.withAfterPropertyChangedCallback((o, n) -> {
            this.textFormat.set(item -> {
                return formatter.get().apply(item.selectedItem.get().orElse(null));
            });
        });

        addEventListener(DLCycleButton.SelectedItemChanged.class, (s, e) -> {
            updateTooltip();
            return false;
        });

        addEventListener(DLGuiStandardEvents.ScrollEvent.class, (s, e) -> {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(AllSoundEvents.SCROLL_VALUE.getMainEvent(), 1.5f + 0.1f * (selectedIndex.get() - 0) / (items.size())));
            return false;
        });

        addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().createModal((mgr) -> new FlyoutItemSelectionBrowser<>(mgr, this, filter.get()));
            return false;
        });
    }    

    @Override
    protected void updateSelectedItem(boolean firstIfUnselected) {        
        updateTooltip();
        super.updateSelectedItem(firstIfUnselected);
    }
    
    
    public void changeValueOnScroll(int direction) {
        super.changeValueOnScroll(direction * -1);
    }

    protected void updateTooltip() {
        List<FormattedText> lines = new ArrayList<>();
		lines.add(title.get().plainCopy().withStyle(s -> s.withColor(AbstractSimiWidget.HEADER_RGB.getRGB())));

        int min = Math.max(0, Math.min(items.size(), selectedIndex.get() + 8) - 16);
        int max = Math.min(items.size(), min + 16);
        
        if (min > 0) {
            lines.add(TextUtils.text("> ...").withStyle(ChatFormatting.GRAY));
        }
        for (int i = min; i < max; i++) {
            T item = items.get(i);
		    if (selectedIndex.get() == i) {
                lines.add(TextUtils.text("-> ").append(formatter.get().apply(item).plainCopy()).withStyle(ChatFormatting.WHITE));
            } else {
                lines.add(TextUtils.text("> ").append(formatter.get().apply(item).plainCopy()).withStyle(ChatFormatting.GRAY));
            }
        }
        if (max < items.size()) {
            lines.add(TextUtils.text("> ...").withStyle(ChatFormatting.GRAY));
        }

		lines.add(hint.get().plainCopy().withStyle(s -> s.withColor(AbstractSimiWidget.HINT_RGB.getRGB())));
		lines.add(scrollToModify);
		lines.add(shiftScrollsFaster);
		lines.add(clickToSearch);

        tooltip.set(new DLTooltip(lines, 200));
    }
}
