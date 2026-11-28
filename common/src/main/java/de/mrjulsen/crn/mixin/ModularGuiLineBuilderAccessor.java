/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.foundation.gui.ModularGuiLine;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;

import net.minecraft.client.gui.Font;

@Mixin(ModularGuiLineBuilder.class)
public interface ModularGuiLineBuilderAccessor {
    @Accessor("target")
    ModularGuiLine crn$getTarget();
    @Accessor("font")
	Font crn$getFont();
    @Accessor("x")
	int crn$getX();
    @Accessor("y")
	int crn$getY();
}
