package de.mrjulsen.crn.client.gui.widgets.autocomplete;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.autocomplete.DLAutocompleteWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.autocomplete.IAutocompletionManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import net.minecraft.client.Minecraft;

public class TrainAutocomplete implements IAutocompletionManager<String> {

    private final List<String> names = new ArrayList<>();

    @Override
    public DLAutocompleteWindow<String> createWindow(DLWindowManager windowManager, DLRichTextEditBox textBox) {
        DLAutocompleteWindow<String> window = IAutocompletionManager.super.createWindow(windowManager, textBox);
        ModNetworkManager.GET_ALL_TRAIN_NAMES.send(NetworkDirection.toServer(), (result) -> {            
            Minecraft.getInstance().execute(() -> {
                names.clear();
                names.addAll(result.getTrainsNames().stream().sorted((a, b) -> a.compareToIgnoreCase(b)).toList());
                configureWindow(window, textBox);
            });
        }, () -> {});
        return window;
    }

    @Override
    public void configureWindow(DLAutocompleteWindow<String> window, DLRichTextEditBox textBox) {
        window.suggestions.set(names);
        window.filter.set(tag -> tag.toLowerCase().contains(textBox.text.get().getPlainText().toLowerCase()));
    }

    @Override
    public void closeWindow(DLAutocompleteWindow<String> window, DLRichTextEditBox textBox) {
        names.clear();
        IAutocompletionManager.super.closeWindow(window, textBox);
    }
    
}
