package org.nem.core.crypto;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;

public class KeyPairTest {

	// region basic construction

	@Test
	public void ctorCanCreateNewKeyPair() {
		// Act:
		final KeyPair kp = new KeyPair();

		// Assert:
		MatcherAssert.assertThat(kp.hasPrivateKey(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(kp.getPrivateKey(), IsNull.notNullValue());
		MatcherAssert.assertThat(kp.getPublicKey(), IsNull.notNullValue());
	}

	@Test
	public void ctorCanCreateKeyPairAroundPrivateKey() {
		// Arrange:
		final KeyPair kp1 = new KeyPair();

		// Act:
		final KeyPair kp2 = new KeyPair(kp1.getPrivateKey());

		// Assert:
		MatcherAssert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(kp2.getPrivateKey(), IsEqual.equalTo(kp1.getPrivateKey()));
		MatcherAssert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
	}

	@Test
	public void ctorCanCreateKeyPairAroundPublicKey() {
		// Arrange:
		final KeyPair kp1 = new KeyPair();

		// Act:
		final KeyPair kp2 = new KeyPair(kp1.getPublicKey());

		// Assert:
		MatcherAssert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(kp2.getPrivateKey(), IsNull.nullValue());
		MatcherAssert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
	}

	// endregion

	@Test
	public void ctorCreatesDifferentInstancesWithDifferentKeys() {
		// Act:
		final KeyPair kp1 = new KeyPair();
		final KeyPair kp2 = new KeyPair();

		// Assert:
		MatcherAssert.assertThat(kp2.getPrivateKey(), IsNot.not(IsEqual.equalTo(kp1.getPrivateKey())));
		MatcherAssert.assertThat(kp2.getPublicKey(), IsNot.not(IsEqual.equalTo(kp1.getPublicKey())));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorFailsIfPublicKeyIsNotCompressed() {
		// Arrange:
		final KeyPairContext context = new KeyPairContext();
		final PublicKey publicKey = Mockito.mock(PublicKey.class);
		Mockito.when(context.analyzer.isKeyCompressed(publicKey)).thenReturn(false);

		// Act:
		new KeyPair(publicKey, context.engine);
	}

	// region delegation

	@Test
	public void ctorCreatesKeyGenerator() {
		// Arrange:
		final KeyPairContext context = new KeyPairContext();

		// Act:
		KeyPair.random(context.engine);

		// Assert:
		Mockito.verify(context.engine, Mockito.times(1)).createKeyGenerator();
	}

	@Test
	public void ctorDelegatesKeyGenerationToKeyGenerator() {
		// Arrange:
		final KeyPairContext context = new KeyPairContext();

		// Act:
		KeyPair.random(context.engine);

		// Assert:
		Mockito.verify(context.generator, Mockito.times(1)).generateKeyPair();
	}

	@Test
	public void ctorWithPrivateKeyDelegatesToDerivePublicKey() {
		// Arrange:
		final KeyPairContext context = new KeyPairContext();

		// Act:
		new KeyPair(context.privateKey, context.engine);

		// Assert:
		Mockito.verify(context.generator, Mockito.times(1)).derivePublicKey(context.privateKey);
	}

	private class KeyPairContext {
		private final CryptoEngine engine = Mockito.mock(CryptoEngine.class);
		private final KeyAnalyzer analyzer = Mockito.mock(KeyAnalyzer.class);
		private final KeyGenerator generator = Mockito.mock(KeyGenerator.class);
		private final PrivateKey privateKey = Mockito.mock(PrivateKey.class);
		private final PublicKey publicKey = Mockito.mock(PublicKey.class);
		private final KeyPair keyPair1 = Mockito.mock(KeyPair.class);

		private KeyPairContext() {
			Mockito.when(this.analyzer.isKeyCompressed(Mockito.any())).thenReturn(true);
			Mockito.when(this.engine.createKeyAnalyzer()).thenReturn(this.analyzer);
			Mockito.when(this.engine.createKeyGenerator()).thenReturn(this.generator);
			Mockito.when(this.generator.generateKeyPair()).thenReturn(this.keyPair1);
			Mockito.when(this.generator.derivePublicKey(this.privateKey)).thenReturn(this.publicKey);
		}
	}

	// endregion
}
