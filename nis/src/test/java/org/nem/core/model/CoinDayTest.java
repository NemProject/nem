package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class CoinDayTest {
	//region constructor
	@Test
	public void ctorSetsFields() {
		// Arrange:
		final CoinDay coinDay = new CoinDay(new BlockHeight(10L), Amount.fromNem(1000L));

		// Assert:
		Assert.assertThat(coinDay.getHeight(), IsEqual.equalTo(new BlockHeight(10L)));
		Assert.assertThat(coinDay.getAmount(), IsEqual.equalTo(Amount.fromNem(1000L)));
	}
	//endregion


}
