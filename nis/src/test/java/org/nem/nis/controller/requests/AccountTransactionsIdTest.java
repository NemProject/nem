package org.nem.nis.controller.requests;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.test.*;

public class AccountTransactionsIdTest {

	@Test
	public void idCanBeCreatedAroundOnlyValidAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsId id = new AccountTransactionsId(address.getEncoded(), null);

		// Assert:
		MatcherAssert.assertThat(id.getAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(id.getHash(), IsNull.nullValue());
	}

	@Test
	public void idCanBeCreatedAroundValidAddressAndValidHash() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsId id = new AccountTransactionsId(address.getEncoded(), "ffeeddccbbaa99887766554433221100");

		// Assert:
		MatcherAssert.assertThat(id.getAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(id.getHash(), IsEqual.equalTo(Hash.fromHexString("ffeeddccbbaa99887766554433221100")));
	}

	@Test
	public void idCannotBeCreatedAroundInvalidAddress() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new AccountTransactionsId(null, null), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new AccountTransactionsId("ABC", null), IllegalArgumentException.class);
	}
}
