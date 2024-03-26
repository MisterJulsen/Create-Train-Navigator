package de.mrjulsen.crn.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiPredicate;

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

    public static <T> boolean compareCollections(Collection<T> a, Collection<T> b, BiPredicate<T, T> compare) {
        if (a.size() != b.size()) {
            return false;
        }

        Iterator<T> i = a.iterator();
        Iterator<T> k = b.iterator();

        while (i.hasNext() && k.hasNext()) {
            if (!compare.test(i.next(), k.next())) {
                return false;
            }
        }

        return true;
    }
}
