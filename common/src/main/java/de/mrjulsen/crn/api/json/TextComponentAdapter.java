/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

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
