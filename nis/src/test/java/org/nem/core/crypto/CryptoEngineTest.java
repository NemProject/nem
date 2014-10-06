package org.nem.core.crypto;

import org.hamcrest.core.IsInstanceOf;
import org.junit.*;

public abstract class CryptoEngineTest {

	@Test
	public void canCreateDsaSigner() {
		// Act:
		final DsaSigner signer = getCryptoEngine().createDsaSigner(new KeyPair());

		// Assert:
		Assert.assertThat(signer, IsInstanceOf.instanceOf(DsaSigner.class));
	}

	@Test
	public void canCreateKeyGenerator() {
		// Act:
		final KeyGenerator keyGenerator = getCryptoEngine().createKeyGenerator();

		// Assert:
		Assert.assertThat(keyGenerator, IsInstanceOf.instanceOf(KeyGenerator.class));
	}

	@Test
	public void canCreateKeyAnalyzer() {
		// Act:
		final KeyAnalyzer keyAnalyzer = getCryptoEngine().createKeyAnalyzer();

		// Assert:
		Assert.assertThat(keyAnalyzer, IsInstanceOf.instanceOf(KeyAnalyzer.class));
	}

	@Test
	public void canCreateIesCipher() {
		// Act:
		final IesCipher iesCipher = getCryptoEngine().createIesCipher(new KeyPair(), new KeyPair());

		// Assert:
		Assert.assertThat(iesCipher, IsInstanceOf.instanceOf(IesCipher.class));
	}

	protected abstract CryptoEngines.CryptoEngine getCryptoEngine();
}
