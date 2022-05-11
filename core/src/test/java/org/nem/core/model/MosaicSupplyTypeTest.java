package org.nem.core.model;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class MosaicSupplyTypeTest {

	// region value

	@Test
	public void valueReturnsCorrespondingRawValueForKnownValue() {
		// Assert:
		MatcherAssert.assertThat(MosaicSupplyType.Create.value(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(MosaicSupplyType.Delete.value(), IsEqual.equalTo(2));
	}

	// endregion

	// region isValid

	@Test
	public void isValidReturnsTrueForValidSupplyTypes() {
		// Assert:
		MatcherAssert.assertThat(MosaicSupplyType.Create.isValid(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(MosaicSupplyType.Delete.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void isValidReturnsFalseForInvalidSupplyTypes() {
		MatcherAssert.assertThat(MosaicSupplyType.Unknown.isValid(), IsEqual.equalTo(false));
	}

	// endregion

	// region fromValueOrDefault

	@Test
	public void fromValueOrDefaultReturnsCorrespondingEnumValueForKnownValue() {
		// Assert:
		MatcherAssert.assertThat(MosaicSupplyType.fromValueOrDefault(1), IsEqual.equalTo(MosaicSupplyType.Create));
		MatcherAssert.assertThat(MosaicSupplyType.fromValueOrDefault(2), IsEqual.equalTo(MosaicSupplyType.Delete));
	}

	@Test
	public void fromValueOrDefaultReturnsUnknownEnumValueForUnknownValue() {
		// Assert:
		MatcherAssert.assertThat(MosaicSupplyType.fromValueOrDefault(0), IsEqual.equalTo(MosaicSupplyType.Unknown));
		MatcherAssert.assertThat(MosaicSupplyType.fromValueOrDefault(9999), IsEqual.equalTo(MosaicSupplyType.Unknown));
	}

	// endregion
}
