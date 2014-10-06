package org.nem.core.crypto;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public abstract class KeyAnalyzerTest {

	@Test
	public void isKeyCompressedReturnsTrueForCompressedPublicKey() {
		// Arrange:
		initCryptoEngine();
		final KeyAnalyzer analyzer = getKeyAnalyzer();
		final KeyPair keyPair = new KeyPair();

		// Act + Assert:
		Assert.assertThat(analyzer.isKeyCompressed(keyPair.getPublicKey()), IsEqual.equalTo(true));
	}

	@Test
	public void isKeyCompressedReturnsFalseIfKeyHasWrongLength() {
		// Arrange:
		initCryptoEngine();
		final KeyAnalyzer analyzer = getKeyAnalyzer();
		final PublicKey key = new PublicKey(new byte[35]);

		// Act + Assert:
		Assert.assertThat(analyzer.isKeyCompressed(key), IsEqual.equalTo(false));
	}

	protected abstract KeyAnalyzer getKeyAnalyzer();
	protected abstract void initCryptoEngine();
}
