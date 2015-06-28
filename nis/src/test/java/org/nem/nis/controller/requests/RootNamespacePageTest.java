package org.nem.nis.controller.requests;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class RootNamespacePageTest {

	@Test
	public void canCreatePageWithDefaultValues() {
		// Act:
		final RootNamespacePage page = new RootNamespacePage(null, null);

		// Assert:
		Assert.assertThat(page.getId(), IsNull.nullValue());
		Assert.assertThat(page.getPageSize(), IsEqual.equalTo(25));
	}

	@Test
	public void canCreatePageWithSpecificValues() {
		// Act:
		final RootNamespacePage page = new RootNamespacePage("1234", "85");

		// Assert:
		Assert.assertThat(page.getId(), IsEqual.equalTo(1234L));
		Assert.assertThat(page.getPageSize(), IsEqual.equalTo(85));
	}

	@Test
	public void cannotCreatePageWithMalformedValues() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new RootNamespacePage("12x34", "85"), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new RootNamespacePage("1234", "8x5"), IllegalArgumentException.class);
	}

	@Test
	public void pageSizeIsConstrainedToAllowableRange() {
		// Assert:
		Assert.assertThat(getPageSize(-1000), IsEqual.equalTo(5));
		Assert.assertThat(getPageSize(4), IsEqual.equalTo(5));
		Assert.assertThat(getPageSize(5), IsEqual.equalTo(5));
		Assert.assertThat(getPageSize(55), IsEqual.equalTo(55));
		Assert.assertThat(getPageSize(100), IsEqual.equalTo(100));
		Assert.assertThat(getPageSize(101), IsEqual.equalTo(100));
		Assert.assertThat(getPageSize(1000), IsEqual.equalTo(100));
	}

	private static int getPageSize(final int value) {
		return new RootNamespacePage(null, Integer.toString(value)).getPageSize();
	}
}