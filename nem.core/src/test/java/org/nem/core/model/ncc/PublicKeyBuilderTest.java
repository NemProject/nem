package org.nem.core.model.ncc;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.test.Utils;

public class PublicKeyBuilderTest {

	@Test
	public void publicKeyCanBeBuilt() {
		// Arrange:
		final PublicKey original = Utils.generateRandomPublicKey();
		final PublicKeyBuilder builder = new PublicKeyBuilder();

		// Act:
		builder.setPublicKey(original.toString());
		final PublicKey publicKey = builder.build();

		// Assert:
		MatcherAssert.assertThat(publicKey, IsEqual.equalTo(original));
	}
}
