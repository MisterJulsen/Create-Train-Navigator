/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.block.blockentity;

import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface IContraptionBlockEntity<T extends BlockEntity> {
    /**
     * Client-side only!
     * @param level
     * @param pos
     * @param state
     * @param contraption
     */
    void contraptionTick(Level level, BlockPos pos, BlockState state, Contraption contraption);
}
