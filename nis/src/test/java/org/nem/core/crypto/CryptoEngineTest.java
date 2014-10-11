package org.nem.core.crypto;

import org.hamcrest.core.IsInstanceOf;
import org.junit.*;

public abstract class CryptoEngineTest {

	@Test
	public void canCreateDsaSigner() {
		// Act:
		final DsaSigner signer = this.getCryptoEngine().createDsaSigner(new KeyPair());

		// Assert:
		Assert.assertThat(signer, IsInstanceOf.instanceOf(DsaSigner.class));
	}

	@Test
	public void canCreateKeyGenerator() {
		// Act:
		final KeyGenerator keyGenerator = this.getCryptoEngine().createKeyGenerator();

		// Assert:
		Assert.assertThat(keyGenerator, IsInstanceOf.instanceOf(KeyGenerator.class));
	}

	@Test
	public void canCreateKeyAnalyzer() {
		// Act:
		final KeyAnalyzer keyAnalyzer = this.getCryptoEngine().createKeyAnalyzer();

		// Assert:
		Assert.assertThat(keyAnalyzer, IsInstanceOf.instanceOf(KeyAnalyzer.class));
	}

	@Test
	public void canCreateBlockCipher() {
		// Act:
		final BlockCipher blockCipher = this.getCryptoEngine().createBlockCipher(new KeyPair(), new KeyPair());

		// Assert:
		Assert.assertThat(blockCipher, IsInstanceOf.instanceOf(BlockCipher.class));
	}

	protected abstract CryptoEngines.CryptoEngine getCryptoEngine();
}
