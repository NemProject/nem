package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.ed25519.Ed25519Engine;

// TODO 20141006 BR: Beware of mocks. Since KeyPairContext mocks a lot, even tests not designed to use mocks will end up with mocks.
public class KeyPairTest {

	@Test
	public void ctorCanCreateNewKeyPair() {
		// Act:
		final KeyPair kp = new KeyPair();

		// Assert:
		Assert.assertThat(kp.hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPrivateKey(), IsNull.notNullValue());
		Assert.assertThat(kp.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPublicKey(), IsNull.notNullValue());
	}

	@Test
	public void ctorCreatesNewKeyPairWithCompressedPublicKey() {
		// Act:
		final KeyPair kp = new KeyPair();

		// Assert:
		Assert.assertThat(kp.getPublicKey().isCompressed(), IsEqual.equalTo(true));
	}

	// TODO 20141010 J-B: ctorCreatesDifferentInstancesWithDifferentKeys still seems valid

	@Test
	public void ctorCanCreateKeyPairAroundPrivateKey() {
		// Arrange:
		final KeyPair kp1 = new KeyPair();

		// Act:
		final KeyPair kp2 = new KeyPair(kp1.getPrivateKey());

		// Assert:
		Assert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPrivateKey(), IsEqual.equalTo(kp1.getPrivateKey()));
		Assert.assertThat(kp2.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
	}

	@Test
	public void ctorCanCreateKeyPairAroundPublicKey() {
		// Arrange:
		final KeyPair kp1 = new KeyPair();

		// Act:
		final KeyPair kp2 = new KeyPair(kp1.getPublicKey());

		// Assert:
		Assert.assertThat(kp2.hasPrivateKey(), IsEqual.equalTo(false));
		Assert.assertThat(kp2.getPrivateKey(), IsNull.nullValue());
		Assert.assertThat(kp2.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp2.getPublicKey(), IsEqual.equalTo(kp1.getPublicKey()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void generateKeyPairFailsIfPublicKeyIsNotCompressed() {
		// Arrange:
		final PublicKey publicKey = Mockito.mock(PublicKey.class);
		Mockito.when(publicKey.isCompressed()).thenReturn(false);

		// Act:
		new KeyPair(publicKey);
	}

	@Test
	public void ctorCreatesKeyGenerator() {
		// Arrange:
		final KeyPairContext context = new KeyPairContext();

		// Act:
		new KeyPair();

		// Assert:
		Mockito.verify(context.engine, Mockito.times(1)).createKeyGenerator();
	}

	@Test
	public void ctorDelegatesKeyGenerationToKeyGenerator() {
		// Arrange:
		final KeyPairContext context = new KeyPairContext();

		// Act:
		new KeyPair();

		// Assert:
		Mockito.verify(context.generator, Mockito.times(1)).generateKeyPair();
	}

	@Test
	public void ctorWithPrivateKeyDelegatesToDerivePublicKey() {
		// Arrange:
		final KeyPairContext context = new KeyPairContext();

		// Act:
		new KeyPair(context.privateKey);

		// Assert:
		Mockito.verify(context.generator, Mockito.times(1)).derivePublicKey(context.privateKey);
	}

	private class KeyPairContext {
		private final Ed25519Engine engine = Mockito.mock(Ed25519Engine.class);
		private final KeyAnalyzer analyzer = Mockito.mock(KeyAnalyzer.class);
		private final KeyGenerator generator = Mockito.mock(KeyGenerator.class);
		private final PrivateKey privateKey = Mockito.mock(PrivateKey.class);
		private final PublicKey publicKey = Mockito.mock(PublicKey.class);
		private final KeyPair keyPair;

		private KeyPairContext() {
			CryptoEngines.setDefaultEngine(this.engine);
			Mockito.when(this.analyzer.isKeyCompressed(Mockito.any())).thenReturn(true);
			Mockito.when(this.engine.createKeyAnalyzer()).thenReturn(this.analyzer);
			Mockito.when(this.publicKey.isCompressed()).thenReturn(true);
			this.keyPair = new KeyPair(this.privateKey, this.publicKey);
			Mockito.when(this.engine.createKeyGenerator()).thenReturn(this.generator);
			Mockito.when(this.generator.generateKeyPair()).thenReturn(this.keyPair);
			Mockito.when(this.generator.derivePublicKey(this.privateKey)).thenReturn(this.publicKey);
		}
	}
}
