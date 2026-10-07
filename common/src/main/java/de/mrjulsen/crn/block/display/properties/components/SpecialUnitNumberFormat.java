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

    public SpecialUnitNumberFormat(int decimals, String unit, boolean allowAuto, boolean allowMax, double minLimit, double maxLimit) {
        super(decimals);
        this.unit = unit;
        this.allowAuto = allowAuto;
        this.allowMax = allowMax;
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
    }

    public SpecialUnitNumberFormat(int decimals, String unit) {
        this(decimals, unit, false, false, 0, 0);
    }

    @Override
    public String format(double value) {
        double epsilon = 0.00001;

        if (allowAuto && Math.abs(value - minLimit) < epsilon) {
            return TextUtils.translate("gui.createrailwaysnavigator.common.auto").getString();
        }

        if (allowMax && Math.abs(value - maxLimit) < epsilon) {
            return TextUtils.translate("gui.createrailwaysnavigator.common.max").getString();
        }

        return String.format(Locale.US, "%." + decimals + "f%s", value, unit);
    }

    @Override
    public double parse(String input) throws NumberFormatException {
        String trimmedInput = input.trim();

        if (allowAuto && trimmedInput.equalsIgnoreCase(TextUtils.translate("gui.createrailwaysnavigator.common.auto").getString())) {
            return minLimit;
        }
        if (allowMax && trimmedInput.equalsIgnoreCase(TextUtils.translate("gui.createrailwaysnavigator.common.max").getString())) {
            return maxLimit;
        }

        String clean = trimmedInput.replace(unit, "").trim();
        return Double.parseDouble(clean);
    }
}
