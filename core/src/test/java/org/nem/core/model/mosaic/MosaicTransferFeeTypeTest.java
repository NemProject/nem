package org.nem.core.model.mosaic;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class MosaicTransferFeeTypeTest {

	// region value

	@Test
	public void valueReturnsExpectedValue() {
		MatcherAssert.assertThat(MosaicTransferFeeType.Absolute.value(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(MosaicTransferFeeType.Percentile.value(), IsEqual.equalTo(2));
	}

	// endregion

	// region fromValue

	@Test
	public void fromValueReturnsCorrespondingEnumValueForKnownValue() {
		// Assert:
		MatcherAssert.assertThat(MosaicTransferFeeType.fromValue(1), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		MatcherAssert.assertThat(MosaicTransferFeeType.fromValue(2), IsEqual.equalTo(MosaicTransferFeeType.Percentile));
	}

	@Test
	public void fromValueOrDefaultReturnsUnknownEnumValueForUnknownValue() {
		// Assert:
		ExceptionAssert.assertThrows(v -> MosaicTransferFeeType.fromValue(0), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> MosaicTransferFeeType.fromValue(9999), IllegalArgumentException.class);
	}

	// endregion
}
