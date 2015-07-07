package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;

public class AccountNamespaceMaxIdPageBuilderTest {

	@Test
	public void accountNamespacePageCanBeBuilt() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountNamespaceMaxIdPageBuilder builder = new AccountNamespaceMaxIdPageBuilder();

		// Act:
		builder.setAddress(address.getEncoded());
		builder.setParent("foo");
		builder.setId("85");
		final AccountNamespaceMaxIdPage page = builder.build();

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getParent(), IsEqual.equalTo(new NamespaceId("foo")));
		Assert.assertThat(page.getId(), IsEqual.equalTo(85L));
	}
}
