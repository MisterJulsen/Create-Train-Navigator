package de.mrjulsen.crn.api.json;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.minecraft.core.BlockPos;

/** Writes a position as {@code {x, y, z}}, which survives being read by something that is not Java. */
final class BlockPosAdapter implements JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {

    @Override
    public JsonElement serialize(BlockPos value, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("x", value.getX());
        json.addProperty("y", value.getY());
        json.addProperty("z", value.getZ());
        return json;
    }

    @Override
    public BlockPos deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
        JsonObject json = element.getAsJsonObject();
        return new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
    }
}
