package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.test.*;

public class AccountIdTest {

	@Test
	public void accountIdCanBeCreatedAroundValidAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountId page = new AccountId(address.getEncoded());

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void accountIdCannotBeCreatedAroundInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountId(null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new AccountId("ABC"), IllegalArgumentException.class);
	}
}