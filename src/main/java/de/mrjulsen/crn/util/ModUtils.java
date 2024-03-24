package de.mrjulsen.crn.util;

import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ModUtils {

    private static final Component TEXT_CONCAT = Utils.text("     ***     ");

    public static Component concat(Component... components) {
        if (components.length <= 0) {
            return Utils.emptyText();
        }

        MutableComponent c = components[0].copy();
        for (int i = 1; i < components.length; i++) {
            c.append(TEXT_CONCAT);
            c.append(components[i]);
        }
        return c;
    }
}
