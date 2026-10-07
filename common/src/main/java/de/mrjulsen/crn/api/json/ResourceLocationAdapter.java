package de.mrjulsen.crn.api.json;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.minecraft.resources.ResourceLocation;

final class ResourceLocationAdapter implements JsonSerializer<ResourceLocation>, JsonDeserializer<ResourceLocation> {

    @Override
    public JsonElement serialize(ResourceLocation value, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(value.toString());
    }

    @Override
    public ResourceLocation deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
        return new ResourceLocation(element.getAsString());
    }
}
