package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NamespacePageBuilderTest {

	@Test
	public void rootNamespacePageCanBeBuilt() {
		// Arrange:
		final NamespacePageBuilder builder = new NamespacePageBuilder();

		// Act:
		builder.setId("12345");
		builder.setPageSize("73");
		final NamespacePage page = builder.build();

		// Assert:
		Assert.assertThat(page.getId(), IsEqual.equalTo(12345L));
		Assert.assertThat(page.getPageSize(), IsEqual.equalTo(73));
	}
}