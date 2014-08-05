package org.nem.nis.controller.viewmodels;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Address;
import org.nem.core.test.Utils;

public class AccountTransactionsPageBuilderTest {

	@Test
	public void accountPageCanBeBuilt() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountTransactionsPageBuilder builder = new AccountTransactionsPageBuilder();

		// Act:
		builder.setAddress(address.getEncoded());
		builder.setHash("ffeeddccbbaa99887766554433221100");
		final AccountTransactionsPage page = builder.build();

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getHash(), IsEqual.equalTo(Hash.fromHexString("ffeeddccbbaa99887766554433221100")));
	}
}