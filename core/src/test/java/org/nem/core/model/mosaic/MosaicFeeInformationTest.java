package org.nem.core.model.mosaic;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Supply;

public class MosaicFeeInformationTest {

	@Test
	public void canCreateMosaicFeeInformation() {
		// Act:
		final MosaicFeeInformation information = new MosaicFeeInformation(new Supply(575), 3);

		// Assert:
		MatcherAssert.assertThat(information.getSupply(), IsEqual.equalTo(new Supply(575)));
		MatcherAssert.assertThat(information.getDivisibility(), IsEqual.equalTo(3));
	}
}
