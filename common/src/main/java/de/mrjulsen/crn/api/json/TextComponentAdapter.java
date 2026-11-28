/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

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
