package org.nem.nis.controller.requests;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;

public class AccountNamespaceBuilderTest {

	@Test
	public void accountNamespacePageCanBeBuilt() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountNamespaceBuilder builder = new AccountNamespaceBuilder();

		// Act:
		builder.setAddress(address.getEncoded());
		builder.setParent("foo");
		final AccountNamespace accountNamespace = builder.build();

		// Assert:
		MatcherAssert.assertThat(accountNamespace.getAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(accountNamespace.getParent(), IsEqual.equalTo(new NamespaceId("foo")));
	}
}
