package org.nem.nis.controller.requests;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class DefaultPageTest {

	@Test
	public void canCreatePageWithDefaultValues() {
		// Act:
		final DefaultPage page = new DefaultPage(null, null);

		// Assert:
		MatcherAssert.assertThat(page.getId(), IsNull.nullValue());
		MatcherAssert.assertThat(page.getPageSize(), IsEqual.equalTo(25));
	}

	@Test
	public void canCreatePageWithSpecificValues() {
		// Act:
		final DefaultPage page = new DefaultPage("1234", "85");

		// Assert:
		MatcherAssert.assertThat(page.getId(), IsEqual.equalTo(1234L));
		MatcherAssert.assertThat(page.getPageSize(), IsEqual.equalTo(85));
	}

	@Test
	public void cannotCreatePageWithMalformedValues() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new DefaultPage("12x34", "85"), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new DefaultPage("1234", "8x5"), IllegalArgumentException.class);
	}

	@Test
	public void pageSizeIsConstrainedToAllowableRange() {
		// Assert:
		MatcherAssert.assertThat(getPageSize(-1000), IsEqual.equalTo(5));
		MatcherAssert.assertThat(getPageSize(4), IsEqual.equalTo(5));
		MatcherAssert.assertThat(getPageSize(5), IsEqual.equalTo(5));
		MatcherAssert.assertThat(getPageSize(55), IsEqual.equalTo(55));
		MatcherAssert.assertThat(getPageSize(100), IsEqual.equalTo(100));
		MatcherAssert.assertThat(getPageSize(101), IsEqual.equalTo(100));
		MatcherAssert.assertThat(getPageSize(1000), IsEqual.equalTo(100));
	}

	private static int getPageSize(final int value) {
		return new DefaultPage(null, Integer.toString(value)).getPageSize();
	}
}
