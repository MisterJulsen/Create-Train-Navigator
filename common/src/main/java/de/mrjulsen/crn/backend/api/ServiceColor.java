package de.mrjulsen.crn.backend.api;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.mcdragonlib.util.DLColor;

/**
 * Resolves the colour to show for a service.
 * <p>
 * A line and a category both carry a colour and either may be left unset, so neither alone answers
 * what to paint. The line is the more specific of the two and wins where it has an opinion; the
 * category covers everything running under it; and a service with no colour at all still has to
 * render as something.
 */
public final class ServiceColor {

    private ServiceColor() {}

    /** The colour of the given line, falling back to the category's and then to the default. */
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
