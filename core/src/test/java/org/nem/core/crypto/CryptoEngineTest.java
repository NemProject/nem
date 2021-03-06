package org.nem.core.crypto;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.*;

public abstract class CryptoEngineTest {

	@Test
	public void canGetCurve() {
		// Act:
		final Curve curve = this.getCryptoEngine().getCurve();

		// Assert:
		MatcherAssert.assertThat(curve, IsInstanceOf.instanceOf(Curve.class));
	}

	@Test
	public void canCreateDsaSigner() {
		// Act:
		final CryptoEngine engine = this.getCryptoEngine();
		final DsaSigner signer = engine.createDsaSigner(KeyPair.random(engine));

		// Assert:
		MatcherAssert.assertThat(signer, IsInstanceOf.instanceOf(DsaSigner.class));
	}

	@Test
	public void canCreateKeyGenerator() {
		// Act:
		final KeyGenerator keyGenerator = this.getCryptoEngine().createKeyGenerator();

		// Assert:
		MatcherAssert.assertThat(keyGenerator, IsInstanceOf.instanceOf(KeyGenerator.class));
	}

	@Test
	public void canCreateKeyAnalyzer() {
		// Act:
		final KeyAnalyzer keyAnalyzer = this.getCryptoEngine().createKeyAnalyzer();

		// Assert:
		MatcherAssert.assertThat(keyAnalyzer, IsInstanceOf.instanceOf(KeyAnalyzer.class));
	}

	@Test
	public void canCreateBlockCipher() {
		// Act:
		final CryptoEngine engine = this.getCryptoEngine();
		final BlockCipher blockCipher = engine.createBlockCipher(KeyPair.random(engine), KeyPair.random(engine));

		// Assert:
		MatcherAssert.assertThat(blockCipher, IsInstanceOf.instanceOf(BlockCipher.class));
	}

	protected abstract CryptoEngine getCryptoEngine();
}
