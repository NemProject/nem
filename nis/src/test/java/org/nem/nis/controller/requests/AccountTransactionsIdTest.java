package org.nem.nis.controller.requests;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.test.*;

public class AccountTransactionsIdTest {

	@Test
	public void accountTransactionsPageCanBeCreatedAroundOnlyValidAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsId page = new AccountTransactionsId(address.getEncoded(), null, null);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsNull.nullValue());
		Assert.assertThat(page.getId(), IsNull.nullValue());
	}

	@Test
	public void accountTransactionsPageCanBeCreatedAroundValidAddressAndValidHash() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsId page = new AccountTransactionsId(address.getEncoded(), "ffeeddccbbaa99887766554433221100", null);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(Hash.fromHexString("ffeeddccbbaa99887766554433221100")));
		Assert.assertThat(page.getId(), IsNull.nullValue());
	}

	@Test
	public void accountTransactionsPageCanBeCreatedAroundValidAddressAndValidId() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsId page = new AccountTransactionsId(address.getEncoded(), null, "12345");

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsNull.nullValue());
		Assert.assertThat(page.getId(), IsEqual.equalTo(12345L));
	}

	@Test
	public void accountTransactionsPageCanBeCreatedAroundCompleteValidData() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsId page = new AccountTransactionsId(address.getEncoded(), "ffeeddccbbaa99887766554433221100", "12345");

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(Hash.fromHexString("ffeeddccbbaa99887766554433221100")));
		Assert.assertThat(page.getId(), IsEqual.equalTo(12345L));
	}

	@Test
	public void accountTransactionsPageCannotBeCreatedAroundInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountTransactionsId(null, null, null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new AccountTransactionsId("ABC", null, null), IllegalArgumentException.class);
	}
}