package org.nem.nis.controller.requests;

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
		Assert.assertThat(request.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(20)));
		Assert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
	}

	@Test
	public void canCreateAccountHistoricalDataRequestWithEndHeightEqualToStartHeight() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountHistoricalDataRequest request = new AccountHistoricalDataRequest(address.toString(), "10", "10", "5");

		// Assert:
		Assert.assertThat(request.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(request.getStartHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(request.getEndHeight(), IsEqual.equalTo(new BlockHeight(10)));
		Assert.assertThat(request.getIncrement(), IsEqual.equalTo(5L));
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

	@Test
	public void cannotCreateAccountHistoricalDataRequestWithNullStartHeight() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountHistoricalDataRequest(
				Utils.generateRandomAddress().toString(), null, "20", "5"),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateAccountHistoricalDataRequestWithNullEndHeight() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountHistoricalDataRequest(
						Utils.generateRandomAddress().toString(), "10", null, "5"),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateAccountHistoricalDataRequestWithNullIncrement() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountHistoricalDataRequest(
						Utils.generateRandomAddress().toString(), "10", "20", null),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateAccountHistoricalDataRequestWithStartHeightLargerThanEndHeight() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountHistoricalDataRequest(
				Utils.generateRandomAddress().toString(), "10", "9", "5"),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateAccountHistoricalDataRequestWithZeroIncrement() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountHistoricalDataRequest(
						Utils.generateRandomAddress().toString(), "10", "20", "0"),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateAccountHistoricalDataRequestWithNegativeIncrement() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountHistoricalDataRequest(
						Utils.generateRandomAddress().toString(), "10", "20", "-1"),
				IllegalArgumentException.class);
	}
}
