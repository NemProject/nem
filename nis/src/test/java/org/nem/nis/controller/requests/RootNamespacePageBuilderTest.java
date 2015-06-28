package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class RootNamespacePageBuilderTest {

	@Test
	public void rootNamespacePageCanBeBuilt() {
		// Arrange:
		final RootNamespacePageBuilder builder = new RootNamespacePageBuilder();

		// Act:
		builder.setId("12345");
		builder.setPageSize("73");
		final RootNamespacePage page = builder.build();

		// Assert:
		Assert.assertThat(page.getId(), IsEqual.equalTo(12345L));
		Assert.assertThat(page.getPageSize(), IsEqual.equalTo(73));
	}
}