package org.nem.core.crypto;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public abstract class KeyAnalyzerTest {

	@Test
	public void isKeyCompressedReturnsTrueForCompressedPublicKey() {
		// Arrange:
		final KeyAnalyzer analyzer = this.getKeyAnalyzer();
		final KeyPair keyPair = this.getCryptoEngine().createKeyGenerator().generateKeyPair();

		// Act + Assert:
		Assert.assertThat(analyzer.isKeyCompressed(keyPair.getPublicKey()), IsEqual.equalTo(true));
	}

	@Test
	public void isKeyCompressedReturnsFalseIfKeyHasWrongLength() {
		// Arrange:
		final KeyAnalyzer analyzer = this.getKeyAnalyzer();
		final KeyPair keyPair = this.getCryptoEngine().createKeyGenerator().generateKeyPair();
		final PublicKey key = new PublicKey(new byte[keyPair.getPublicKey().getRaw().length + 1]);

		// Act + Assert:
		Assert.assertThat(analyzer.isKeyCompressed(key), IsEqual.equalTo(false));
	}

	protected KeyAnalyzer getKeyAnalyzer() {
		return this.getCryptoEngine().createKeyAnalyzer();
	}

	protected abstract CryptoEngine getCryptoEngine();
}
