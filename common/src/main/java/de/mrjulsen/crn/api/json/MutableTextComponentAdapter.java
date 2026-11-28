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
import net.minecraft.network.chat.MutableComponent;

import java.lang.reflect.Type;

final class MutableTextComponentAdapter implements JsonSerializer<MutableComponent>, JsonDeserializer<MutableComponent> {

    @Override
    public JsonElement serialize(MutableComponent value, Type type, JsonSerializationContext context) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow(JsonParseException::new);
    }

    @Override
    public MutableComponent deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element)
                .getOrThrow(JsonParseException::new).copy();
    }
}
