package org.nem.nis.controller.viewmodels;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.test.*;

public class AccountPageTest {

	@Test
	public void accountPageCanBeCreatedAroundValidAddressAndNullTimestamp() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountPage page = new AccountPage(address.getEncoded(), null);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getTimeStamp(), IsNull.nullValue());
	}

	@Test
	public void accountPageCanBeCreatedAroundValidAddressAndNonNullTimestamp() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountPage page = new AccountPage(address.getEncoded(), "2452");

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getTimeStamp(), IsEqual.equalTo("2452"));
	}

	@Test
	public void accountPageCannotBeCreatedAroundInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountPage(null, null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new AccountPage("ABC", null), IllegalArgumentException.class);
	}
}