/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.block.display.properties;

import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import net.minecraft.nbt.CompoundTag;

public interface IDisplaySettings extends INBTSerializable {

    CompoundTag serializeNbt();

    void deserializeNbt(CompoundTag nbt);

    void onChangeSettings(IDisplaySettings oldSettings);

 void buildGui(GuiBuilderContext context);
}
