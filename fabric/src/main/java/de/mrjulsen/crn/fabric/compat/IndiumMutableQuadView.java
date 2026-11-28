/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.fabric.compat;

import link.infra.indium.renderer.mesh.EncodingFormat;
import link.infra.indium.renderer.mesh.MutableQuadViewImpl;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class IndiumMutableQuadView extends MutableQuadViewImpl {
    public IndiumMutableQuadView() {
        data = new int[EncodingFormat.TOTAL_STRIDE];
        clear();
    }

    @Override
    public void emitDirectly() {
        throw new NotImplementedException("IntermediateMutableQuadView.emitDirectly() is not implemented");
    }
}