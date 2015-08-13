package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

public abstract class BlockCipherTest {

	@Test
	public void encryptedDataCanBeDecrypted() {
		// Arrange:
		final CryptoEngine engine = this.getCryptoEngine();
		final KeyPair kp = KeyPair.random(engine);
		final BlockCipher blockCipher = this.getBlockCipher(kp, kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = blockCipher.encrypt(input);
		final byte[] decryptedBytes = blockCipher.decrypt(encryptedBytes);

		// Assert:
		Assert.assertThat(encryptedBytes, IsNot.not(IsEqual.equalTo(decryptedBytes)));
		Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
	}

	@Test
	public void dataCanBeEncryptedWithSenderPrivateKeyAndRecipientPublicKey() {
		// Arrange:
		final CryptoEngine engine = this.getCryptoEngine();
		final KeyPair skp = KeyPair.random(engine);
		final KeyPair rkp = KeyPair.random(engine);
		final BlockCipher blockCipher = this.getBlockCipher(skp, new KeyPair(rkp.getPublicKey(), engine));
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = blockCipher.encrypt(input);

		// Assert:
		Assert.assertThat(encryptedBytes, IsNot.not(IsEqual.equalTo(input)));
	}

	@Test
	public void dataCanBeDecryptedWithSenderPublicKeyAndRecipientPrivateKey() {
		// Arrange:
		final CryptoEngine engine = this.getCryptoEngine();
		final KeyPair skp = KeyPair.random(engine);
		final KeyPair rkp = KeyPair.random(engine);
		final BlockCipher blockCipher1 = this.getBlockCipher(skp, new KeyPair(rkp.getPublicKey(), engine));
		final BlockCipher blockCipher2 = this.getBlockCipher(new KeyPair(skp.getPublicKey(), engine), rkp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = blockCipher1.encrypt(input);
		final byte[] decryptedBytes = blockCipher2.decrypt(encryptedBytes);

		// Assert:
		Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
	}

	@Test
	public void dataCanBeDecryptedWithSenderPrivateKeyAndRecipientPublicKey() {
		// Arrange:
		final CryptoEngine engine = this.getCryptoEngine();
		final KeyPair skp = KeyPair.random(engine);
		final KeyPair rkp = KeyPair.random(engine);
		final BlockCipher blockCipher1 = this.getBlockCipher(skp, new KeyPair(rkp.getPublicKey(), engine));
		final BlockCipher blockCipher2 = this.getBlockCipher(new KeyPair(rkp.getPublicKey(), engine), skp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = blockCipher1.encrypt(input);
		final byte[] decryptedBytes = blockCipher2.decrypt(encryptedBytes);

		// Assert:
		Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
	}

	@Test
	public void dataEncryptedWithPrivateKeyCanOnlyBeDecryptedByMatchingPublicKey() {
		// Arrange:
		final CryptoEngine engine = this.getCryptoEngine();
		final BlockCipher blockCipher1 = this.getBlockCipher(KeyPair.random(engine), KeyPair.random(engine));
		final BlockCipher blockCipher2 = this.getBlockCipher(KeyPair.random(engine), KeyPair.random(engine));
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes1 = blockCipher1.encrypt(input);
		final byte[] encryptedBytes2 = blockCipher2.encrypt(input);

		// Assert:
		Assert.assertThat(blockCipher1.decrypt(encryptedBytes1), IsEqual.equalTo(input));
		Assert.assertThat(blockCipher1.decrypt(encryptedBytes2), IsNot.not(IsEqual.equalTo(input)));
		Assert.assertThat(blockCipher2.decrypt(encryptedBytes1), IsNot.not(IsEqual.equalTo(input)));
		Assert.assertThat(blockCipher2.decrypt(encryptedBytes2), IsEqual.equalTo(input));
	}

	protected BlockCipher getBlockCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		return this.getCryptoEngine().createBlockCipher(senderKeyPair, recipientKeyPair);
	}

	protected abstract CryptoEngine getCryptoEngine();
}
