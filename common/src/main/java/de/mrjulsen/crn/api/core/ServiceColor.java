package de.mrjulsen.crn.api.core;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.mcdragonlib.util.DLColor;

/** Picks the colour a service should be shown in. */
public final class ServiceColor {

    private ServiceColor() {}

    /**
     * The colour to use for a service, preferring the line's own colour over the category's and
     * falling back to a neutral default when neither has one. Either argument may be {@code null}.
     */
    public static DLColor of(LineRef line, CategoryRef category) {
        if (line != null && !line.color().isTransparent()) {
            return line.color();
        }
        if (category != null && !category.color().isTransparent()) {
            return category.color();
        }
        return Constants.COLOR_TRAIN_BACKGROUND;
    }
}
