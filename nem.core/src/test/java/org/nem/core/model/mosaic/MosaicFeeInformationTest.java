package org.nem.core.model.mosaic;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.Supply;
import org.nem.core.test.Utils;

public class MosaicFeeInformationTest {

	@Test
	public void canCreateMosaicFeeInformation() {
		// Act:
		final MosaicTransferFeeInfo feeInfo = Utils.createMosaicTransferFeeInfo();
		final MosaicFeeInformation information = new MosaicFeeInformation(new Supply(575), 3, feeInfo);

		// Assert:
		Assert.assertThat(information.getSupply(), IsEqual.equalTo(new Supply(575)));
		Assert.assertThat(information.getDivisibility(), IsEqual.equalTo(3));
		Assert.assertThat(information.getTransferFeeInfo(), IsSame.sameInstance(feeInfo));
	}
}