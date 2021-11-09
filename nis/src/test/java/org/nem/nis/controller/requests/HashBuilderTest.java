package org.nem.nis.controller.requests;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.test.Utils;

public class HashBuilderTest {

	@Test
	public void hashCanBeBuilt() {
		// Arrange:
		final Hash originalHash = Utils.generateRandomHash();
		final HashBuilder builder = new HashBuilder();

		// Act:
		builder.setHash(originalHash.toString());
		final Hash hash = builder.build();

		// Assert:
		MatcherAssert.assertThat(hash, IsEqual.equalTo(originalHash));
	}
}
