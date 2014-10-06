package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

public abstract class IesCipherTest {

	@Test
	public void encryptedDataCanBeDecrypted() {
		// Arrange:
		initCryptoEngine();
		final KeyPair kp = new KeyPair();
		final IesCipher iesCipher = getIesCipher(kp, kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = iesCipher.encrypt(input);
		final byte[] decryptedBytes = iesCipher.decrypt(encryptedBytes);

		// Assert:
		Assert.assertThat(encryptedBytes, IsNot.not(IsEqual.equalTo(decryptedBytes)));
		Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
	}

	@Test
	public void dataCanBeEncryptedWithSenderPrivateKeyAndRecipientPublicKey() {
		// Arrange:
		initCryptoEngine();
		final KeyPair skp = new KeyPair();
		final KeyPair rkp = new KeyPair();
		final IesCipher iesCipher = getIesCipher(skp, new KeyPair(rkp.getPublicKey()));
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = iesCipher.encrypt(input);

		// Assert:
		Assert.assertThat(encryptedBytes, IsNot.not(IsEqual.equalTo(input)));
	}

	@Test
	public void dataCanBeDecryptedWithSenderPublicKeyAndRecipientPrivateKey() {
		// Arrange:
		initCryptoEngine();
		final KeyPair skp = new KeyPair();
		final KeyPair rkp = new KeyPair();
		final IesCipher iesCipher1 = getIesCipher(skp, new KeyPair(rkp.getPublicKey()));
		final IesCipher iesCipher2 = getIesCipher(new KeyPair(skp.getPublicKey()), rkp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = iesCipher1.encrypt(input);
		final byte[] decryptedBytes = iesCipher2.decrypt(encryptedBytes);

		// Assert:
		Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
	}

	@Test
	public void dataCanBeDecryptedWithSenderPrivateKeyAndRecipientPublicKey() {
		// Arrange:
		initCryptoEngine();
		final KeyPair skp = new KeyPair();
		final KeyPair rkp = new KeyPair();
		final IesCipher iesCipher1 = getIesCipher(skp, new KeyPair(rkp.getPublicKey()));
		final IesCipher iesCipher2 = getIesCipher(new KeyPair(rkp.getPublicKey()), skp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = iesCipher1.encrypt(input);
		final byte[] decryptedBytes = iesCipher2.decrypt(encryptedBytes);

		// Assert:
		Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
	}

	@Test
	public void dataEncryptedWithPrivateKeyCanOnlyBeDecryptedByMatchingPublicKey() {
		// Arrange:
		initCryptoEngine();
		final IesCipher iesCipher1 = getIesCipher(new KeyPair(), new KeyPair());
		final IesCipher iesCipher2 = getIesCipher(new KeyPair(), new KeyPair());
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes1 = iesCipher1.encrypt(input);
		final byte[] encryptedBytes2 = iesCipher2.encrypt(input);

		// Assert:
		Assert.assertThat(iesCipher1.decrypt(encryptedBytes1), IsEqual.equalTo(input));
		Assert.assertThat(iesCipher1.decrypt(encryptedBytes2), IsNull.nullValue());
		Assert.assertThat(iesCipher2.decrypt(encryptedBytes1), IsNull.nullValue());
		Assert.assertThat(iesCipher2.decrypt(encryptedBytes2), IsEqual.equalTo(input));
	}

	protected abstract IesCipher getIesCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair);
	protected abstract void initCryptoEngine();
}
