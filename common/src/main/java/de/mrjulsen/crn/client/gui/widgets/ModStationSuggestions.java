package de.mrjulsen.crn.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

public class ModStationSuggestions extends ModCommandSuggestions {

	private List<String> viableStations;
	private String previous = "<>";
	private Font font;
	private boolean active;

	private List<Suggestion> currentSuggestions;
	private int yOffset;

	public ModStationSuggestions(Minecraft pMinecraft, Screen pScreen, EditBox pInput, Font pFont, List<String> viableStations, int yOffset) {
		super(pMinecraft, pScreen, pInput, pFont, true, true, 0, 7, false, 0xee_303030);
		this.font = pFont;
		this.viableStations = viableStations;
		this.yOffset = yOffset;
		currentSuggestions = new ArrayList<>();
		active = false;
	}

	public void tick() {
		if (suggestions == null)
			input.setSuggestion("");
		if (active == input.isFocused())
			return;
		active = input.isFocused();
		updateCommandInfo();
	}

	@Override
	public void updateCommandInfo() {
		String value = this.input.getValue();
		if (value.equals(previous))
			return;
		if (!active) {
			suggestions = null;
			return;
		}

		previous = value;
		currentSuggestions = viableStations.stream()
			.filter(ia -> !ia.equals(value) && ia.toLowerCase().startsWith(value.toLowerCase()))
			.map(s -> new Suggestion(new StringRange(0, s.length()), s))
			.toList();

		showSuggestions(false);
	}

	public void showSuggestions(boolean pNarrateFirstSuggestion) {
		if (currentSuggestions.isEmpty()) {
			suggestions = null;
			return;
		}

		int width = 0;
		for (Suggestion suggestion : currentSuggestions)
			width = Math.max(width, this.font.width(suggestion.getText()));
		int x = Mth.clamp(input.getScreenX(0), 0, input.getScreenX(0) + input.getInnerWidth() - width);
		suggestions = new ModCommandSuggestions.SuggestionsList(x, yOffset, width, currentSuggestions, false);
	}

	public EditBox getEditBox() {
		return input;
	}

	public void setYOffset(int offset) {
		this.yOffset = offset;
	}

}