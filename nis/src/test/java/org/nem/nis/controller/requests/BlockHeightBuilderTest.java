package org.nem.nis.controller.requests;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;

public class BlockHeightBuilderTest {

	@Test
	public void blockHeightCanBeBuilt() {
		// Arrange:
		final BlockHeightBuilder builder = new BlockHeightBuilder();

		// Act:
		builder.setHeight("12345");
		final BlockHeight blockHeight = builder.build();

		// Assert:
		MatcherAssert.assertThat(blockHeight, IsEqual.equalTo(new BlockHeight(12345)));
	}

	@Test
	public void blockHeightCanBeBuiltWhenUnset() {
		// Arrange:
		final BlockHeightBuilder builder = new BlockHeightBuilder();

		// Act:
		final BlockHeight blockHeight = builder.build();

		// Assert:
		MatcherAssert.assertThat(blockHeight, IsEqual.equalTo(new BlockHeight(Long.MAX_VALUE)));
	}
}
