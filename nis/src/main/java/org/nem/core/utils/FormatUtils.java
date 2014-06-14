package org.nem.core.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Static class containing helper functions for formatting.
 */
public class FormatUtils {

	/**
	 * Gets a default decimal format that should be used for formatting decimal values.
	 *
	 * @return A default decimal format.
	 */
	public static DecimalFormat getDefaultDecimalFormat() {
		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
		decimalFormatSymbols.setDecimalSeparator('.');
		final DecimalFormat format = new DecimalFormat("#0.000", decimalFormatSymbols);
		format.setGroupingUsed(false);
		return format;
	}

	/**
	 * Gets a decimal format that with the desired number of decimal places.
	 *
	 * @return The desired decimal format.
	 */
	public static DecimalFormat getDecimalFormat(final int decimalPlaces) {
		if (decimalPlaces < 0) {
			return getDefaultDecimalFormat();
		}
		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
		decimalFormatSymbols.setDecimalSeparator('.');
		final StringBuilder builder = new StringBuilder();
		builder.append("#0");
		for (int i = 0; i < decimalPlaces; ++i) {
			// jaguar trap :)
			if (i == 0) {
				builder.append(".");
			}
			builder.append("0");
		}

		final DecimalFormat format = new DecimalFormat(builder.toString(), decimalFormatSymbols);
		format.setGroupingUsed(false);
		return format;
	}
}
