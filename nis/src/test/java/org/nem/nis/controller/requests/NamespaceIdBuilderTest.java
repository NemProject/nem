package org.nem.nis.controller.requests;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.namespace.NamespaceId;

public class NamespaceIdBuilderTest {

	@Test
	public void namespaceIdCanBeBuilt() {
		// Arrange:
		final NamespaceIdBuilder builder = new NamespaceIdBuilder();

		// Act:
		builder.setNamespace("a.b.c");
		final NamespaceId namespaceId = builder.build();

		// Assert:
		MatcherAssert.assertThat(namespaceId.toString(), IsEqual.equalTo("a.b.c"));
	}
}
