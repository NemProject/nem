package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.test.Utils;

public class AccountTransactionsIdBuilderTest {

	@Test
	public void idCanBeBuilt() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsIdBuilder builder = new AccountTransactionsIdBuilder();

		// Act:
		builder.setAddress(address.getEncoded());
		builder.setHash("ffeeddccbbaa99887766554433221100");
		final AccountTransactionsId id = builder.build();

		// Assert:
		Assert.assertThat(id.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(id.getHash(), IsEqual.equalTo(Hash.fromHexString("ffeeddccbbaa99887766554433221100")));
	}
}