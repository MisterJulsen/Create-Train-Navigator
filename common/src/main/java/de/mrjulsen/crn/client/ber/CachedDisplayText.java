package de.mrjulsen.crn.client.ber;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.util.VariableManager;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class CachedDisplayText {

    private String input;
    private boolean isJson;
    private boolean hasVariables;
    private String resolved;
    private MutableComponent component;
    private MutableComponent applied;

    public void applyTo(BERLabel label, String input, AdvancedDisplayBlockEntity blockEntity) {
        MutableComponent text = get(input, blockEntity);
        if (text != applied) {
            label.text.set(text);
            applied = text;
        }
    }

    public MutableComponent get(String input, AdvancedDisplayBlockEntity blockEntity) {
        if (input == null) {
            input = "";
        }
        if (!input.equals(this.input)) {
            this.input = input;
            this.isJson = isJson(VariableManager.unescapePlaceholders(input));
            this.hasVariables = VariableManager.hasVariables(input);
            this.resolved = null;
            this.component = null;
        }

        if (component != null && !hasVariables) {
            return component;
        }

        String resolved = VariableManager.replacePlaceholders(input, blockEntity);
        if (component == null || !resolved.equals(this.resolved)) {
            this.resolved = resolved;
            this.component = parse(resolved);
        }
        return component;
    }

    private MutableComponent parse(String text) {
        if (text.isBlank()) {
            return TextUtils.empty();
        }
        if (isJson) {
            try {
                MutableComponent component = Component.Serializer.fromJson(JsonParser.parseString(text));
                if (component != null) {
                    return component;
                }
            } catch (Throwable ignored) {
            }
        }
        return TextUtils.text(text);
    }

    private static boolean isJson(String text) {
        if (text.isBlank()) {
            return false;
        }
        try {
            JsonElement element = JsonParser.parseString(text);
            if (element.isJsonObject() || element.isJsonArray()) {
                return true;
            }
            String trimmed = text.strip();
            return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && (trimmed.startsWith("\"") || trimmed.startsWith("'"));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
