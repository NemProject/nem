package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

public abstract class BlockCipherTest {

	// TODO 20141010 J-B: consider marking initCryptoEngine with @Before (or having a before function that calls it if that doesn't work)
	// > instead of calling it in each test
	// TODO 20141011 BR -> J: Thanks for the hint. Done. (needed to declare initCryptoEngine public but that shouldn't be a problem or is it?)

	@Test
	public void encryptedDataCanBeDecrypted() {
		// Arrange:
		final KeyPair kp = new KeyPair();
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
		final KeyPair skp = new KeyPair();
		final KeyPair rkp = new KeyPair();
		final BlockCipher blockCipher = this.getBlockCipher(skp, new KeyPair(rkp.getPublicKey()));
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = blockCipher.encrypt(input);

		// Assert:
		Assert.assertThat(encryptedBytes, IsNot.not(IsEqual.equalTo(input)));
	}

	@Test
	public void dataCanBeDecryptedWithSenderPublicKeyAndRecipientPrivateKey() {
		// Arrange:
		final KeyPair skp = new KeyPair();
		final KeyPair rkp = new KeyPair();
		final BlockCipher blockCipher1 = this.getBlockCipher(skp, new KeyPair(rkp.getPublicKey()));
		final BlockCipher blockCipher2 = this.getBlockCipher(new KeyPair(skp.getPublicKey()), rkp);
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
		final KeyPair skp = new KeyPair();
		final KeyPair rkp = new KeyPair();
		final BlockCipher blockCipher1 = this.getBlockCipher(skp, new KeyPair(rkp.getPublicKey()));
		final BlockCipher blockCipher2 = this.getBlockCipher(new KeyPair(rkp.getPublicKey()), skp);
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
		final BlockCipher blockCipher1 = this.getBlockCipher(new KeyPair(), new KeyPair());
		final BlockCipher blockCipher2 = this.getBlockCipher(new KeyPair(), new KeyPair());
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes1 = blockCipher1.encrypt(input);
		final byte[] encryptedBytes2 = blockCipher2.encrypt(input);

		// Assert:
		Assert.assertThat(blockCipher1.decrypt(encryptedBytes1), IsEqual.equalTo(input));
		Assert.assertThat(blockCipher1.decrypt(encryptedBytes2), IsNull.nullValue());
		Assert.assertThat(blockCipher2.decrypt(encryptedBytes1), IsNull.nullValue());
		Assert.assertThat(blockCipher2.decrypt(encryptedBytes2), IsEqual.equalTo(input));
	}

	protected abstract BlockCipher getBlockCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair);

	@Before
	public abstract void initCryptoEngine();
}
