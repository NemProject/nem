package org.nem.core.utils;

import java.text.DecimalFormat;
import java.util.function.Function;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class FormatUtilsTest {

	@Test
	public void defaultDecimalFormatFormatsValuesCorrectly() {
		// Arrange:
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();

		// Assert:
		MatcherAssert.assertThat(format.format(2.1234), IsEqual.equalTo("2.123"));
		MatcherAssert.assertThat(format.format(3.2345), IsEqual.equalTo("3.235"));
		MatcherAssert.assertThat(format.format(5012.0123), IsEqual.equalTo("5012.012"));
		MatcherAssert.assertThat(format.format(5.0126), IsEqual.equalTo("5.013"));
		MatcherAssert.assertThat(format.format(11.1234), IsEqual.equalTo("11.123"));
		MatcherAssert.assertThat(format.format(1), IsEqual.equalTo("1.000"));
		MatcherAssert.assertThat(format.format(8.0), IsEqual.equalTo("8.000"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void decimalFormatCannotBeSpecifiedWithNegativeDecimalPlaces() {
		// Assert:
		FormatUtils.getDecimalFormat(-1);
	}

	@Test
	public void decimalFormatWithZeroDecimalPlacesFormatsValuesCorrectly() {
		// Arrange:
		final DecimalFormat format = FormatUtils.getDecimalFormat(0);

		// Assert:
		MatcherAssert.assertThat(format.format(2.1234), IsEqual.equalTo("2"));
		MatcherAssert.assertThat(format.format(3.2345), IsEqual.equalTo("3"));
		MatcherAssert.assertThat(format.format(5012.0123), IsEqual.equalTo("5012"));
		MatcherAssert.assertThat(format.format(5.0126), IsEqual.equalTo("5"));
		MatcherAssert.assertThat(format.format(11.1234), IsEqual.equalTo("11"));
		MatcherAssert.assertThat(format.format(1), IsEqual.equalTo("1"));
		MatcherAssert.assertThat(format.format(8.0), IsEqual.equalTo("8"));
	}

	@Test
	public void decimalFormatWithCustomDecimalPlacesFormatsValuesCorrectly() {
		// Arrange:
		final DecimalFormat format = FormatUtils.getDecimalFormat(5);

		// Assert:
		assertFiveDecimalPlaceFormatting(format::format);
	}

	@Test
	public void formatWithCustomDecimalPlacesFormatsValuesCorrectly() {
		// Assert:
		assertFiveDecimalPlaceFormatting(d -> FormatUtils.format(d, 5));
	}

	private static void assertFiveDecimalPlaceFormatting(final Function<Double, String> format) {
		// Assert:
		MatcherAssert.assertThat(format.apply(2.1234), IsEqual.equalTo("2.12340"));
		MatcherAssert.assertThat(format.apply(3.2345), IsEqual.equalTo("3.23450"));
		MatcherAssert.assertThat(format.apply(5012.0123), IsEqual.equalTo("5012.01230"));
		MatcherAssert.assertThat(format.apply(5.0126), IsEqual.equalTo("5.01260"));
		MatcherAssert.assertThat(format.apply(11.1234), IsEqual.equalTo("11.12340"));
		MatcherAssert.assertThat(format.apply(1.), IsEqual.equalTo("1.00000"));
		MatcherAssert.assertThat(format.apply(8.0), IsEqual.equalTo("8.00000"));
	}
}
