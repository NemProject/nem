package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NamespaceIdMaxIdPageBuilderTest {

	@Test
	public void pageCanBeBuilt() {
		// Arrange:
		final NamespaceIdMaxIdPageBuilder builder = new NamespaceIdMaxIdPageBuilder();

		// Act:
		builder.setId("12345");
		builder.setPageSize("73");
		builder.setNamespace("foo");
		final NamespaceIdMaxIdPage page = builder.build();

		// Assert:
		Assert.assertThat(page.getId(), IsEqual.equalTo(12345L));
		Assert.assertThat(page.getPageSize(), IsEqual.equalTo(73));
		Assert.assertThat(page.getNamespaceId().toString(), IsEqual.equalTo("foo"));
	}
}
