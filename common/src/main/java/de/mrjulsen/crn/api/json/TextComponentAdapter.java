package de.mrjulsen.crn.api.json;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.lang.reflect.Type;

final class TextComponentAdapter implements JsonSerializer<Component>, JsonDeserializer<Component> {

    @Override
    public JsonElement serialize(Component value, Type type, JsonSerializationContext context) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow(JsonParseException::new);
    }

    @Override
    public Component deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element)
                .getOrThrow(JsonParseException::new);
    }
}
