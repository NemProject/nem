package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class SmartTileSupplyTypeTest {

	//region value

	@Test
	public void valueReturnsCorrespondingRawValueForKnownValue() {
		// Assert:
		Assert.assertThat(SmartTileSupplyType.CreateSmartTiles.value(), IsEqual.equalTo(1));
		Assert.assertThat(SmartTileSupplyType.DeleteSmartTiles.value(), IsEqual.equalTo(2));
	}

	//endregion

	//region isValid

	@Test
	public void isValidReturnsTrueForValidSupplyTypes() {
		// Assert:
		Assert.assertThat(SmartTileSupplyType.CreateSmartTiles.isValid(), IsEqual.equalTo(true));
		Assert.assertThat(SmartTileSupplyType.DeleteSmartTiles.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void isValidReturnsFalseForInvalidSupplyTypes() {
		Assert.assertThat(SmartTileSupplyType.Unknown.isValid(), IsEqual.equalTo(false));
	}

	//endregion

	//region fromValueOrDefault

	@Test
	public void fromValueOrDefaultReturnsCorrespondingEnumValueForKnownValue() {
		// Assert:
		Assert.assertThat(SmartTileSupplyType.fromValueOrDefault(1), IsEqual.equalTo(SmartTileSupplyType.CreateSmartTiles));
		Assert.assertThat(SmartTileSupplyType.fromValueOrDefault(2), IsEqual.equalTo(SmartTileSupplyType.DeleteSmartTiles));
	}

	@Test
	public void fromValueOrDefaultReturnsUnknownEnumValueForUnknownValue() {
		// Assert:
		Assert.assertThat(SmartTileSupplyType.fromValueOrDefault(0), IsEqual.equalTo(SmartTileSupplyType.Unknown));
		Assert.assertThat(SmartTileSupplyType.fromValueOrDefault(9999), IsEqual.equalTo(SmartTileSupplyType.Unknown));
	}

	//endregion
}
