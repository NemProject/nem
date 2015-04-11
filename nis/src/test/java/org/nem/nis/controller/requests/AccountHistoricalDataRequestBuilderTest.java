package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;

public class AccountHistoricalDataRequestBuilderTest {

	@Test
	public void accountHistoricalDataRequestCanBeBuilt() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountHistoricalDataRequestBuilder builder = new AccountHistoricalDataRequestBuilder();

		// Act:
		builder.setAddress(address.getEncoded());
		builder.setStartHeight("10");
		builder.setEndHeight("20");
		builder.setIncrement("5");
		final AccountHistoricalDataRequest request = builder.build();

		// Assert:
		Assert.assertThat(request.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(20)));
		Assert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}
}
