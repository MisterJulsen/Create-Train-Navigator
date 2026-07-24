package de.mrjulsen.crn.api.json;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import de.mrjulsen.mcdragonlib.util.DLColor;

final class DLColorAdapter implements JsonSerializer<DLColor>, JsonDeserializer<DLColor> {

    @Override
    public JsonElement serialize(DLColor value, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(String.format("#%08X", value.getAsARGB()));
    }

    @Override
    public DLColor deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
        String text = element.getAsString().trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        return DLColor.fromInt((int) Long.parseLong(text, 16));
    }
}
