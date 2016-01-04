package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class MosaicSupplyTypeTest {

	//region value

	@Test
	public void valueReturnsCorrespondingRawValueForKnownValue() {
		// Assert:
		Assert.assertThat(MosaicSupplyType.Create.value(), IsEqual.equalTo(1));
		Assert.assertThat(MosaicSupplyType.Delete.value(), IsEqual.equalTo(2));
	}

	//endregion

	//region isValid

	@Test
	public void isValidReturnsTrueForValidSupplyTypes() {
		// Assert:
		Assert.assertThat(MosaicSupplyType.Create.isValid(), IsEqual.equalTo(true));
		Assert.assertThat(MosaicSupplyType.Delete.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void isValidReturnsFalseForInvalidSupplyTypes() {
		Assert.assertThat(MosaicSupplyType.Unknown.isValid(), IsEqual.equalTo(false));
	}

	//endregion

	//region fromValueOrDefault

	@Test
	public void fromValueOrDefaultReturnsCorrespondingEnumValueForKnownValue() {
		// Assert:
		Assert.assertThat(MosaicSupplyType.fromValueOrDefault(1), IsEqual.equalTo(MosaicSupplyType.Create));
		Assert.assertThat(MosaicSupplyType.fromValueOrDefault(2), IsEqual.equalTo(MosaicSupplyType.Delete));
	}

	@Test
	public void fromValueOrDefaultReturnsUnknownEnumValueForUnknownValue() {
		// Assert:
		Assert.assertThat(MosaicSupplyType.fromValueOrDefault(0), IsEqual.equalTo(MosaicSupplyType.Unknown));
		Assert.assertThat(MosaicSupplyType.fromValueOrDefault(9999), IsEqual.equalTo(MosaicSupplyType.Unknown));
	}

	//endregion
}
