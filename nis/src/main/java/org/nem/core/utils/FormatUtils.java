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
}
