package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;

public class FeeForkTest {

	@Test
	public void canCreateObject() {
		// Arrange:
		final BlockHeight firstHeight = new BlockHeight(1);
		final BlockHeight secondHeight = new BlockHeight(2);

		// Act:
		final FeeFork feeFork = new FeeFork(firstHeight, secondHeight);

		// Assert:
		MatcherAssert.assertThat(feeFork.getFirstHeight(), IsSame.sameInstance(firstHeight));
		MatcherAssert.assertThat(feeFork.getSecondHeight(), IsSame.sameInstance(secondHeight));
	}
}
