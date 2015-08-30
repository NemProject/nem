package org.nem.nis.controller.requests;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;

public class AccountNamespaceTest {

	@Test
	public void accountNamespaceCanBeCreatedAroundOnlyValidAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountNamespace accountNamespace = new AccountNamespace(address.getEncoded(), null);

		// Assert:
		Assert.assertThat(accountNamespace.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(accountNamespace.getParent(), IsNull.nullValue());
	}

	@Test
	public void accountNamespaceCanBeCreatedAroundCompleteValidData() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountNamespace accountNamespace = new AccountNamespace(address.getEncoded(), "foo");

		// Assert:
		Assert.assertThat(accountNamespace.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(accountNamespace.getParent(), IsEqual.equalTo(new NamespaceId("foo")));
	}
}
