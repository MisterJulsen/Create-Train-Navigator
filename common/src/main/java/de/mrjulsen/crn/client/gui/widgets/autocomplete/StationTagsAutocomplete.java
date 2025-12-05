package de.mrjulsen.crn.client.gui.widgets.autocomplete;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.network.packets.pain.GetAllStationsAsTagsPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.gui.widgets.components.DLRichTextEditBox;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.autocomplete.DLAutocompleteWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.autocomplete.IAutocompletionManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import net.minecraft.client.Minecraft;

public class StationTagsAutocomplete implements IAutocompletionManager<StationTag> {

    private final List<StationTag> tags = new ArrayList<>();

    @Override
    public DLAutocompleteWindow<StationTag> createWindow(DLWindowManager windowManager, DLRichTextEditBox textBox) {
        DLAutocompleteWindow<StationTag> window = IAutocompletionManager.super.createWindow(windowManager, textBox);
        ModNetworkManager.GET_ALL_STATIONS_AS_STATION_TAGS.send(NetworkDirection.toServer(), new GetAllStationsAsTagsPacketData.Request(true), (result) -> {            
            Minecraft.getInstance().execute(() -> {
                tags.clear();
                tags.addAll(result.getTags().stream().sorted((a, b) -> a.getTagName().get().compareToIgnoreCase(b.getTagName().get())).toList());
                configureWindow(window, textBox);
            });
        }, () -> {});
        return window;
    }

    @Override
    public void configureWindow(DLAutocompleteWindow<StationTag> window, DLRichTextEditBox textBox) {
        window.suggestions.set(tags);
        window.filter.set(tag -> tag.getTagName().get().toLowerCase().contains(textBox.text.get().getPlainText().toLowerCase()));
    }

    @Override
    public void closeWindow(DLAutocompleteWindow<StationTag> window, DLRichTextEditBox textBox) {
        tags.clear();
        IAutocompletionManager.super.closeWindow(window, textBox);
    }
    
}
