package de.mrjulsen.crn.client.gui.overlay;

import java.util.Arrays;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import net.minecraft.util.StringRepresentable;

public enum OverlayPosition implements StringRepresentable, ITranslatableEnum {
    TOP_LEFT("top_left", ModGuiIcons.TOP_LEFT),
    TOP_RIGHT("top_right", ModGuiIcons.TOP_RIGHT),
    BOTTOM_LEFT("bottom_left", ModGuiIcons.BOTTOM_LEFT),
    BOTTOM_RIGHT("bottom_right", ModGuiIcons.BOTTOM_RIGHT);

    private static final String ENUM_NAME = "overlay_position";
    private String name;
    private ModGuiIcons icon;

    OverlayPosition(String name, ModGuiIcons icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public ModGuiIcons getIcon() {
        return icon;
    }

    public static OverlayPosition getPositionByName(String name) {
        return Arrays.stream(OverlayPosition.values()).filter(x -> x.getName().equals(name)).findFirst().orElse(TOP_LEFT);
    }

    @Override
    public String getEnumName() {
        return ENUM_NAME;
    }

    @Override
    public String getEnumValueName() {
        return name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
