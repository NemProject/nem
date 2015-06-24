package org.nem.nis.controller.requests;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;

public class AccountNamespacePageTest {

	@Test
	public void accountNamespacePageCanBeCreatedAroundOnlyValidAddress() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountNamespacePage page = new AccountNamespacePage(address.getEncoded(), null);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getParent(), IsNull.nullValue());
	}

	@Test
	public void accountNamespacePageCanBeCreatedAroundCompleteValidData() {
		// Act:
		final Address address = Utils.generateRandomAddress();
		final AccountNamespacePage page = new AccountNamespacePage(address.getEncoded(), "foo");

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(page.getParent(), IsEqual.equalTo(new NamespaceId("foo")));
	}
}
