package de.mrjulsen.crn.api.json;

import com.google.gson.*;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Type;

final class TextComponentAdapter implements JsonSerializer<Component>, JsonDeserializer<Component> {

    @Override
    public JsonElement serialize(Component value, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(Component.Serializer.toJson(value));
    }

    @Override
    public Component deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
        return Component.Serializer.fromJson(element);
    }
}
