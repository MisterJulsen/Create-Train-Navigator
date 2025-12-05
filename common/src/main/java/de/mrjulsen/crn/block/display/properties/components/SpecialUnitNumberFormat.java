package de.mrjulsen.crn.block.display.properties.components;

import java.util.Locale;

import de.mrjulsen.mcdragonlib.client.gui.widgets.util.INumberFormatAdapter.DecimalNumberFormat;
import de.mrjulsen.mcdragonlib.util.TextUtils;

public class SpecialUnitNumberFormat extends DecimalNumberFormat {

    protected final String unit;
    protected final boolean allowAuto;
    protected final boolean allowMax;
    protected final double minLimit;
    protected final double maxLimit;

    /**
     * Konstruktor mit allen Parametern.
     * @param minLimit Der Wert, der als "Auto" angezeigt werden soll (wenn allowAuto true).
     * @param maxLimit Der Wert, der als "Max" angezeigt werden soll (wenn allowMax true).
     */
    public SpecialUnitNumberFormat(int decimals, String unit, boolean allowAuto, boolean allowMax, double minLimit, double maxLimit) {
        super(decimals);
        this.unit = unit;
        this.allowAuto = allowAuto;
        this.allowMax = allowMax;
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
    }

    // Optionaler vereinfachter Konstruktor (falls keine Limits benötigt werden, verhält er sich wie vorher)
    public SpecialUnitNumberFormat(int decimals, String unit) {
        this(decimals, unit, false, false, 0, 0);
    }

    @Override
    public String format(double value) {
        // Wir nutzen ein kleines Epsilon für den Double-Vergleich, um Rundungsfehler zu vermeiden
        double epsilon = 0.00001;

        // Prüfung für "Auto" (niedrigste Zahl)
        if (allowAuto && Math.abs(value - minLimit) < epsilon) {
            return TextUtils.translate("gui.createrailwaysnavigator.common.auto").getString();
        }

        // Prüfung für "Max" (höchste Zahl)
        if (allowMax && Math.abs(value - maxLimit) < epsilon) {
            return TextUtils.translate("gui.createrailwaysnavigator.common.max").getString();
        }

        // Standard-Formatierung
        return String.format(Locale.US, "%." + decimals + "f%s", value, unit);
    }

    @Override
    public double parse(String input) throws NumberFormatException {
        // Eingabe bereinigen
        String trimmedInput = input.trim();

        // Rückwärts-Parsing für Auto/Max
        if (allowAuto && trimmedInput.equalsIgnoreCase(TextUtils.translate("gui.createrailwaysnavigator.common.auto").getString())) {
            return minLimit;
        }
        if (allowMax && trimmedInput.equalsIgnoreCase(TextUtils.translate("gui.createrailwaysnavigator.common.max").getString())) {
            return maxLimit;
        }

        // Standard-Parsing
        String clean = trimmedInput.replace(unit, "").trim();
        return Double.parseDouble(clean);
    }
}
