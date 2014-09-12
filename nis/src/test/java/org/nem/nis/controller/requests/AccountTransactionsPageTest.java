package org.nem.nis.controller.requests;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.test.*;

public class AccountTransactionsPageTest {

	@Test
	public void accountTransactionsPageCanBeCreatedAroundValidAddressAndNullHash() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsPage page = new AccountTransactionsPage(address.getEncoded(), null);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsNull.nullValue());
	}

	@Test
	public void accountTransactionsPageCanBeCreatedAroundValidAddressAndNonNullHash() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsPage page = new AccountTransactionsPage(address.getEncoded(), "ffeeddccbbaa99887766554433221100");

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(Hash.fromHexString("ffeeddccbbaa99887766554433221100")));
	}

	@Test
	public void accountTransactionsPageCannotBeCreatedAroundInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountTransactionsPage(null, null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new AccountTransactionsPage("ABC", null), IllegalArgumentException.class);
	}
}