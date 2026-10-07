package de.mrjulsen.crn.api.json;

import com.google.gson.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.lang.reflect.Type;

final class MutableTextComponentAdapter implements JsonSerializer<MutableComponent>, JsonDeserializer<MutableComponent> {

    @Override
    public JsonElement serialize(MutableComponent value, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(Component.Serializer.toJson(value));
    }

    @Override
    public MutableComponent deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
        return Component.Serializer.fromJson(element);
    }
}
