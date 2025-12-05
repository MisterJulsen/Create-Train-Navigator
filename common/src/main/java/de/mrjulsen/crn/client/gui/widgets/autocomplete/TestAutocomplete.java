package de.mrjulsen.crn.client.gui.widgets.autocomplete;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.autocomplete.DLAutocompleteWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.autocomplete.IAutocompletionManager;

public class TestAutocomplete implements IAutocompletionManager<String> {

    @Override
    public void configureWindow(DLAutocompleteWindow<String> window, DLRichTextEditBox textBox) {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 34; i++) {
            data.add("Test " + i);
        }
        window.suggestions.set(data);
        window.filter.set(s -> s.toLowerCase().startsWith(textBox.text.get().getPlainText().toLowerCase()));
    }
    
}
