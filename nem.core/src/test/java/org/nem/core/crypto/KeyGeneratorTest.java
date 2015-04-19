package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;

public abstract class KeyGeneratorTest {

	@Test
	public void generateKeyPairReturnsNewKeyPair() {
		// Arrange:
		final KeyGenerator generator = this.getKeyGenerator();

		// Act:
		final KeyPair kp = generator.generateKeyPair();

		// Assert:
		Assert.assertThat(kp.hasPrivateKey(), IsEqual.equalTo(true));
		Assert.assertThat(kp.getPrivateKey(), IsNull.notNullValue());
		Assert.assertThat(kp.getPublicKey(), IsNull.notNullValue());
	}

	@Test
	public void derivePublicKeyReturnsPublicKey() {
		// Arrange:
		final KeyGenerator generator = this.getKeyGenerator();
		final KeyPair kp = generator.generateKeyPair();

		// Act:
		final PublicKey publicKey = generator.derivePublicKey(kp.getPrivateKey());

		// Assert:
		Assert.assertThat(publicKey, IsNull.notNullValue());
		Assert.assertThat(publicKey.getRaw(), IsEqual.equalTo(kp.getPublicKey().getRaw()));
	}

	@Test
	public void generateKeyPairCreatesDifferentInstancesWithDifferentKeys() {
		// Act:
		final KeyPair kp1 = this.getKeyGenerator().generateKeyPair();
		final KeyPair kp2 = this.getKeyGenerator().generateKeyPair();

		// Assert:
		Assert.assertThat(kp2.getPrivateKey(), IsNot.not(IsEqual.equalTo(kp1.getPrivateKey())));
		Assert.assertThat(kp2.getPublicKey(), IsNot.not(IsEqual.equalTo(kp1.getPublicKey())));
	}

	protected KeyGenerator getKeyGenerator() {
		return this.getCryptoEngine().createKeyGenerator();
	}

	protected abstract CryptoEngine getCryptoEngine();
}
