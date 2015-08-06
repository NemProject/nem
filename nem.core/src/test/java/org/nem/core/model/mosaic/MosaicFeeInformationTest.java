package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Supply;

public class MosaicFeeInformationTest {

	@Test
	public void canCreateMosaicFeeInformation() {
		// Act:
		final MosaicFeeInformation information = new MosaicFeeInformation(new Supply(575), 3);

		// Assert:
		Assert.assertThat(information.getSupply(), IsEqual.equalTo(new Supply(575)));
		Assert.assertThat(information.getDivisibility(), IsEqual.equalTo(3));
	}
}