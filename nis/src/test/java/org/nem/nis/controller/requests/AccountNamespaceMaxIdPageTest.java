package org.nem.nis.controller.requests;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.Utils;

public class AccountNamespaceMaxIdPageTest {

	@Test
	public void canCreatePageAroundCompleteValidData() {
		// Assert:
		assertCanCreatePageFromData(Utils.generateRandomAddress().getEncoded(), "foo", "85");
	}

	@Test
	public void canCreatePageAroundOnlyValidAddress() {
		// Assert:
		assertCanCreatePageFromData(Utils.generateRandomAddress().getEncoded(), null, null);
	}

	@Test
	public void canCreatePageAroundValidAddressAndValidParent() {
		// Assert:
		assertCanCreatePageFromData(Utils.generateRandomAddress().getEncoded(), "foo", null);
	}

	@Test
	public void canCreatePageAroundValidAddressAndValidId() {
		// Assert:
		assertCanCreatePageFromData(Utils.generateRandomAddress().getEncoded(), null, "85");
	}

	private static void assertCanCreatePageFromData(final String address, final String parent, final String id) {
		final AccountNamespaceMaxIdPage page = new AccountNamespaceMaxIdPage(address, parent, id);

		// Assert:
		Assert.assertThat(page.getAddress(), IsEqual.equalTo(Address.fromEncoded(address)));
		Assert.assertThat(page.getParent(), null == parent ? IsNull.nullValue() : IsEqual.equalTo(new NamespaceId(parent)));
		Assert.assertThat(page.getId(), null == id ? IsNull.nullValue() : IsEqual.equalTo(Long.valueOf(id)));
	}
}
