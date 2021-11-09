package org.nem.nis.controller.requests;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;

public class AccountHistoricalDataRequestTest {

	@Test
	public void canCreateAccountHistoricalDataRequestFromValidParameters() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountHistoricalDataRequest request = new AccountHistoricalDataRequest(address.toString(), "10", "20", "5");

		// Assert:
		MatcherAssert.assertThat(request.getAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		MatcherAssert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(20)));
		MatcherAssert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}

	@Test
	public void cannotCreateAccountHistoricalDataRequestWithInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountHistoricalDataRequest("FOO", "10", "20", "5"), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateAccountHistoricalDataRequestWithNullAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountHistoricalDataRequest(null, "10", "20", "5"), IllegalArgumentException.class);
	}
}
