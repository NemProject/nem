package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;

public abstract class KeyGeneratorTest {

	@Test
	public void generateKeyPairReturnsNewKeyPair() {
		// Arrange:
		initCryptoEngine();
		final KeyGenerator generator = getKeyGenerator();

		// Act:
		final KeyPair kp = generator.generateKeyPair();

		// Assert:
		Assert.assertThat(kp.hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPrivateKey(), IsNull.notNullValue());
		Assert.assertThat(kp.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPublicKey(), IsNull.notNullValue());
	}

	@Test
	public void derivePublicKeyReturnsPublicKey() {
		// Arrange:
		initCryptoEngine();
		final KeyGenerator generator = getKeyGenerator();
		final KeyPair kp = generator.generateKeyPair();

		// Act:
		final PublicKey publicKey = generator.derivePublicKey(kp.getPrivateKey());

		// Assert:
		Assert.assertThat(publicKey, IsNull.notNullValue());
		Assert.assertThat(publicKey.getRaw(), IsEqual.equalTo(kp.getPublicKey().getRaw()));
	}

	protected abstract KeyGenerator getKeyGenerator();
	protected abstract void initCryptoEngine();
}
