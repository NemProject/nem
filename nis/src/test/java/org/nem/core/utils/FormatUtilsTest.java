package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.text.DecimalFormat;

public class FormatUtilsTest {

	@Test
	public void defaultDecimalFormatFormatsValuesCorrectly() {
		// Arrange:
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();

		// Assert:
		Assert.assertThat(format.format(2.1234), IsEqual.equalTo("2.123"));
		Assert.assertThat(format.format(3.2345), IsEqual.equalTo("3.235"));
		Assert.assertThat(format.format(5012.0123), IsEqual.equalTo("5012.012"));
		Assert.assertThat(format.format(5.0126), IsEqual.equalTo("5.013"));
		Assert.assertThat(format.format(11.1234), IsEqual.equalTo("11.123"));
		Assert.assertThat(format.format(1), IsEqual.equalTo("1.000"));
		Assert.assertThat(format.format(8.0), IsEqual.equalTo("8.000"));
	}
}
