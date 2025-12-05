package de.mrjulsen.crn.client.gui.overlay;

import java.util.Arrays;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

public enum OverlayPosition implements ITranslatableEnum {
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
    public Data getTranslationData() {
        return new Data(CreateRailwaysNavigator.MOD_ID, ENUM_NAME, name);
    }
}
