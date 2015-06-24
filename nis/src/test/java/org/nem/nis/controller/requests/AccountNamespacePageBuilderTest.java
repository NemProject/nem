package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;

public class AccountNamespacePageBuilderTest {

	@Test
	public void accountNamespacePageCanBeBuilt() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountNamespacePageBuilder builder = new AccountNamespacePageBuilder();

		// Act:
		builder.setAddress(address.getEncoded());
		builder.setParent("foo");
		final AccountNamespacePage page = builder.build();

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getParent(), IsEqual.equalTo(new NamespaceId("foo")));
	}
}
