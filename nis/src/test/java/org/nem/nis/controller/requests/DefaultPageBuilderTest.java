package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class DefaultPageBuilderTest {

	@Test
	public void defaultPageCanBeBuilt() {
		// Arrange:
		final DefaultPageBuilder builder = new DefaultPageBuilder();

		// Act:
		builder.setId("12345");
		builder.setPageSize("73");
		final DefaultPage page = builder.build();

		// Assert:
		Assert.assertThat(page.getId(), IsEqual.equalTo(12345L));
		Assert.assertThat(page.getPageSize(), IsEqual.equalTo(73));
	}
}