package org.nem.nis.controller.requests;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.MosaicId;

public class MosaicIdBuilderTest {

	@Test
	public void namespaceIdCanBeBuilt() {
		// Arrange:
		final MosaicIdBuilder builder = new MosaicIdBuilder();

		// Act:
		builder.setMosaicId("alice.vouchers * foo");
		final MosaicId mosaicId = builder.build();

		// Assert:
		Assert.assertThat(mosaicId.getNamespaceId().toString(), IsEqual.equalTo("alice.vouchers"));
		Assert.assertThat(mosaicId.getName(), IsEqual.equalTo("foo"));
	}
}
