/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.mixin;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Create.class, remap = false)
public interface CreateAccessor {
  @Accessor("REGISTRATE")
  static CreateRegistrate getRegistrate() {
    throw new AssertionError();
  }
}
