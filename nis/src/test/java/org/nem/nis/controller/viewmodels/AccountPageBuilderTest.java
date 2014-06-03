package org.nem.nis.controller.viewmodels;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.test.Utils;

public class AccountPageBuilderTest {

	@Test
	public void accountPageCanBeBuilt() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountPageBuilder builder = new AccountPageBuilder();

		// Act:
		builder.setAddress(address.getEncoded());
		builder.setTimestamp("12345");
		final AccountPage page = builder.build();

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getTimestamp(), IsEqual.equalTo("12345"));
	}
}