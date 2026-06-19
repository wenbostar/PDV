package PDVGUI.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Creates decimal formats for file formats and numeric UI values.
 */
public final class DecimalFormats {

    private DecimalFormats() {
    }

    public static DecimalFormat create(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
    }
}
