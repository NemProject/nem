package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.test.Utils;

public class AccountTransactionsIdBuilderTest {

	@Test
	public void accountPageCanBeBuilt() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsIdBuilder builder = new AccountTransactionsIdBuilder();

		// Act:
		builder.setAddress(address.getEncoded());
		builder.setHash("ffeeddccbbaa99887766554433221100");
		builder.setId("12345");
		final AccountTransactionsId page = builder.build();

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(Hash.fromHexString("ffeeddccbbaa99887766554433221100")));
		Assert.assertThat(page.getId(), IsEqual.equalTo(12345L));
	}
}