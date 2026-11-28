/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.block.properties;

import java.util.Arrays;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

public enum ETimeDisplay implements ITranslatableEnum {
    ABS((byte)0, "abs"),
    ETA((byte)1, "eta");

    private byte id;
    private String name;

    private ETimeDisplay(byte id, String name) {
        this.id = id;
        this.name = name;
    }

    public byte getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static ETimeDisplay getById(int id) {
		for (ETimeDisplay timeDisplay : values())
			if (timeDisplay.getId() == id) return timeDisplay;
		return ABS;
    }

    @Override
    public String getSerializedName() {
        return getName();
    }

    @Override
    public Data getTranslationData() {
        return new Data(CreateRailwaysNavigator.MOD_ID, "time_display", name);
    }

}