/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.network.packets;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;

public final class EmptyNetworkPacketData extends NetworkPacketData {
    public EmptyNetworkPacketData(DLStatus status) {
        super(status);
    }
    
    public EmptyNetworkPacketData() {
        super(DLStatus.OK);
    }

    @Override
    protected void write(CompoundTag nbt) {
    }

    @Override
    protected void read(CompoundTag nbt) {
    }        
}
