package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class NamespaceIdMaxIdPageTest {

	@Test
	public void canCreatePageWithSpecificValues() {
		// Act:
		final NamespaceIdMaxIdPage page = new NamespaceIdMaxIdPage("1234", "85","foo");

		// Assert:
		Assert.assertThat(page.getId(), IsEqual.equalTo(1234L));
		Assert.assertThat(page.getPageSize(), IsEqual.equalTo(85));
		Assert.assertThat(page.getNamespaceId().toString(), IsEqual.equalTo("foo"));
	}

	@Test
	public void cannotCreatePageWithInvalidNamespaceId() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new NamespaceIdMaxIdPage("1234", "85", null), NullPointerException.class);
		ExceptionAssert.assertThrows(v -> new NamespaceIdMaxIdPage("1234", "85", "_foo"), IllegalArgumentException.class);
	}
}
