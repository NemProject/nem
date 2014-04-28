package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;

public class CipherTest {

	@Test
	public void encryptedDataCanBeDecrypted() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Cipher cipher = new Cipher(kp, kp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = cipher.encrypt(input);
		final byte[] decryptedBytes = cipher.decrypt(encryptedBytes);

		// Assert:
		Assert.assertThat(encryptedBytes, IsNot.not(IsEqual.equalTo(decryptedBytes)));
		Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
	}

	@Test
	public void dataCanBeEncryptedWithSenderPrivateKeyAndRecipientPublicKey() {
		// Arrange:
		final KeyPair skp = new KeyPair();
		final KeyPair rkp = new KeyPair();
		final Cipher cipher = new Cipher(skp, new KeyPair(rkp.getPublicKey()));
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = cipher.encrypt(input);

		// Assert:
		Assert.assertThat(encryptedBytes, IsNot.not(IsEqual.equalTo(input)));
	}

	@Test
	public void dataCanBeDecryptedWithSenderPublicKeyAndRecipientPrivateKey() {
		// Arrange:
		final KeyPair skp = new KeyPair();
		final KeyPair rkp = new KeyPair();
		final Cipher cipher1 = new Cipher(skp, new KeyPair(rkp.getPublicKey()));
		final Cipher cipher2 = new Cipher(new KeyPair(skp.getPublicKey()), rkp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = cipher1.encrypt(input);
		final byte[] decryptedBytes = cipher2.decrypt(encryptedBytes);

		// Assert:
		Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
	}

	@Test
	public void dataCanBeDecryptedWithSenderPrivateKeyAndRecipientPublicKey() {
		// Arrange:
		final KeyPair skp = new KeyPair();
		final KeyPair rkp = new KeyPair();
		final Cipher cipher1 = new Cipher(skp, new KeyPair(rkp.getPublicKey()));
		final Cipher cipher2 = new Cipher(new KeyPair(rkp.getPublicKey()), skp);
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes = cipher1.encrypt(input);
		final byte[] decryptedBytes = cipher2.decrypt(encryptedBytes);

		// Assert:
		Assert.assertThat(decryptedBytes, IsEqual.equalTo(input));
	}

	@Test
	public void dataEncryptedWithPrivateKeyCanOnlyBeDecryptedByMatchingPublicKey() {
		// Arrange:
		final Cipher cipher1 = new Cipher(new KeyPair(), new KeyPair());
		final Cipher cipher2 = new Cipher(new KeyPair(), new KeyPair());
		final byte[] input = Utils.generateRandomBytes();

		// Act:
		final byte[] encryptedBytes1 = cipher1.encrypt(input);
		final byte[] encryptedBytes2 = cipher2.encrypt(input);

		// Assert:
		Assert.assertThat(cipher1.decrypt(encryptedBytes1), IsEqual.equalTo(input));
		Assert.assertThat(cipher1.decrypt(encryptedBytes2), IsNull.nullValue());
		Assert.assertThat(cipher2.decrypt(encryptedBytes1), IsNull.nullValue());
		Assert.assertThat(cipher2.decrypt(encryptedBytes2), IsEqual.equalTo(input));
	}
}
