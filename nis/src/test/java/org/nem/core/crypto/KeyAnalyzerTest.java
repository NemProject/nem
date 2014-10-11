package org.nem.core.crypto;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public abstract class KeyAnalyzerTest {

	@Test
	public void isKeyCompressedReturnsTrueForCompressedPublicKey() {
		// Arrange:
		final KeyAnalyzer analyzer = this.getKeyAnalyzer();
		final KeyPair keyPair = new KeyPair();

		// Act + Assert:
		Assert.assertThat(analyzer.isKeyCompressed(keyPair.getPublicKey()), IsEqual.equalTo(true));
	}

	@Test
	public void isKeyCompressedReturnsFalseIfKeyHasWrongLength() {
		// Arrange:
		final KeyAnalyzer analyzer = this.getKeyAnalyzer();
		final KeyPair keyPair = new KeyPair();
		// TODO 20141010 J-B might be safer to get a real public key and resize it -1
		// TODO 20141011 BR -> J: you mean like this?
		final PublicKey key = new PublicKey(new byte[keyPair.getPublicKey().getRaw().length + 1]);

		// Act + Assert:
		Assert.assertThat(analyzer.isKeyCompressed(key), IsEqual.equalTo(false));
	}

	protected abstract KeyAnalyzer getKeyAnalyzer();

	@Before
	public abstract void initCryptoEngine();
}
